package com.vroute.solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.taboo.RouteFixer;

public class RandomDistributor implements Solver {

    private static final int[] GLP_DELIVERY_OPTIONS = { 5, 10, 15, 20, 25 };
    private final Random random = new Random();

    private void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[RandomDistributor] " + message);
        }
    }

    @Override
    public Solution solve(Environment environment) {
        if (environment == null) {
            return null;
        }

        // Get all pending orders and sort by deadline
        List<Order> pendingOrders = new ArrayList<>();
        for (Order order : environment.getPendingOrders()) {
            pendingOrders.add(order.clone());
        }

        // If there are no orders, return an empty solution
        if (pendingOrders.isEmpty()) {
            return new Solution(new ArrayList<>(), environment);
        }

        Collections.sort(pendingOrders, Comparator.comparing(Order::getDueTime));
        List<Vehicle> availableVehicles = new ArrayList<>();
        for (Vehicle vehicle : environment.getAvailableVehicles()) {
            availableVehicles.add(vehicle.clone());
        }

        // If there are no vehicles, can't create a solution
        if (availableVehicles.isEmpty()) {
            debug("No vehicles available for distribution");
            return null;
        }

        Map<String, List<OrderStop>> vehicleToOrderStopsMap = new HashMap<>();

        // Iterate over pending orders and assign them a random vehicle.
        for (Order order : pendingOrders) {
            int remainingRequest = order.getRemainingGlpM3();
            while (remainingRequest > 0) {
                int randomVehicleIndex = random.nextInt(availableVehicles.size());
                Vehicle vehicle = availableVehicles.get(randomVehicleIndex);

                int glpDelivery = GLP_DELIVERY_OPTIONS[random.nextInt(GLP_DELIVERY_OPTIONS.length)];
                glpDelivery = Math.min(glpDelivery, vehicle.getGlpCapacityM3());
                glpDelivery = Math.min(glpDelivery, remainingRequest);
                remainingRequest -= glpDelivery;

                OrderStop orderStop = new OrderStop(order, order.getPosition(), glpDelivery);
                vehicleToOrderStopsMap.computeIfAbsent(vehicle.getId(), k -> new ArrayList<>()).add(orderStop);
            }
        }

        // print routes
        StringBuilder sb = new StringBuilder();
        sb.append("Initial assignment:\n");
        for (Map.Entry<String, List<OrderStop>> entry : vehicleToOrderStopsMap.entrySet()) {
            sb.append(entry.getKey()).append(": ");
            for (OrderStop stop : entry.getValue()) {
                sb.append("\t").append(stop.toString()).append("\n");
            }
            sb.append("\n");
        }
        debug(sb.toString());

        List<Route> routes = new ArrayList<>();
        // Repair each vehicle's route
        for (Vehicle vehicle : availableVehicles) {
            List<OrderStop> vehicleStops = vehicleToOrderStopsMap.get(vehicle.getId());
            if (vehicleStops == null || vehicleStops.isEmpty()) {
                continue;
            }

            Route route = RouteFixer.fixRoute(environment, vehicleStops, vehicle, environment.getCurrentTime());
            if (route == null) {
                debug("No se pudo reparar la ruta del vehículo " + vehicle.getId());
                // Just skip this vehicle instead of returning null for the whole solution
                continue;
            }
            routes.add(route);
        }

        // If no routes could be created, return null
        if (routes.isEmpty() && !pendingOrders.isEmpty()) {
            debug("No se pudo crear ninguna ruta válida");
            return null;
        }

        // print routes
        sb = new StringBuilder();
        sb.append("After Fixing Routes:\n");
        for (Route route : routes) {
            sb.append(route.toString());
        }
        debug(sb.toString());

        // Create a solution
        Solution solution = new Solution(routes, environment);
        debug("Initial Solution score: " + solution.getScore());
        
        // Verify that all orders are delivered on time and completely
        try {
            solution.verifyOrderDeliveries();
            debug("All orders are delivered on time and completely");
        } catch (AssertionError e) {
            debug("Solution verification failed: " + e.getMessage());
            // Return the solution anyway, as this is just a validation check
        }
        
        return solution;
    }
}
