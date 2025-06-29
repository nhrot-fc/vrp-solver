package com.vroute.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Constants;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.models.Position;

public class Solution {
    private final String id;
    private final List<Route> routes;
    private final Environment environment;
    private Double score;

    // Constructor with environment
    public Solution(List<Route> routes, Environment environment) {
        this.id = UUID.randomUUID().toString();
        this.routes = routes;
        this.environment = environment;
        this.score = null;
    }

    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getId() {
        return id;
    }

    public double getScore() {
        if (score == null) {
            score = Evaluator.evaluateSolution(environment, this);
        }
        return score;
    }

    @Override
    public String toString() {
        return String.format("Solution with %d routes", routes.size());
    }

    /**
     * Create a deep clone of this solution
     * @return A new Solution instance with copies of all routes
     */
    @Override
    public Solution clone() {
        List<Route> clonedRoutes = new ArrayList<>();
        for (Route route : this.routes) {
            clonedRoutes.add(route.clone());
        }
        return new Solution(clonedRoutes, this.environment);
    }

    /**
     * Verifies that all orders are delivered on time and completely.
     * @throws AssertionError if any order is not delivered completely or is late
     */
    public void verifyOrderDeliveries() throws AssertionError {
        Map<String, Integer> deliveryMap = new HashMap<>();
        Map<String, Boolean> onTimeMap = new HashMap<>();
        
        // Initialize maps with zero values and false for on-time
        for (Order order : environment.getPendingOrders()) {
            deliveryMap.put(order.getId(), 0);
            onTimeMap.put(order.getId(), true);
        }
        
        // Process each route and collect delivery information
        for (Route route : routes) {
            Position currentPosition = route.getVehicle().getCurrentPosition();
            LocalDateTime currentTime = route.getStartTime();
            
            for (RouteStop stop : route.getStops()) {
                // Calculate arrival time to the stop
                PathResult pathResult = PathFinder.findPath(environment, currentPosition, stop.getPosition(), currentTime);
                if (pathResult == null) {
                    throw new AssertionError("No path found from " + currentPosition + " to " + stop.getPosition());
                }
                
                LocalDateTime eta = pathResult.getArrivalTimes().getLast();
                currentTime = eta;
                
                // Process order stops
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    Order order = orderStop.getOrder();
                    int glpDelivery = orderStop.getGlpDelivery();
                    
                    // Check if delivery is on time
                    if (eta.isAfter(order.getDueTime())) {
                        onTimeMap.put(order.getId(), false);
                    }
                    
                    // Update delivery amount
                    deliveryMap.put(order.getId(), 
                        deliveryMap.getOrDefault(order.getId(), 0) + glpDelivery);
                    
                    currentTime = currentTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
                } else if (stop instanceof DepotStop) {
                    // For depot stops, just update the current time
                    currentTime = currentTime.plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
                }
                
                currentPosition = stop.getPosition();
            }
        }
        
        // Verify all orders are fully delivered and on time
        StringBuilder errorMessages = new StringBuilder();
        for (Order order : environment.getPendingOrders()) {
            int delivered = deliveryMap.getOrDefault(order.getId(), 0);
            boolean onTime = onTimeMap.getOrDefault(order.getId(), true);
            
            // Check if all GLP was delivered
            if (delivered < order.getGlpRequestM3()) {
                errorMessages.append("Order ").append(order.getId())
                    .append(" incomplete: ").append(delivered).append("/")
                    .append(order.getGlpRequestM3()).append("m³ delivered\n");
            }
            
            // Check if delivery was on time
            if (!onTime) {
                errorMessages.append("Order ").append(order.getId())
                    .append(" delivered late\n");
            }
        }
        
        // If there are any error messages, throw an assertion error
        if (errorMessages.length() > 0) {
            throw new AssertionError("Order delivery verification failed:\n" + errorMessages.toString());
        }
    }
}
