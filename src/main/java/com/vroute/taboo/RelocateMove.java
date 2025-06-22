package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.models.Constants;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A move that relocates an order from one route to another.
 */
public class RelocateMove implements TabuMove {
    private final String sourceRouteId;
    private final String targetRouteId;
    private final int sourcePosition; // Position of the order stop in the source route
    private final int targetPosition; // Position to insert the order stop in the target route
    private final String orderId;
    
    // Reference to the environment for path calculations
    private static Environment environment;

    public RelocateMove(String sourceRouteId, String targetRouteId, int sourcePosition, 
                        int targetPosition, String orderId) {
        this.sourceRouteId = sourceRouteId;
        this.targetRouteId = targetRouteId;
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.orderId = orderId;
    }
    
    /**
     * Sets the environment for path calculations.
     * This must be called before using any move operations.
     * 
     * @param env The environment instance
     */
    public static void setEnvironment(Environment env) {
        environment = env;
    }

    @Override
    public Solution apply(Solution solution) {
        if (environment == null) {
            System.err.println("Environment not set for path calculations in RelocateMove");
            return solution;
        }
        
        // Clone the solution to avoid modifying the original
        Map<String, Order> orders = solution.getOrders();
        List<Route> originalRoutes = solution.getRoutes();
        
        // Find source and target routes
        Route sourceRoute = null;
        Route targetRoute = null;
        
        for (Route route : originalRoutes) {
            if (route.getId().equals(sourceRouteId)) {
                sourceRoute = route;
            }
            if (route.getId().equals(targetRouteId)) {
                targetRoute = route;
            }
        }
        
        if (sourceRoute == null || targetRoute == null) {
            // Cannot apply move if routes not found
            return solution;
        }
        
        // Create new lists of stops for both routes
        List<RouteStop> newSourceStops = new ArrayList<>(sourceRoute.getStops());
        List<RouteStop> newTargetStops = new ArrayList<>(targetRoute.getStops());
        
        // Check if the source position is valid
        if (sourcePosition < 0 || sourcePosition >= newSourceStops.size()) {
            return solution;
        }
        
        // Get the stop to relocate
        RouteStop stopToMove = newSourceStops.get(sourcePosition);
        
        // Check if it's an OrderStop
        if (!(stopToMove instanceof OrderStop)) {
            return solution;
        }
        
        OrderStop orderStopToMove = (OrderStop) stopToMove;
        
        // Verify this is the correct order
        if (!orderStopToMove.getEntityID().equals(orderId)) {
            return solution;
        }
        
        // Remove from source route
        newSourceStops.remove(sourcePosition);
        
        // Calculate the new arrival time for the order in the target route
        OrderStop updatedOrderStop = recalculateOrderStop(orderStopToMove, targetRoute, targetPosition);
        if (updatedOrderStop == null) {
            // Move is not feasible
            return solution;
        }
        
        // Insert into target route
        if (targetPosition <= newTargetStops.size()) {
            newTargetStops.add(targetPosition, updatedOrderStop);
        } else {
            newTargetStops.add(updatedOrderStop);
        }
        
        // Recalculate arrival times for all subsequent stops in both routes
        recalculateRouteTimings(newSourceStops, sourceRoute.getVehicle(), sourcePosition);
        recalculateRouteTimings(newTargetStops, targetRoute.getVehicle(), targetPosition);
        
        // Create new routes
        List<Route> newRoutes = new ArrayList<>(originalRoutes);
        
        // Replace the modified routes
        for (int i = 0; i < newRoutes.size(); i++) {
            Route route = newRoutes.get(i);
            if (route.getId().equals(sourceRouteId)) {
                newRoutes.set(i, new Route(sourceRouteId, sourceRoute.getVehicle(), newSourceStops));
            } else if (route.getId().equals(targetRouteId)) {
                newRoutes.set(i, new Route(targetRouteId, targetRoute.getVehicle(), newTargetStops));
            }
        }
        
        // Create and return the new solution
        return new Solution(orders, newRoutes);
    }
    
    private OrderStop recalculateOrderStop(OrderStop orderStop, Route targetRoute, int targetPosition) {
        Vehicle vehicle = targetRoute.getVehicle();
        List<RouteStop> targetStops = targetRoute.getStops();
        
        // Determine the position before insertion
        Position prevPosition;
        LocalDateTime departureTime;
        
        if (targetPosition == 0) {
            // If inserting at the start, use the vehicle's current position
            prevPosition = vehicle.getCurrentPosition();
            departureTime = LocalDateTime.now(); // This should be obtained from the solution context
        } else {
            // Otherwise, use the previous stop's position
            RouteStop prevStop = targetStops.get(targetPosition - 1);
            prevPosition = prevStop.getPosition();
            departureTime = prevStop.getArrivalTime().plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
        }
        
        // Calculate the path to the new position
        PathResult pathResult = PathFinder.findPath(
                environment, // Use the environment reference
                prevPosition, 
                orderStop.getPosition(),
                departureTime);
        
        if (!pathResult.isPathFound()) {
            return null; // Path not found, move is not feasible
        }
        
        // Create a new OrderStop with updated arrival time
        return new OrderStop(
                orderStop.getEntityID(),
                orderStop.getPosition(),
                pathResult.getArrivalTime(),
                orderStop.getGlpDelivery()
        );
    }
    
    private void recalculateRouteTimings(List<RouteStop> stops, Vehicle vehicle, int startPosition) {
        if (stops.size() <= startPosition) {
            return; // Nothing to recalculate
        }
        
        for (int i = startPosition; i < stops.size(); i++) {
            if (i == 0) {
                // First stop uses vehicle's starting position
                continue;
            }
            
            RouteStop prevStop = stops.get(i - 1);
            RouteStop currentStop = stops.get(i);
            
            // Calculate departure time from previous stop
            LocalDateTime departureTime = prevStop.getArrivalTime().plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
            
            // Calculate new arrival time
            PathResult pathResult = PathFinder.findPath(
                    environment, // Use the environment reference
                    prevStop.getPosition(),
                    currentStop.getPosition(),
                    departureTime);
            
            if (!pathResult.isPathFound()) {
                continue; // Maintain current timing if path not found
            }
            
            // Update the arrival time for this stop
            if (currentStop instanceof OrderStop) {
                OrderStop orderStop = (OrderStop) currentStop;
                stops.set(i, new OrderStop(
                        orderStop.getEntityID(),
                        orderStop.getPosition(),
                        pathResult.getArrivalTime(),
                        orderStop.getGlpDelivery()
                ));
            } 
            // Similarly handle DepotStop if needed
        }
    }

    @Override
    public String getTabuKey() {
        // The tabu key identifies the move for the tabu list
        return "RELOCATE_" + orderId + "_FROM_" + sourceRouteId + "_TO_" + targetRouteId;
    }
    
    @Override
    public String toString() {
        return "RelocateMove: Order " + orderId + " from route " + sourceRouteId + 
               " (pos " + sourcePosition + ") to route " + targetRouteId + 
               " (pos " + targetPosition + ")";
    }
} 