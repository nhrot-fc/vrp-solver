package com.vroute.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.taboo.RouteFixer;

public class SIHSolver implements Solver {

    private static final int[] GLP_DELIVERY_OPTIONS = { 5, 10, 15, 20, 25 };
    private final Random random = new Random();

    @Override
    public Solution solve(Environment environment) {
        if (environment == null) {
            return null;
        }

        // Get all pending orders and sort by deadline
        List<Order> pendingOrders = new ArrayList<>(environment.getPendingOrders());
        Collections.sort(pendingOrders, Comparator.comparing(Order::getDueTime));

        // Get available vehicles
        List<Vehicle> availableVehicles = new ArrayList<>(environment.getAvailableVehicles());
        if (availableVehicles.isEmpty()) {
            return null; // No vehicles available
        }

        // Initialize maps and lists to build our solution
        Map<String, Order> ordersMap = new HashMap<>();
        Map<Vehicle, List<OrderStop>> vehicleToOrderStopsMap = new HashMap<>();
        Map<String, Position> lastPositionMap = new HashMap<>();

        // Initialize each vehicle's order stops list and position map
        for (Vehicle vehicle : availableVehicles) {
            vehicleToOrderStopsMap.put(vehicle, new ArrayList<>());
            lastPositionMap.put(vehicle.getId(), vehicle.getCurrentPosition());
        }

        // Process each order
        for (Order order : pendingOrders) {
            ordersMap.put(order.getId(), order);

            // Determine how much GLP to deliver (randomly pick from options, but limited by order's remaining)
            int glpToDeliver = Math.min(order.getRemainingGlpM3(),
                    GLP_DELIVERY_OPTIONS[random.nextInt(GLP_DELIVERY_OPTIONS.length)]);

            // Find the best vehicle based on distance to the order
            Vehicle bestVehicle = null;
            double shortestDistance = Double.MAX_VALUE;

            for (Vehicle vehicle : availableVehicles) {
                // Verificar si el vehículo puede transportar esta cantidad de GLP (basado en capacidad máxima)
                if (glpToDeliver > vehicle.getGlpCapacityM3()) {
                    continue; // El vehículo no tiene suficiente capacidad máxima para esta entrega
                }
                
                Position lastPosition = lastPositionMap.get(vehicle.getId());
                PathResult pathResult = PathFinder.findPath(
                        environment,
                        lastPosition,
                        order.getPosition(),
                        environment.getCurrentTime());

                // Skip vehicles that can't reach the order
                if (pathResult == null) {
                    continue;
                }

                double distance = pathResult.getDistance();
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    bestVehicle = vehicle;
                }
            }

            // If we found a suitable vehicle, assign the order to it
            if (bestVehicle != null) {
                OrderStop orderStop = new OrderStop(
                        order.getId(),
                        order.getPosition(),
                        environment.getCurrentTime(), // This will be corrected by RouteFixer
                        glpToDeliver);

                vehicleToOrderStopsMap.get(bestVehicle).add(orderStop);
                lastPositionMap.put(bestVehicle.getId(), order.getPosition());
            }
        }

        // Create and fix routes
        List<Route> routes = new ArrayList<>();
        for (Vehicle vehicle : availableVehicles) {
            List<OrderStop> orderStops = vehicleToOrderStopsMap.get(vehicle);

            // Skip vehicles with no assigned orders
            if (orderStops.isEmpty()) {
                continue;
            }

            // Fix the route (add depot stops as needed for GLP and fuel)
            Route fixedRoute = RouteFixer.fixRoute(
                    environment,
                    orderStops,
                    vehicle,
                    environment.getCurrentTime());

            if (fixedRoute != null) {
                routes.add(fixedRoute);
            }
        }

        // Create the solution
        return new Solution(ordersMap, routes);
    }
}
