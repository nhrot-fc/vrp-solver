package com.vroute.alns.operators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A destroy operator that randomly removes orders from routes.
 */
public class RandomRemovalOperator extends AbstractOperator implements DestroyOperator {
    private final Random random;
    
    /**
     * Creates a new random removal operator.
     */
    public RandomRemovalOperator() {
        super("RandomRemoval");
        this.random = new Random();
    }
    
    @Override
    public Solution destroy(Solution solution, Environment environment, int removalCount) {
        // Get a copy of the current solution
        Map<String, Order> orders = new HashMap<>(solution.getOrders());
        List<Route> routes = new ArrayList<>(solution.getRoutes());
        
        // Create a list of all OrderStops across all routes
        List<RouteOrderPair> orderStops = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            for (int j = 0; j < route.getStops().size(); j++) {
                RouteStop stop = route.getStops().get(j);
                if (stop instanceof OrderStop) {
                    orderStops.add(new RouteOrderPair(i, j, (OrderStop)stop));
                }
            }
        }
        
        // Shuffle the order stops to randomize removal
        Collections.shuffle(orderStops, random);
        
        // Remove up to removalCount order stops, or as many as we have
        int actualRemovalCount = Math.min(removalCount, orderStops.size());
        Map<Integer, List<Integer>> removalsPerRoute = new HashMap<>();
        
        for (int i = 0; i < actualRemovalCount; i++) {
            RouteOrderPair pair = orderStops.get(i);
            
            // Keep track of removals per route, but don't actually remove them yet
            // (as that would mess up the indices)
            removalsPerRoute.computeIfAbsent(pair.routeIndex, k -> new ArrayList<>())
                            .add(pair.stopIndex);
        }
        
        // Now process each route and remove the stops (in reverse order to maintain indices)
        for (Map.Entry<Integer, List<Integer>> entry : removalsPerRoute.entrySet()) {
            int routeIndex = entry.getKey();
            List<Integer> stopIndices = entry.getValue();
            
            // Sort in descending order to remove from back to front (preserves indices)
            Collections.sort(stopIndices, Collections.reverseOrder());
            
            Route route = routes.get(routeIndex);
            List<RouteStop> stops = new ArrayList<>(route.getStops());
            
            for (int stopIndex : stopIndices) {                
                // Remove the stop
                stops.remove(stopIndex);
            }
            
            // Create a new route with the remaining stops
            Route newRoute = new Route(route.getVehicle(), stops, route.getStartTime());
            routes.set(routeIndex, newRoute);
        }
        
        // Remove empty routes
        routes = routes.stream()
                      .filter(r -> !r.getStops().isEmpty())
                      .collect(Collectors.toList());
        
        // Return the modified solution
        return new Solution(orders, routes);
    }
    
    /**
     * Helper class to keep track of route index, stop index, and the order stop.
     */
    private static class RouteOrderPair {
        final int routeIndex;
        final int stopIndex;
        final OrderStop orderStop;
        
        RouteOrderPair(int routeIndex, int stopIndex, OrderStop orderStop) {
            this.routeIndex = routeIndex;
            this.stopIndex = stopIndex;
            this.orderStop = orderStop;
        }
    }
}
