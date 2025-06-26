package com.vroute.solution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;

/**
 * Class responsible for evaluating and scoring solutions and routes
 */
public class Evaluator {
    // Constants for penalty calculation
    private static final double UNDELIVERED_ORDER_PENALTY = 10000.0;
    private static final double DISTANCE_COST_PER_KM = 10.0;
    private static final double LATE_DELIVERY_PENALTY_PER_MINUTE = 50.0;

    private static void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[DEBUG-EVAL] " + message);
        }
    }

    public static double evaluateSolution(Environment env, Solution solution) {
        if (solution == null || env == null) {
            debug("Invalid solution or environment (null)");
            return Double.NEGATIVE_INFINITY;
        }

        debug("Evaluating solution with " + solution.getRoutes().size() + " routes");
        double totalCost = 0.0;
        Map<String, Integer> deliveredGlpByOrderId = new HashMap<>();

        // Evaluate each route
        for (Route route : solution.getRoutes()) {
            double routeCost = evaluateRoute(env, route, solution.getOrders(), deliveredGlpByOrderId);
            debug(String.format("Route %s cost: %f", route.getVehicle().getId(), routeCost));
            totalCost += routeCost;
        }

        // Check if all orders are fully delivered
        double undeliveredPenalty = 0.0;
        for (Order order : solution.getOrders().values()) {
            int requestedGlp = order.getGlpRequestM3();
            int deliveredGlp = deliveredGlpByOrderId.getOrDefault(order.getId(), 0);

            if (deliveredGlp < requestedGlp) {
                // Order partially or not delivered
                double undeliveredRatio = (double) (requestedGlp - deliveredGlp) / requestedGlp;
                double penalty = UNDELIVERED_ORDER_PENALTY * undeliveredRatio;
                undeliveredPenalty += penalty;
                debug("Order " + order.getId() + " partially undelivered: " + undeliveredRatio +
                        ", penalty: " + penalty);
            }
        }

        totalCost += undeliveredPenalty;
        debug("Total solution cost: " + totalCost + " (including undelivered penalty: " + undeliveredPenalty + ")");
        return totalCost;
    }

    public static double evaluateRoute(Environment env, Route route, Map<String, Order> orders,
            Map<String, Integer> deliveredGlpByOrderId) {
        double routeCost = 0.0;
        double latePenalty = 0.0;
        double distanceCost = 0.0;

        Vehicle currentVehicle = route.getVehicle().clone();
        Position currentPosition = currentVehicle.getCurrentPosition();
        LocalDateTime currentTime = route.getStartTime();
        
        for (RouteStop stop : route.getStops()) {
            // check path to stop from current vehicle position
            PathResult path = PathFinder.findPath(env, currentPosition, stop.getPosition(), currentTime);
            if (path == null) {
                debug("No path found from " + currentPosition + " to " + stop.getPosition());
                return Double.NEGATIVE_INFINITY;
            }

            // Calculate distance and fuel consumption
            int distance = path.getDistance();
            double fuelConsumedGallons = currentVehicle.calculateFuelNeeded(distance);
            
            // Get arrival time at the destination from PathResult's arrivalTimes
            List<LocalDateTime> arrivalTimes = path.getArrivalTimes();
            if (arrivalTimes == null || arrivalTimes.isEmpty()) {
                debug("Path result contains no arrival times");
                return Double.NEGATIVE_INFINITY;
            }
            
            LocalDateTime arrivalTime = arrivalTimes.getLast();
            
            // Update the RouteStop with the calculated arrival time
            // Note: This won't actually modify the stop in the original solution,
            // but it's useful for more accurate evaluation
            
            // Check if vehicle has enough fuel to reach stop
            if (currentVehicle.getCurrentFuelGal() < fuelConsumedGallons) {
                debug("Vehicle " + currentVehicle.getId() + " doesn't have enough fuel to reach stop");
                return Double.NEGATIVE_INFINITY;
            }

            if (stop instanceof OrderStop) {
                OrderStop orderStop = (OrderStop) stop;
                Order order = orders.get(orderStop.getEntityID());
                
                if (order == null) {
                    debug("Order " + orderStop.getEntityID() + " not found in solution");
                    return Double.NEGATIVE_INFINITY;
                }
                
                // verify GLP capacity
                if (currentVehicle.getCurrentGlpM3() - orderStop.getGlpDelivery() < 0) {
                    debug("Vehicle " + currentVehicle.getId() + " doesn't have enough GLP for order " + order.getId());
                    return Double.NEGATIVE_INFINITY;
                }

                // Check if delivery is late and apply penalty
                if (arrivalTime.isAfter(order.getDueTime())) {
                    Duration lateDuration = Duration.between(order.getDueTime(), arrivalTime);
                    long lateMinutes = lateDuration.toMinutes();
                    double orderLatePenalty = lateMinutes * LATE_DELIVERY_PENALTY_PER_MINUTE;
                    latePenalty += orderLatePenalty;
                    debug("Late delivery for order " + order.getId() + " by " + lateMinutes +
                            " minutes, penalty: " + orderLatePenalty);
                }

                // Update vehicle state
                currentVehicle.serveOrder(order, orderStop.getGlpDelivery(), currentTime);
                
                // Record order delivery
                int previousDelivery = deliveredGlpByOrderId.getOrDefault(order.getId(), 0);
                deliveredGlpByOrderId.put(order.getId(), previousDelivery + orderStop.getGlpDelivery());
                
                debug("Delivered " + orderStop.getGlpDelivery() + "m³ to order " + order.getId() + 
                      " at " + arrivalTime + " (due: " + order.getDueTime() + ")");
            }

            if (stop instanceof DepotStop) {
                DepotStop depotStop = (DepotStop) stop;
                
                // Check depot capacity constraints
                if (currentVehicle.getCurrentGlpM3() + depotStop.getGlpRecharge() > currentVehicle.getGlpCapacityM3()) {
                    debug("Recharge at depot " + depotStop.getEntityID() + 
                          " exceeds vehicle capacity by " + 
                          (currentVehicle.getCurrentGlpM3() + depotStop.getGlpRecharge() - 
                           currentVehicle.getGlpCapacityM3()) + "m³");
                    return Double.NEGATIVE_INFINITY;
                }
                
                // Update vehicle state
                currentVehicle.refill(depotStop.getGlpRecharge());
                debug("Refilled " + depotStop.getGlpRecharge() + "m³ at depot " + depotStop.getEntityID());
            }

            // Add distance cost
            double segmentDistanceCost = distance * DISTANCE_COST_PER_KM;
            distanceCost += segmentDistanceCost;
            
            // Update current position and time for next iteration
            currentPosition = stop.getPosition();
            currentTime = arrivalTime;
            
            debug("Vehicle " + currentVehicle.getId() + 
                  " arrived at " + stop.getEntityID() + 
                  ", fuel: " + String.format("%.2f", currentVehicle.getCurrentFuelGal()) + "gal, " + 
                  "GLP: " + currentVehicle.getCurrentGlpM3() + "m³");
        }

        // Calculate total route cost
        routeCost = distanceCost + latePenalty;
        
        debug("Route cost breakdown - Distance cost: " + distanceCost + 
              ", Late delivery penalty: " + latePenalty + 
              ", Total: " + routeCost);
        
        return routeCost;
    }
}
