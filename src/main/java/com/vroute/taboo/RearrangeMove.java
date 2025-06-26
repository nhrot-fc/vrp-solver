package com.vroute.taboo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

public class RearrangeMove implements TabuMove {
    private final Random random = new Random();

    public void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[REARRANGE_MOVE] " + message);
        }
    }

    @Override
    public Solution apply(Environment env, Solution solution) {
        // Operation details:
        // 1. Select a random Route from the solution
        // 2. Extract all OrderStops from the Route
        // 3. Select a random i and j indices (i < j)
        // 4. Shuffle the OrderStops between i and j
        // 5. Repair the Route to ensure it is valid
        // 6. Return the solution with the modified Route

        if (solution == null || solution.getRoutes().isEmpty()) {
            debug("Solution is null or has no routes");
            return solution;
        }

        List<Route> routes = solution.getRoutes();

        int randomRouteIndex = random.nextInt(routes.size());
        Route route = routes.get(randomRouteIndex);

        List<RouteStop> stops = route.getStops();

        // Extract all OrderStops
        List<OrderStop> orderStops = new ArrayList<>();
        for (RouteStop stop : stops) {
            if (stop instanceof OrderStop) {
                orderStops.add((OrderStop) stop);
            }
        }

        // If there are less than 2 order stops, no rearrangement is possible
        if (orderStops.size() < 2) {
            debug("Not enough order stops (" + orderStops.size() + "), no rearrangement possible");
            return solution;
        }

        Collections.shuffle(orderStops, random);

        // Fix the route
        Route fixedRoute = RouteFixer.fixRoute(env, orderStops, route.getVehicle(), route.getStartTime());
        if (fixedRoute == null) {
            debug("Failed to fix route after rearrangement, reverting to original solution");
            return solution;
        }

        routes.set(randomRouteIndex, fixedRoute);
        return new Solution(solution.getOrders(), routes);
    }
}
