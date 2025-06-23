package com.vroute.taboo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * Generates neighborhood moves for tabu search
 */
public class MoveGenerator {
    private final Random random;
    
    public MoveGenerator() {
        this.random = new Random();
    }
    
    /**
     * Generate a random move from all possible move types
     * @param solution Current solution
     * @return A random TabuMove
     */
    public TabuMove generateRandomMove(Solution solution) {
        int moveType = random.nextInt(5); // 5 different move types
        
        switch (moveType) {
            case 0:
                return generateRelocateMove(solution);
            case 1:
                return generateSwapMove(solution);
            case 2:
                return generateTwoOptMove(solution);
            case 3:
                return generateInterRouteRelocateMove(solution);
            case 4:
                return generateInterRouteSwapMove(solution);
            default:
                return generateRelocateMove(solution); // Default to relocate move
        }
    }
    
    /**
     * Generate a random relocate move
     * @param solution Current solution
     * @return A relocate move
     */
    public RelocateMove generateRelocateMove(Solution solution) {
        List<Route> routes = solution.getRoutes();
        if (routes.isEmpty()) {
            return null;
        }
        
        // Select a random route with at least one OrderStop
        List<Integer> validRouteIndices = getRoutesWithOrderStops(solution);
        if (validRouteIndices.isEmpty()) {
            return null;
        }
        
        int routeIndex = validRouteIndices.get(random.nextInt(validRouteIndices.size()));
        Route route = routes.get(routeIndex);
        List<RouteStop> stops = route.getStops();
        
        // Find all OrderStop indices
        List<Integer> orderStopIndices = findOrderStopIndices(stops);
        if (orderStopIndices.isEmpty()) {
            return null;
        }
        
        // Select a random OrderStop to move
        int fromIndex = orderStopIndices.get(random.nextInt(orderStopIndices.size()));
        
        // Select a random insertion point (avoiding the fromIndex and depot stops)
        int toIndex;
        do {
            toIndex = 1 + random.nextInt(stops.size() - 2); // Avoid first and last positions (depots)
        } while (toIndex == fromIndex);
        
        return new RelocateMove(routeIndex, fromIndex, toIndex);
    }
    
    /**
     * Generate a random swap move
     * @param solution Current solution
     * @return A swap move
     */
    public SwapMove generateSwapMove(Solution solution) {
        List<Route> routes = solution.getRoutes();
        if (routes.isEmpty()) {
            return null;
        }
        
        // Select a random route with at least two OrderStops
        List<Integer> validRouteIndices = getRoutesWithMultipleOrderStops(solution);
        if (validRouteIndices.isEmpty()) {
            return null;
        }
        
        int routeIndex = validRouteIndices.get(random.nextInt(validRouteIndices.size()));
        Route route = routes.get(routeIndex);
        List<RouteStop> stops = route.getStops();
        
        // Find all OrderStop indices
        List<Integer> orderStopIndices = findOrderStopIndices(stops);
        if (orderStopIndices.size() < 2) {
            return null;
        }
        
        // Select two different random OrderStops to swap
        int firstIdx = random.nextInt(orderStopIndices.size());
        int secondIdx;
        do {
            secondIdx = random.nextInt(orderStopIndices.size());
        } while (secondIdx == firstIdx);
        
        int firstIndex = orderStopIndices.get(firstIdx);
        int secondIndex = orderStopIndices.get(secondIdx);
        
        return new SwapMove(routeIndex, firstIndex, secondIndex);
    }
    
    /**
     * Generate a random 2-opt move
     * @param solution Current solution
     * @return A 2-opt move
     */
    public TwoOptMove generateTwoOptMove(Solution solution) {
        List<Route> routes = solution.getRoutes();
        if (routes.isEmpty()) {
            return null;
        }
        
        // Select a random route with at least 4 stops (we need at least 2 OrderStops to invert)
        List<Integer> validRouteIndices = getRoutesWithMultipleOrderStops(solution);
        if (validRouteIndices.isEmpty()) {
            return null;
        }
        
        int routeIndex = validRouteIndices.get(random.nextInt(validRouteIndices.size()));
        Route route = routes.get(routeIndex);
        List<RouteStop> stops = route.getStops();
        
        // Choose two random positions (ensuring they are at least 2 positions apart)
        int minPos = 1; // Skip first depot
        int maxPos = stops.size() - 2; // Skip last depot
        
        if (maxPos - minPos < 2) {
            return null; // Not enough stops to perform 2-opt
        }
        
        int fromIndex = minPos + random.nextInt(maxPos - minPos - 1);
        int toIndex = fromIndex + 1 + random.nextInt(maxPos - fromIndex);
        
        return new TwoOptMove(routeIndex, fromIndex, toIndex);
    }
    
