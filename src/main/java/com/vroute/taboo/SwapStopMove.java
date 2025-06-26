package com.vroute.taboo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vroute.models.Constants;
import com.vroute.models.Environment;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

public class SwapStopMove implements TabuMove {
    private final Random random = new Random();
    
    public void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[SWAP_STOP_MOVE] " + message);
        }
    }
    
    @Override
    public Solution apply(Environment env, Solution solution) {
        // Operation details:
        // 1. Select two random Routes from the solution
        // 2. Extract all OrderStops from the Routes
        // 3. Select two random OrderStops from the Routes
        // 4. Swap the OrderStops between the Routes
        // 5. Repair the Routes to ensure they are valid
        // 6. Return the solution with the modified Routes

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

        if (orderStops1.isEmpty() || orderStops2.isEmpty()) {
            debug("One of the routes has no order stops");
            return solution;
        }
        
        int randomOrderStopIndex1 = random.nextInt(orderStops1.size());
        int randomOrderStopIndex2 = random.nextInt(orderStops2.size());

        // Store the stops to swap
        OrderStop stop1 = orderStops1.get(randomOrderStopIndex1);
        OrderStop stop2 = orderStops2.get(randomOrderStopIndex2);
        
        // Perform the swap
        orderStops1.set(randomOrderStopIndex1, stop2);
        orderStops2.set(randomOrderStopIndex2, stop1);

        Route fixedRoute1 = RouteFixer.fixRoute(env, orderStops1, route1.getVehicle(), route1.getStartTime());
        Route fixedRoute2 = RouteFixer.fixRoute(env, orderStops2, route2.getVehicle(), route2.getStartTime());

        if (fixedRoute1 == null || fixedRoute2 == null) {
            debug("Failed to fix one of the routes after swapping");
            return solution;
        }

        routes.set(randomRouteIndex1, fixedRoute1);
        routes.set(randomRouteIndex2, fixedRoute2);
        return new Solution(solution.getOrders(), routes);
    }
}
