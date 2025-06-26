package com.vroute.taboo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.models.Vehicle;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.Solution;
import com.vroute.solution.RouteStop;

public class SwapVehicleMove implements TabuMove {
    private final Random random = new Random();

    public void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[SWAP_VEHICLE_MOVE] " + message);
        }
    }

    @Override
    public Solution apply(Environment env, Solution solution) {
        // Operation details:
        // 1. Select two random Routes from the solution
        // 2. Extract all OrderStops from the Routes
        // 3. Swap the Vehicles between the Routes
        // 4. Repair the Routes to ensure they are valid
        // 5. Return the solution with the modified Routes

        if (solution == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or has no routes");
            return solution;
        }

        Solution clonedSolution = solution.clone();
        List<Route> routes = clonedSolution.getRoutes();

        if (routes.size() < 2) {
            debug("Solution has less than 2 routes");
            return solution;
        }

        int randomRouteIndex1 = random.nextInt(routes.size());
        int randomRouteIndex2 = random.nextInt(routes.size());
        if (randomRouteIndex1 == randomRouteIndex2) {
            debug("Selected the same route for swapping");
            return solution;
        }

        Route route1 = routes.get(randomRouteIndex1);
        Route route2 = routes.get(randomRouteIndex2);

        Vehicle vehicle1 = route1.getVehicle().clone();
        Vehicle vehicle2 = route2.getVehicle().clone();

        List<OrderStop> orderStops1 = new ArrayList<>();
        for (RouteStop stop : route1.getStops()) {
            if (stop instanceof OrderStop) {
                orderStops1.add((OrderStop) stop);
            }
        }

        List<OrderStop> orderStops2 = new ArrayList<>();
        for (RouteStop stop : route2.getStops()) {
            if (stop instanceof OrderStop) {
                orderStops2.add((OrderStop) stop);
            }
        }

        // Don't swap vehicles if either route has no order stops
        if (orderStops1.isEmpty() || orderStops2.isEmpty()) {
            debug("One of the routes has no order stops");
            return solution;
        }

        Route fixedRoute1 = RouteFixer.fixRoute(env, orderStops1, vehicle2, route1.getStartTime());
        Route fixedRoute2 = RouteFixer.fixRoute(env, orderStops2, vehicle1, route2.getStartTime());

        if (fixedRoute1 == null || fixedRoute2 == null) {
            debug("Failed to fix one of the routes after swapping");
            return solution;
        }

        routes.set(randomRouteIndex1, fixedRoute1);
        routes.set(randomRouteIndex2, fixedRoute2);
        return new Solution(solution.getOrders(), routes);
    }
}