    /**
     * Generate a random inter-route relocate move
     * @param solution Current solution
     * @return An inter-route relocate move
     */
    public InterRouteRelocateMove generateInterRouteRelocateMove(Solution solution) {
        List<Route> routes = solution.getRoutes();
        if (routes.size() < 2) {
            return null; // Need at least 2 routes
        }
        
        // Select a source route with at least one OrderStop
        List<Integer> sourceRouteIndices = getRoutesWithOrderStops(solution);
        if (sourceRouteIndices.isEmpty()) {
            return null;
        }
        
        int fromRouteIndex = sourceRouteIndices.get(random.nextInt(sourceRouteIndices.size()));
        
        // Select a different target route
        int toRouteIndex;
        do {
            toRouteIndex = random.nextInt(routes.size());
        } while (toRouteIndex == fromRouteIndex);
        
        Route fromRoute = routes.get(fromRouteIndex);
        Route toRoute = routes.get(toRouteIndex);
        
        // Find an OrderStop in the source route
        List<Integer> orderStopIndices = findOrderStopIndices(fromRoute.getStops());
        if (orderStopIndices.isEmpty()) {
            return null;
        }
        
        // Select a random OrderStop to move
        int fromStopIndex = orderStopIndices.get(random.nextInt(orderStopIndices.size()));
        
        // Select a random insertion point in the target route
        int toStopIndex = 1 + random.nextInt(toRoute.getStops().size() - 1); // Avoid first position (start depot)
        
        return new InterRouteRelocateMove(fromRouteIndex, fromStopIndex, toRouteIndex, toStopIndex);
    }
    
    /**
     * Generate a random inter-route swap move
     * @param solution Current solution
     * @return An inter-route swap move
     */
    public InterRouteSwapMove generateInterRouteSwapMove(Solution solution) {
        List<Route> routes = solution.getRoutes();
        if (routes.size() < 2) {
            return null; // Need at least 2 routes
        }
        
        // Select two different routes with at least one OrderStop each
        List<Integer> routesWithOrderStops = getRoutesWithOrderStops(solution);
        if (routesWithOrderStops.size() < 2) {
            return null;
        }
        
        // Select first route
        int firstRouteIdx = random.nextInt(routesWithOrderStops.size());
        int firstRouteIndex = routesWithOrderStops.get(firstRouteIdx);
        
        // Select second route (different from first)
        int secondRouteIdx;
        do {
            secondRouteIdx = random.nextInt(routesWithOrderStops.size());
        } while (secondRouteIdx == firstRouteIdx);
        int secondRouteIndex = routesWithOrderStops.get(secondRouteIdx);
        
        Route firstRoute = routes.get(firstRouteIndex);
        Route secondRoute = routes.get(secondRouteIndex);
        
        // Find OrderStops in each route
        List<Integer> firstOrderStopIndices = findOrderStopIndices(firstRoute.getStops());
        List<Integer> secondOrderStopIndices = findOrderStopIndices(secondRoute.getStops());
        
        if (firstOrderStopIndices.isEmpty() || secondOrderStopIndices.isEmpty()) {
            return null;
        }
        
        // Select a random OrderStop from each route
        int firstStopIndex = firstOrderStopIndices.get(random.nextInt(firstOrderStopIndices.size()));
        int secondStopIndex = secondOrderStopIndices.get(random.nextInt(secondOrderStopIndices.size()));
        
        return new InterRouteSwapMove(firstRouteIndex, firstStopIndex, secondRouteIndex, secondStopIndex);
    }
    
    /**
     * Find all routes that have at least one OrderStop
     */
    private List<Integer> getRoutesWithOrderStops(Solution solution) {
        List<Integer> validRoutes = new ArrayList<>();
        List<Route> routes = solution.getRoutes();
        
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            if (route.getStops().stream().anyMatch(s -> s instanceof OrderStop)) {
                validRoutes.add(i);
            }
        }
        
        return validRoutes;
    }
    
    /**
     * Find all routes that have at least two OrderStops
     */
    private List<Integer> getRoutesWithMultipleOrderStops(Solution solution) {
        List<Integer> validRoutes = new ArrayList<>();
        List<Route> routes = solution.getRoutes();
        
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            long orderStopCount = route.getStops().stream()
                    .filter(s -> s instanceof OrderStop)
                    .count();
            
            if (orderStopCount >= 2) {
                validRoutes.add(i);
            }
        }
        
        return validRoutes;
    }
    
    /**
     * Find indices of all OrderStops in a route
     */
    private List<Integer> findOrderStopIndices(List<RouteStop> stops) {
        List<Integer> indices = new ArrayList<>();
        
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i) instanceof OrderStop) {
                indices.add(i);
            }
        }
        
        return indices;
    }
} 