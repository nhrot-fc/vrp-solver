package com.vroute.alns.operators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.stream.Collectors;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A destroy operator that removes orders with the highest cost contribution.
 */
public class WorstRemovalOperator extends AbstractOperator implements DestroyOperator {
    private final Random random;
    private final double deterministicFactor; // 0 = fully random, 1 = fully deterministic
    
    /**
     * Creates a new worst removal operator.
     * 
     * @param deterministicFactor Controls randomness in selection (0-1)
     */
    public WorstRemovalOperator(double deterministicFactor) {
        super("WorstRemoval");
        this.random = new Random();
        this.deterministicFactor = Math.max(0.0, Math.min(1.0, deterministicFactor));
    }
    
    @Override
    public Solution destroy(Solution solution, Environment environment, int removalCount) {
        // Get a copy of the current solution
        Map<String, Order> orders = new HashMap<>(solution.getOrders());
        List<Route> routes = new ArrayList<>(solution.getRoutes());
        
        // Calculate cost contribution of each order stop
        PriorityQueue<OrderCostPair> costQueue = new PriorityQueue<>(
            Comparator.comparingDouble(pair -> -pair.costContribution) // Descending order (highest cost first)
        );
        
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            List<RouteStop> stops = route.getStops();
            
            for (int j = 0; j < stops.size(); j++) {
                if (stops.get(j) instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stops.get(j);
                    double costContribution = calculateCostContribution(orderStop, stops, j);
                    
                    // Apply deterministic/random factor
                    if (deterministicFactor < 1.0) {
                        // Add some randomness to the score
                        double randomFactor = 1.0 - deterministicFactor; // how much randomness to add
                        double noise = (random.nextDouble() * 2 - 1) * randomFactor; // between -randomFactor and +randomFactor
                        costContribution = costContribution * (1 + noise);
                    }
                    
                    costQueue.add(new OrderCostPair(i, j, orderStop, costContribution));
                }
            }
        }
        
        // Extract the orders with highest cost contribution
        int actualRemovalCount = Math.min(removalCount, costQueue.size());
        Map<Integer, List<Integer>> removalsPerRoute = new HashMap<>();
        
        for (int i = 0; i < actualRemovalCount; i++) {
            OrderCostPair pair = costQueue.poll();
            if (pair == null) break;
            
            // Keep track of removals per route
            removalsPerRoute.computeIfAbsent(pair.routeIndex, k -> new ArrayList<>())
                            .add(pair.stopIndex);
        }
        
        // Now process each route and remove the stops (in reverse order to maintain indices)
        for (Map.Entry<Integer, List<Integer>> entry : removalsPerRoute.entrySet()) {
            int routeIndex = entry.getKey();
            List<Integer> stopIndices = entry.getValue();
            
            // Sort in descending order to remove from back to front
            stopIndices.sort(Comparator.reverseOrder());
            
            Route route = routes.get(routeIndex);
            List<RouteStop> stops = new ArrayList<>(route.getStops());
            
            for (int stopIndex : stopIndices) {
                OrderStop orderStop = (OrderStop)stops.get(stopIndex);
                
                // Mark order as not delivered
                Order order = orders.get(orderStop.getEntityID());
                if (order != null) {
                    // We simply remove the stop without changing the order status
                    // as this is handled by the repair operator
                }
                
                // Remove the stop
                stops.remove(stopIndex);
            }
            
            // Create a new route with the remaining stops
            Route newRoute = new Route(route.getId(), route.getVehicle(), stops);
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
     * Calculate how much this order contributes to the total route cost.
     * Higher values mean the order is more expensive (worse) to include in the route.
     */
    private double calculateCostContribution(OrderStop orderStop, List<RouteStop> stops, int stopIndex) {
        // Simple heuristic: calculate detour distance caused by this stop
        if (stops.size() < 3 || stopIndex == 0 || stopIndex >= stops.size() - 1) {
            // If this is the only stop or first/last stop, just use its position
            return 0.0;
        }
        
        Position prevPos = stops.get(stopIndex - 1).getPosition();
        Position currPos = orderStop.getPosition();
        Position nextPos = stops.get(stopIndex + 1).getPosition();
        
        // Calculate distance with and without this stop
        double distanceWithStop = prevPos.distanceTo(currPos) + currPos.distanceTo(nextPos);
        double distanceWithoutStop = prevPos.distanceTo(nextPos);
        
        // The detour cost is the additional distance caused by this stop
        return distanceWithStop - distanceWithoutStop;
    }
    
    /**
     * Helper class to keep track of route index, stop index, order stop, and cost.
     */
    private static class OrderCostPair {
        final int routeIndex;
        final int stopIndex;
        final OrderStop orderStop;
        final double costContribution;
        
        OrderCostPair(int routeIndex, int stopIndex, OrderStop orderStop, double costContribution) {
            this.routeIndex = routeIndex;
            this.stopIndex = stopIndex;
            this.orderStop = orderStop;
            this.costContribution = costContribution;
        }
    }
}
