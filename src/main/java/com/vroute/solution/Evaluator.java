package com.vroute.solution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

/**
 * Class responsible for evaluating and scoring solutions and routes
 */
public class Evaluator {

    // Constants for penalty calculation
    private static final double LATE_DELIVERY_PENALTY_PER_HOUR = 500.0;
    private static final double UNDELIVERED_ORDER_PENALTY = 10000.0;
    private static final double DISTANCE_COST_PER_KM = 10.0;

    /**
     * Evaluates a solution based on its routes and orders.
     * 
     * @param env      The environment with blockages, orders, etc.
     * @param solution The solution to evaluate
     * @return The cost of the solution, or NEGATIVE_INFINITY if the solution is
     *         invalid
     */
    public static double evaluateSolution(Environment env, Solution solution) {
        if (solution == null || env == null) {
            return Double.NEGATIVE_INFINITY;
        }

        double totalCost = 0.0;
        Map<String, Integer> deliveredGlpByOrderId = new HashMap<>();

        // Evaluate each route
        for (Route route : solution.getRoutes()) {
            double routeCost = evaluateRoute(env, route, solution.getOrders(), deliveredGlpByOrderId);

            if (routeCost == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY; // Invalid route invalidates entire solution
            }

            totalCost += routeCost;
        }

        // Check if all orders are fully delivered
        for (Order order : solution.getOrders().values()) {
            int requestedGlp = order.getGlpRequestM3();
            int deliveredGlp = deliveredGlpByOrderId.getOrDefault(order.getId(), 0);

            if (deliveredGlp < requestedGlp) {
                // Order partially or not delivered
                double undeliveredRatio = (double) (requestedGlp - deliveredGlp) / requestedGlp;
                totalCost += undeliveredRatio * UNDELIVERED_ORDER_PENALTY;
            }
        }

        return totalCost;
    }

    /**
     * Evaluates a single route.
     * 
     * @param env                   The environment with blockages, etc.
     * @param route                 The route to evaluate
     * @param orders                Map of orders in the solution
     * @param deliveredGlpByOrderId Map to track GLP delivered by order ID (will be
     *                              updated)
     * @return The cost of the route, or NEGATIVE_INFINITY if the route is invalid
     */
    public static double evaluateRoute(Environment env, Route route, Map<String, Order> orders,
            Map<String, Integer> deliveredGlpByOrderId) {
        if (route == null || env == null) {
            return Double.NEGATIVE_INFINITY;
        }

        double routeCost = 0.0;
        Vehicle vehicle = route.getVehicle().clone();
        List<RouteStop> stops = route.getStops();
        Position currentPosition = vehicle.getCurrentPosition();

        for (RouteStop stop : stops) {
            Position stopPosition = stop.getPosition();
            double distance = currentPosition.distanceTo(stopPosition);

            // Add distance cost
            routeCost += distance * DISTANCE_COST_PER_KM;

            // Check fuel
            double fuelNeeded = vehicle.calculateFuelNeeded(distance);
            if (fuelNeeded > vehicle.getCurrentFuelGal()) {
                return Double.NEGATIVE_INFINITY; // Not enough fuel
            }
            vehicle.consumeFuel(distance);

            if (stop instanceof OrderStop) {
                OrderStop orderStop = (OrderStop) stop;
                int glpDelivery = orderStop.getGlpDelivery();

                // Check GLP availability
                if (glpDelivery > vehicle.getCurrentGlpM3()) {
                    return Double.NEGATIVE_INFINITY; // Not enough GLP
                }

                vehicle.dispenseGlp(glpDelivery);

                // Track delivered GLP by order ID
                String orderId = orderStop.getEntityID();
                deliveredGlpByOrderId.put(
                        orderId,
                        deliveredGlpByOrderId.getOrDefault(orderId, 0) + glpDelivery);

                // Check if delivered late
                Order order = orders.get(orderId);
                if (order != null) {
                    LocalDateTime dueTime = order.getDueTime();
                    LocalDateTime deliveryTime = orderStop.getArrivalTime();

                    if (deliveryTime.isAfter(dueTime)) {
                        Duration delay = Duration.between(dueTime, deliveryTime);
                        long hoursLate = delay.toHours();
                        if (delay.toMinutes() % 60 > 0) {
                            hoursLate++; // Round up to next hour
                        }
                        routeCost += hoursLate * LATE_DELIVERY_PENALTY_PER_HOUR;
                    }
                }
            } else if (stop instanceof DepotStop) {
                DepotStop depotStop = (DepotStop) stop;
                int glpRecharge = depotStop.getGlpRecharge();

                // Check if GLP exceeds capacity
                if (vehicle.getCurrentGlpM3() + glpRecharge > vehicle.getGlpCapacityM3()) {
                    return Double.NEGATIVE_INFINITY; // Too much GLP
                }

                vehicle.refill(glpRecharge);
                vehicle.refuel(); // Refuel at depot
            }

            // Update position
            vehicle.setCurrentPosition(stopPosition);
            currentPosition = stopPosition;
        }

        return routeCost;
    }

    /**
     * Checks if this solution is valid (all routes are feasible).
     * 
     * @param env      The environment with blockages, orders, etc.
     * @param solution The solution to validate
     * @return true if the solution is valid, false otherwise
     */
    public static boolean isSolutionValid(Environment env, Solution solution) {
        return evaluateSolution(env, solution) != Double.NEGATIVE_INFINITY;
    }
}
