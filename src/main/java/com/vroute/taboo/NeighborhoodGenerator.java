package com.vroute.taboo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.solution.OrderStop;
import com.vroute.solution.RandomDistributor;
import com.vroute.solution.Route;
import com.vroute.solution.Solution;

/**
 * Generates neighborhoods for the Tabu Search algorithm
 * Uses RouteFixer to ensure all solutions respect GLP and fuel constraints
 */
public class NeighborhoodGenerator {
    private static final Random random = new Random();

    private static void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[NeighborhoodGenerator] " + message);
        }
    }

    public static Solution generateInitialSolution(Environment env) {
        RandomDistributor distributor = new RandomDistributor();
        return distributor.solve(env);
    }

    public static List<Solution> generateNeighbors(Environment env, Solution solution, int maxNeighbors) {
        List<Solution> neighbors = new ArrayList<>();

        if (solution.getRoutes().isEmpty()) {
            return neighbors;
        }

        for (int i = 0; i < maxNeighbors; i++) {
            int operatorChoice = random.nextInt(10);
            Solution neighbor = null;

            switch (operatorChoice) {
                case 0:
                    // Swap two order stops within the same route
                    neighbor = swapWithinRoute(env, solution);
                    break;
                case 1:
                    // Move an order stop within the same route
                    neighbor = moveWithinRoute(env, solution);
                    break;
                case 2:
                    // Shuffle order stops within the same route from random i to random j
                    neighbor = shuffleWithinRoute(env, solution);
                    break;
                case 3:
                    // Swap two order stops between two routes
                    neighbor = swapBetweenRoutes(env, solution);
                    break;
                case 4:
                    // Move an order stop from one route to another
                    neighbor = moveBetweenRoutes(env, solution);
                    break;
                case 5:
                    // Swap vehicle between two routes
                    neighbor = swapVehicleBetweenRoutes(env, solution);
                    break;
                case 6:
                    // Reverse a sequence within a route
                    neighbor = reverseSequence(env, solution);
                    break;
                case 7:
                    // Two-opt move within a route
                    neighbor = twoOpt(env, solution);
                    break;
                case 8:
                    // Insert order stop at specific position in different route
                    neighbor = relocateOrder(env, solution);
                    break;
                case 9:
                    // Three-way exchange of order stops
                    neighbor = threeWayExchange(env, solution);
                    break;
            }
            if (neighbor != null) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    private static Solution swapWithinRoute(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or env is null or solution has no routes");
            return null;
        }

        int idx = random.nextInt(solution.getRoutes().size());
        Route route = solution.getRoutes().get(idx);
        List<OrderStop> orderStops = route.getOrderStops();

        if (orderStops.size() < 2) {
            debug("Route has less than 2 order stops");
            return null;
        }

        int stop1Idx = random.nextInt(orderStops.size());
        int stop2Idx = random.nextInt(orderStops.size());

        // Ensure we select different stops
        while (stop2Idx == stop1Idx) {
            stop2Idx = random.nextInt(orderStops.size());
        }

        OrderStop stop1 = orderStops.get(stop1Idx);
        OrderStop stop2 = orderStops.get(stop2Idx);

        orderStops.set(stop1Idx, stop2);
        orderStops.set(stop2Idx, stop1);

        Route newRoute = RouteFixer.fixRoute(env, orderStops, route.getVehicle(), route.getStartTime());
        if (newRoute == null) {
            debug("New route is null");
            return null;
        }

        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(idx, newRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution moveWithinRoute(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or env is null or solution has no routes");
            return null;
        }

        int idx = random.nextInt(solution.getRoutes().size());
        Route route = solution.getRoutes().get(idx);
        List<OrderStop> orderStops = route.getOrderStops();

        if (orderStops.size() < 2) {
            debug("Route has less than 2 order stops");
            return null;
        }

        int sourceIdx = random.nextInt(orderStops.size());
        int targetIdx = random.nextInt(orderStops.size());

        // Ensure we select different positions
        while (targetIdx == sourceIdx) {
            targetIdx = random.nextInt(orderStops.size());
        }

        OrderStop stop = orderStops.remove(sourceIdx);
        orderStops.add(targetIdx, stop);

        Route newRoute = RouteFixer.fixRoute(env, orderStops, route.getVehicle(), route.getStartTime());
        if (newRoute == null) {
            debug("New route is null");
            return null;
        }

        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(idx, newRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution shuffleWithinRoute(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or env is null or solution has no routes");
            return null;
        }

        int idx = random.nextInt(solution.getRoutes().size());
        Route route = solution.getRoutes().get(idx);
        List<OrderStop> orderStops = route.getOrderStops();

        if (orderStops.size() < 3) {
            debug("Route has less than 3 order stops");
            return null;
        }

        int start = random.nextInt(orderStops.size() - 1);
        int end = start + 1 + random.nextInt(orderStops.size() - start - 1);

        // Shuffle the sublist
        List<OrderStop> subList = new ArrayList<>(orderStops.subList(start, end + 1));
        Collections.shuffle(subList);

        // Replace the original section with the shuffled section
        for (int i = 0; i < subList.size(); i++) {
            orderStops.set(start + i, subList.get(i));
        }

        Route newRoute = RouteFixer.fixRoute(env, orderStops, route.getVehicle(), route.getStartTime());
        if (newRoute == null) {
            debug("New route is null");
            return null;
        }

        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(idx, newRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution swapBetweenRoutes(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().size() < 2) {
            debug("Solution is null or env is null or solution has less than 2 routes");
            return null;
        }

        int route1Idx = random.nextInt(solution.getRoutes().size());
        int route2Idx = random.nextInt(solution.getRoutes().size());

        // Ensure we select different routes
        while (route2Idx == route1Idx) {
            route2Idx = random.nextInt(solution.getRoutes().size());
        }

        Route route1 = solution.getRoutes().get(route1Idx);
        Route route2 = solution.getRoutes().get(route2Idx);

        List<OrderStop> orderStops1 = route1.getOrderStops();
        List<OrderStop> orderStops2 = route2.getOrderStops();

        if (orderStops1.isEmpty() || orderStops2.isEmpty()) {
            debug("One of the routes has no order stops");
            return null;
        }

        int stop1Idx = random.nextInt(orderStops1.size());
        int stop2Idx = random.nextInt(orderStops2.size());

        OrderStop stop1 = orderStops1.get(stop1Idx);
        OrderStop stop2 = orderStops2.get(stop2Idx);

        // Create new order stop lists with swapped stops
        List<OrderStop> newOrderStops1 = new ArrayList<>(orderStops1);
        List<OrderStop> newOrderStops2 = new ArrayList<>(orderStops2);

        newOrderStops1.set(stop1Idx, stop2);
        newOrderStops2.set(stop2Idx, stop1);

        // Fix routes
        Route newRoute1 = RouteFixer.fixRoute(env, newOrderStops1, route1.getVehicle(), route1.getStartTime());
        Route newRoute2 = RouteFixer.fixRoute(env, newOrderStops2, route2.getVehicle(), route2.getStartTime());

        if (newRoute1 == null || newRoute2 == null) {
            debug("One of the routes is null");
            return null;
        }

        // Create solution with new routes
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(route1Idx, newRoute1);
        newRoutes.set(route2Idx, newRoute2);

        return new Solution(newRoutes, env);
    }

    private static Solution moveBetweenRoutes(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().size() < 2) {
            debug("Solution is null or env is null or solution has less than 2 routes");
            return null;
        }

        int sourceRouteIdx = random.nextInt(solution.getRoutes().size());
        int targetRouteIdx = random.nextInt(solution.getRoutes().size());

        // Ensure we select different routes
        while (targetRouteIdx == sourceRouteIdx) {
            targetRouteIdx = random.nextInt(solution.getRoutes().size());
        }

        Route sourceRoute = solution.getRoutes().get(sourceRouteIdx);
        Route targetRoute = solution.getRoutes().get(targetRouteIdx);

        List<OrderStop> sourceStops = sourceRoute.getOrderStops();
        List<OrderStop> targetStops = targetRoute.getOrderStops();

        if (sourceStops.isEmpty()) {
            debug("Source route has no order stops");
            return null;
        }

        // Select a random stop from source route
        int stopIdx = random.nextInt(sourceStops.size());
        OrderStop stop = sourceStops.get(stopIdx);

        // Create new order stop lists
        List<OrderStop> newSourceStops = new ArrayList<>(sourceStops);
        newSourceStops.remove(stopIdx);

        List<OrderStop> newTargetStops = new ArrayList<>(targetStops);
        int insertPosition = targetStops.isEmpty() ? 0 : random.nextInt(targetStops.size() + 1);
        newTargetStops.add(insertPosition, stop);

        // Fix routes
        Route newSourceRoute = RouteFixer.fixRoute(env, newSourceStops, sourceRoute.getVehicle(),
                sourceRoute.getStartTime());
        Route newTargetRoute = RouteFixer.fixRoute(env, newTargetStops, targetRoute.getVehicle(),
                targetRoute.getStartTime());

        if (newSourceRoute == null || newTargetRoute == null) {
            debug("One of the routes is null");
            return null;
        }

        // Create solution with new routes
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(sourceRouteIdx, newSourceRoute);
        newRoutes.set(targetRouteIdx, newTargetRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution swapVehicleBetweenRoutes(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().size() < 2) {
            debug("Solution is null or env is null or solution has less than 2 routes");
            return null;
        }

        int route1Idx = random.nextInt(solution.getRoutes().size());
        int route2Idx = random.nextInt(solution.getRoutes().size());

        // Ensure we select different routes
        while (route2Idx == route1Idx) {
            route2Idx = random.nextInt(solution.getRoutes().size());
        }

        Route route1 = solution.getRoutes().get(route1Idx);
        Route route2 = solution.getRoutes().get(route2Idx);

        Vehicle vehicle1 = route1.getVehicle();
        Vehicle vehicle2 = route2.getVehicle();

        // Fix routes with swapped vehicles
        Route newRoute1 = RouteFixer.fixRoute(env, route1.getOrderStops(), vehicle2, route1.getStartTime());
        Route newRoute2 = RouteFixer.fixRoute(env, route2.getOrderStops(), vehicle1, route2.getStartTime());

        if (newRoute1 == null || newRoute2 == null) {
            debug("One of the routes is null");
            return null;
        }

        // Create solution with new routes
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(route1Idx, newRoute1);
        newRoutes.set(route2Idx, newRoute2);

        return new Solution(newRoutes, env);
    }

    private static Solution reverseSequence(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or env is null or solution has no routes");
            return null;
        }

        int routeIdx = random.nextInt(solution.getRoutes().size());
        Route route = solution.getRoutes().get(routeIdx);
        List<OrderStop> orderStops = route.getOrderStops();

        if (orderStops.size() < 3) {
            debug("Route has less than 3 order stops");
            return null;
        }

        int start = random.nextInt(orderStops.size() - 2);
        int length = 2 + random.nextInt(Math.min(5, orderStops.size() - start - 1));
        int end = Math.min(start + length - 1, orderStops.size() - 1);

        // Create a new list to hold reversed sequence
        List<OrderStop> newOrderStops = new ArrayList<>(orderStops);

        // Reverse the sublist
        for (int i = 0; i <= (end - start) / 2; i++) {
            OrderStop temp = newOrderStops.get(start + i);
            newOrderStops.set(start + i, newOrderStops.get(end - i));
            newOrderStops.set(end - i, temp);
        }

        // Fix the route
        Route newRoute = RouteFixer.fixRoute(env, newOrderStops, route.getVehicle(), route.getStartTime());
        if (newRoute == null) {
            debug("New route is null");
            return null;
        }

        // Create solution with the new route
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(routeIdx, newRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution twoOpt(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or env is null or solution has no routes");
            return null;
        }

        int routeIdx = random.nextInt(solution.getRoutes().size());
        Route route = solution.getRoutes().get(routeIdx);
        List<OrderStop> orderStops = route.getOrderStops();

        if (orderStops.size() < 4) {
            debug("Route has less than 4 order stops");
            return null;
        }

        // Select two random cut points
        int i = random.nextInt(orderStops.size() - 3);
        int j = i + 2 + random.nextInt(orderStops.size() - i - 2);

        List<OrderStop> newOrderStops = new ArrayList<>();

        // Add all stops up to i
        for (int k = 0; k <= i; k++) {
            newOrderStops.add(orderStops.get(k));
        }

        // Add stops from i+1 to j in reverse order
        for (int k = j; k > i; k--) {
            newOrderStops.add(orderStops.get(k));
        }

        // Add all stops after j
        for (int k = j + 1; k < orderStops.size(); k++) {
            newOrderStops.add(orderStops.get(k));
        }

        // Fix route
        Route newRoute = RouteFixer.fixRoute(env, newOrderStops, route.getVehicle(), route.getStartTime());
        if (newRoute == null) {
            debug("New route is null");
            return null;
        }

        // Create solution with the new route
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(routeIdx, newRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution relocateOrder(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or env is null or solution has no routes");
            return null;
        }

        if (solution.getRoutes().size() < 2) {
            debug("Solution has less than 2 routes");
            return null;
        }

        // Select source route with at least one order
        List<Integer> validRouteIndices = new ArrayList<>();
        for (int i = 0; i < solution.getRoutes().size(); i++) {
            if (!solution.getRoutes().get(i).getOrderStops().isEmpty()) {
                validRouteIndices.add(i);
            }
        }

        if (validRouteIndices.isEmpty()) {
            debug("No valid routes found");
            return null;
        }

        int sourceRouteIdx = validRouteIndices.get(random.nextInt(validRouteIndices.size()));
        int targetRouteIdx = random.nextInt(solution.getRoutes().size());

        // Ensure we select a different route for target
        while (targetRouteIdx == sourceRouteIdx) {
            targetRouteIdx = random.nextInt(solution.getRoutes().size());
        }

        Route sourceRoute = solution.getRoutes().get(sourceRouteIdx);
        Route targetRoute = solution.getRoutes().get(targetRouteIdx);

        int orderIdx = random.nextInt(sourceRoute.getOrderStops().size());
        OrderStop orderToMove = sourceRoute.getOrderStops().get(orderIdx);

        // Create new order lists
        List<OrderStop> newSourceStops = new ArrayList<>(sourceRoute.getOrderStops());
        newSourceStops.remove(orderIdx);

        List<OrderStop> newTargetStops = new ArrayList<>(targetRoute.getOrderStops());

        // Insert at best position based on distance
        int bestPosition = 0;
        double bestDistance = Double.MAX_VALUE;

        if (!newTargetStops.isEmpty()) {
            for (int i = 0; i <= newTargetStops.size(); i++) {
                // Try inserting at each position and calculate distance
                List<OrderStop> testStops = new ArrayList<>(newTargetStops);
                testStops.add(i, orderToMove);
                double distance = calculateRouteLength(testStops);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestPosition = i;
                }
            }
        }

        newTargetStops.add(bestPosition, orderToMove);

        // Fix routes
        Route newSourceRoute = RouteFixer.fixRoute(env, newSourceStops, sourceRoute.getVehicle(),
                sourceRoute.getStartTime());
        Route newTargetRoute = RouteFixer.fixRoute(env, newTargetStops, targetRoute.getVehicle(),
                targetRoute.getStartTime());

        if (newSourceRoute == null || newTargetRoute == null) {
            debug("One of the routes is null");
            return null;
        }

        // Create solution with new routes
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(sourceRouteIdx, newSourceRoute);
        newRoutes.set(targetRouteIdx, newTargetRoute);

        return new Solution(newRoutes, env);
    }

    private static Solution threeWayExchange(Environment env, Solution solution) {
        if (solution == null || env == null || solution.getRoutes().size() < 3) {
            debug("Solution is null or env is null or solution has less than 3 routes");
            return null;
        }

        // Select three different routes
        int route1Idx = random.nextInt(solution.getRoutes().size());
        int route2Idx = random.nextInt(solution.getRoutes().size());
        while (route2Idx == route1Idx) {
            route2Idx = random.nextInt(solution.getRoutes().size());
        }

        int route3Idx = random.nextInt(solution.getRoutes().size());
        while (route3Idx == route1Idx || route3Idx == route2Idx) {
            route3Idx = random.nextInt(solution.getRoutes().size());
        }

        Route route1 = solution.getRoutes().get(route1Idx);
        Route route2 = solution.getRoutes().get(route2Idx);
        Route route3 = solution.getRoutes().get(route3Idx);

        List<OrderStop> stops1 = route1.getOrderStops();
        List<OrderStop> stops2 = route2.getOrderStops();
        List<OrderStop> stops3 = route3.getOrderStops();

        // Ensure all routes have orders
        if (stops1.isEmpty() || stops2.isEmpty() || stops3.isEmpty()) {
            debug("One of the routes has no order stops");
            return null;
        }

        // Select a random order from each route
        int stop1Idx = random.nextInt(stops1.size());
        int stop2Idx = random.nextInt(stops2.size());
        int stop3Idx = random.nextInt(stops3.size());

        OrderStop stop1 = stops1.get(stop1Idx);
        OrderStop stop2 = stops2.get(stop2Idx);
        OrderStop stop3 = stops3.get(stop3Idx);

        // Create new order lists with rotated stops (1->2, 2->3, 3->1)
        List<OrderStop> newStops1 = new ArrayList<>(stops1);
        List<OrderStop> newStops2 = new ArrayList<>(stops2);
        List<OrderStop> newStops3 = new ArrayList<>(stops3);

        newStops1.set(stop1Idx, stop3);
        newStops2.set(stop2Idx, stop1);
        newStops3.set(stop3Idx, stop2);

        // Fix routes
        Route newRoute1 = RouteFixer.fixRoute(env, newStops1, route1.getVehicle(), route1.getStartTime());
        Route newRoute2 = RouteFixer.fixRoute(env, newStops2, route2.getVehicle(), route2.getStartTime());
        Route newRoute3 = RouteFixer.fixRoute(env, newStops3, route3.getVehicle(), route3.getStartTime());

        if (newRoute1 == null || newRoute2 == null || newRoute3 == null) {
            debug("One of the routes is null");
            return null;
        }

        // Create solution with new routes
        List<Route> newRoutes = new ArrayList<>(solution.getRoutes());
        newRoutes.set(route1Idx, newRoute1);
        newRoutes.set(route2Idx, newRoute2);
        newRoutes.set(route3Idx, newRoute3);

        return new Solution(newRoutes, env);
    }

    // Helper method to roughly calculate route length for heuristic purposes
    private static double calculateRouteLength(List<OrderStop> stops) {
        if (stops.size() < 2) {
            return 0;
        }

        double distance = 0;
        for (int i = 0; i < stops.size() - 1; i++) {
            Position pos1 = stops.get(i).getPosition();
            Position pos2 = stops.get(i + 1).getPosition();
            distance += pos1.distanceTo(pos2);
        }
        return distance;
    }
}
