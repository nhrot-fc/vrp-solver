package com.vroute.solution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Depot;
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
    private static final double UNDELIVERED_ORDER_PENALTY = 50000.0;
    private static final double LATE_DELIVERY_PENALTY = 10000.0;
    private static final double INSUFFICIENT_FUEL_PENALTY = 20000.0;
    private static final double DEPOT_NEGATIVE_GLP_PENALTY = 30000.0;

    private static final double DISTANCE_COST_PER_KM = 0.1;

    private static void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[Evaluator] " + message);
        }
    }

    public static double evaluateSolution(Environment env, Solution solution) {
        Map<String, Integer> deliveryMap = new HashMap<>();
        Map<String, Integer> depotMap = new HashMap<>();

        // Initialize maps with zero values
        for (Order order : env.getPendingOrders()) {
            deliveryMap.put(order.getId(), 0);
        }
        for (Depot depot : env.getDepots()) {
            depotMap.put(depot.getId(), 0);
        }

        double totalDistance = 0.0;
        double totalPenalty = 0.0;

        for (Route route : solution.getRoutes()) {
            Vehicle currentVehicle = route.getVehicle().clone();
            Position currentPosition = currentVehicle.getCurrentPosition();
            LocalDateTime currentTime = route.getStartTime();

            // Iterate over the stops in the route
            for (RouteStop stop : route.getStops()) {
                int glpChange = 0;
                Duration duration = Duration.ZERO;
                PathResult pathResult = PathFinder.findPath(env, currentPosition, stop.getPosition(), currentTime);
                if (pathResult == null) {
                    debug(String.format("No path found from %s to %s", currentPosition, stop.getPosition()));
                    return Double.NEGATIVE_INFINITY;
                }

                // Calculate distance and update total
                int distance = pathResult.getDistance();
                totalDistance += distance;

                // Check fuel constraint
                double fuelNeeded = currentVehicle.calculateFuelNeeded(distance);
                if (fuelNeeded > currentVehicle.getCurrentFuelGal()) {
                    debug(String.format("Vehicle %s has insufficient fuel: %f/%f", currentVehicle.getId(),
                            currentVehicle.getCurrentFuelGal(), fuelNeeded));
                    totalPenalty += INSUFFICIENT_FUEL_PENALTY;
                }
                LocalDateTime eta = pathResult.getArrivalTimes().getLast();
                currentTime = eta;

                // Process the stop
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    Order order = orderStop.getOrder();
                    int glpDelivery = orderStop.getGlpDelivery();

                    if (eta.isAfter(order.getDueTime())) {
                        totalPenalty += LATE_DELIVERY_PENALTY;
                        debug(String.format("Late delivery for order %s by %d minutes", order.getId(),
                                Duration.between(order.getDueTime(), eta).toMinutes()));
                    }

                    // Update the delivery map
                    deliveryMap.put(order.getId(), deliveryMap.getOrDefault(order.getId(), 0) + glpDelivery);
                    glpChange -= glpDelivery;
                    duration = Duration.ofMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
                } else {
                    DepotStop depotStop = (DepotStop) stop;
                    int glpRecharge = depotStop.getGlpRecharge();

                    // Check if depot has enough GLP
                    String depotId = depotStop.getDepot().getId();
                    int currentDepotGlp = depotStop.getDepot().getCurrentGlpM3();

                    if (depotMap.getOrDefault(depotId, 0) + glpRecharge > currentDepotGlp) {
                        debug(String.format("Depot %s has insufficient GLP: %d/%d", depotId, currentDepotGlp,
                                depotMap.getOrDefault(depotId, 0) + glpRecharge));
                        totalPenalty += DEPOT_NEGATIVE_GLP_PENALTY;
                    }

                    depotMap.put(depotId, depotMap.getOrDefault(depotId, 0) + glpRecharge);
                    glpChange += glpRecharge;
                    duration = Duration.ofMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);

                    // Refuel at depot if capable
                    if (depotStop.getDepot().canRefuel()) {
                        currentVehicle.refuel();
                    }
                }

                // Update current status
                currentVehicle.setCurrentPosition(stop.getPosition());
                currentVehicle.setCurrentGlpM3(currentVehicle.getCurrentGlpM3() + glpChange);
                currentVehicle.consumeFuelFromDistance(distance);
                currentPosition = stop.getPosition();
                currentTime = currentTime.plus(duration);

                // Check GLP constraints
                if (currentVehicle.getCurrentGlpM3() < 0
                        || currentVehicle.getCurrentGlpM3() > currentVehicle.getGlpCapacityM3()) {
                    debug(String.format("Vehicle %s has invalid GLP: %d/%d", currentVehicle.getId(),
                            currentVehicle.getCurrentGlpM3(), currentVehicle.getGlpCapacityM3()));
                    return Double.NEGATIVE_INFINITY;
                }

                // Check fuel constraints
                if (currentVehicle.getCurrentFuelGal() < 0) {
                    debug(String.format("Vehicle %s has insufficient fuel: %f", currentVehicle.getId(),
                            currentVehicle.getCurrentFuelGal()));
                    return Double.NEGATIVE_INFINITY;
                }
            }
        }

        // Check if all orders were completed
        for (Order order : env.getPendingOrders()) {
            int glpDelivered = deliveryMap.getOrDefault(order.getId(), 0);
            if (glpDelivered < order.getRemainingGlpM3()) {
                totalPenalty += UNDELIVERED_ORDER_PENALTY;
                debug(String.format("Order %s was not completed: %d/%d GLP delivered", order.getId(), glpDelivered,
                        order.getRemainingGlpM3()));
            }
            if (glpDelivered > order.getRemainingGlpM3()) {
                debug(String.format("Order %s was delivered more than remaining: %d/%d GLP delivered", order.getId(),
                        glpDelivered, order.getRemainingGlpM3()));
                return Double.NEGATIVE_INFINITY;
            }
        }

        // Calculate distance cost
        double distanceCost = totalDistance * DISTANCE_COST_PER_KM;

        return -(totalPenalty + distanceCost);
    }
}
