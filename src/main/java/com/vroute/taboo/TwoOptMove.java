package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A move that performs a 2-opt operation within a single route.
 * This reverses the order of stops between two positions to reduce crossing paths.
 */
public class TwoOptMove implements TabuMove {
    private final String routeId;
    private final int startPos; // Starting position for the segment to reverse
    private final int endPos;   // Ending position for the segment to reverse
    
    // Reference to the environment for path calculations
    private static Environment environment;

    public TwoOptMove(String routeId, int startPos, int endPos) {
        this.routeId = routeId;
        this.startPos = startPos;
        this.endPos = endPos;
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
            System.err.println("Environment not set for path calculations in TwoOptMove");
            return solution;
        }
        
        // Clone the solution to avoid modifying the original
        Map<String, Order> orders = solution.getOrders();
        List<Route> originalRoutes = solution.getRoutes();
        
        // Find the route
        Route route = null;
        for (Route r : originalRoutes) {
            if (r.getId().equals(routeId)) {
                route = r;
                break;
            }
        }
        
        if (route == null) {
            // Cannot apply move if route not found
            return solution;
        }
        
        // Check positions are valid
        List<RouteStop> stops = route.getStops();
        if (startPos < 0 || endPos >= stops.size() || startPos >= endPos) {
            return solution;
        }
        
        // Create new list with the segment reversed
        List<RouteStop> newStops = new ArrayList<>(stops);
        
        // Reverse the segment between startPos and endPos
        List<RouteStop> subList = new ArrayList<>(newStops.subList(startPos, endPos + 1));
        Collections.reverse(subList);
        
        // Replace the segment in the original list
        for (int i = startPos; i <= endPos; i++) {
            newStops.set(i, subList.get(i - startPos));
        }
        
        // Recalculate arrival times for the modified segment and all subsequent stops
        recalculateRouteTimings(newStops, route.getVehicle(), startPos);
        
        // Create new route
        Route newRoute = new Route(routeId, route.getVehicle(), newStops);
        
        // Create new routes list with the modified route
        List<Route> newRoutes = new ArrayList<>(originalRoutes);
        for (int i = 0; i < newRoutes.size(); i++) {
            if (newRoutes.get(i).getId().equals(routeId)) {
                newRoutes.set(i, newRoute);
                break;
            }
        }
        
        return new Solution(orders, newRoutes);
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
        return "TWOOPT_" + routeId + "_" + startPos + "_" + endPos;
    }
    
    @Override
    public String toString() {
        return "TwoOptMove: reverse segment [" + startPos + "-" + endPos + "] in route " + routeId;
    }
} 