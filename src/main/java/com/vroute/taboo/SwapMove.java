package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A move that swaps two orders between routes.
 */
public class SwapMove implements TabuMove {
    private final String firstRouteId;
    private final String secondRouteId;
    private final int firstPosition;
    private final int secondPosition;
    private final String firstOrderId;
    private final String secondOrderId;
    
    // Reference to the environment for path calculations
    private static Environment environment;

    public SwapMove(String firstRouteId, String secondRouteId, int firstPosition, 
                   int secondPosition, String firstOrderId, String secondOrderId) {
        this.firstRouteId = firstRouteId;
        this.secondRouteId = secondRouteId;
        this.firstPosition = firstPosition;
        this.secondPosition = secondPosition;
        this.firstOrderId = firstOrderId;
        this.secondOrderId = secondOrderId;
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
            System.err.println("Environment not set for path calculations in SwapMove");
            return solution;
        }
        
        // Clone the solution to avoid modifying the original
        Map<String, Order> orders = solution.getOrders();
        List<Route> originalRoutes = solution.getRoutes();
        
        // Find the routes
        Route firstRoute = null;
        Route secondRoute = null;
        
        for (Route route : originalRoutes) {
            if (route.getId().equals(firstRouteId)) {
                firstRoute = route;
            }
            if (route.getId().equals(secondRouteId)) {
                secondRoute = route;
            }
        }
        
        if (firstRoute == null || secondRoute == null) {
            // Cannot apply move if routes not found
            return solution;
        }
        
        // Same route swap
        boolean isSameRoute = firstRouteId.equals(secondRouteId);
        
        // Create new lists of stops for both routes
        List<RouteStop> newFirstStops = new ArrayList<>(firstRoute.getStops());
        List<RouteStop> newSecondStops;
        
        if (isSameRoute) {
            newSecondStops = newFirstStops; // Reference the same list for same-route swaps
        } else {
            newSecondStops = new ArrayList<>(secondRoute.getStops());
        }
        
        // Check if positions are valid
        if (firstPosition < 0 || firstPosition >= newFirstStops.size() ||
            secondPosition < 0 || secondPosition >= newSecondStops.size()) {
            return solution;
        }
        
        // Get the stops to swap
        RouteStop firstStop = newFirstStops.get(firstPosition);
        RouteStop secondStop = newSecondStops.get(secondPosition);
        
        // Check if they are order stops
        if (!(firstStop instanceof OrderStop) || !(secondStop instanceof OrderStop)) {
            return solution;
        }
        
        OrderStop firstOrderStop = (OrderStop) firstStop;
        OrderStop secondOrderStop = (OrderStop) secondStop;
        
        // Verify the order IDs
        if (!firstOrderStop.getEntityID().equals(firstOrderId) || 
            !secondOrderStop.getEntityID().equals(secondOrderId)) {
            return solution;
        }
        
        // Recalculate order stops for their new positions
        OrderStop updatedFirstOrderStop = recalculateOrderStop(
            secondOrderStop, firstRoute, firstPosition);
        OrderStop updatedSecondOrderStop = recalculateOrderStop(
            firstOrderStop, secondRoute, secondPosition);
        
        if (updatedFirstOrderStop == null || updatedSecondOrderStop == null) {
            // One of the swaps is not feasible
            return solution;
        }
        
        // Perform the swap
        newFirstStops.set(firstPosition, updatedFirstOrderStop);
        newSecondStops.set(secondPosition, updatedSecondOrderStop);
        
        // Recalculate arrival times for all subsequent stops in both routes
        int firstUpdateStart = firstPosition + 1;
        int secondUpdateStart = secondPosition + 1;
        
        recalculateRouteTimings(newFirstStops, firstRoute.getVehicle(), firstUpdateStart);
        
        if (!isSameRoute) {
            recalculateRouteTimings(newSecondStops, secondRoute.getVehicle(), secondUpdateStart);
        }
        
        // Create new routes
        List<Route> newRoutes = new ArrayList<>(originalRoutes);
        
        // Replace the modified routes
        for (int i = 0; i < newRoutes.size(); i++) {
            Route route = newRoutes.get(i);
            if (route.getId().equals(firstRouteId)) {
                newRoutes.set(i, new Route(firstRouteId, firstRoute.getVehicle(), newFirstStops));
            } 
            if (!isSameRoute && route.getId().equals(secondRouteId)) {
                newRoutes.set(i, new Route(secondRouteId, secondRoute.getVehicle(), newSecondStops));
            }
        }
        
        // Create and return the new solution
        return new Solution(orders, newRoutes);
    }
    
    private OrderStop recalculateOrderStop(OrderStop orderStop, Route targetRoute, int targetPosition) {
        List<RouteStop> targetStops = targetRoute.getStops();
        
        Position prevPosition;
        LocalDateTime departureTime;
        
        if (targetPosition == 0) {
            // If inserting at the start, use the vehicle's current position
            prevPosition = targetRoute.getVehicle().getCurrentPosition();
            departureTime = LocalDateTime.now(); // This should be obtained from solution context
        } else {
            // Use the previous stop's position
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
        return "SWAP_" + firstOrderId + "_" + secondOrderId + "_BETWEEN_" + firstRouteId + "_" + secondRouteId;
    }
    
    @Override
    public String toString() {
        return "SwapMove: Order " + firstOrderId + " (route " + firstRouteId + ", pos " + firstPosition + 
               ") with Order " + secondOrderId + " (route " + secondRouteId + ", pos " + secondPosition + ")";
    }
} 