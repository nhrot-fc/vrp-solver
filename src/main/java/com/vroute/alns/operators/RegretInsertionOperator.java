package com.vroute.alns.operators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A repair operator that inserts orders based on regret value.
 * The regret value is the difference in cost between inserting an order
 * at its best position versus its second-best position.
 */
public class RegretInsertionOperator extends AbstractOperator implements RepairOperator {
    private final int regretDegree; // Typically 2 for basic regret insertion
    private final double alphaDistance;
    private final double betaTime;
    
    /**
     * Creates a new regret insertion operator.
     * 
     * @param regretDegree The regret degree (2 for basic regret-2)
     * @param alphaDistance Weight for distance component
     * @param betaTime Weight for time component
     */
    public RegretInsertionOperator(int regretDegree, double alphaDistance, double betaTime) {
        super("RegretInsertion");
        this.regretDegree = regretDegree;
        this.alphaDistance = alphaDistance;
        this.betaTime = betaTime;
    }
    
    @Override
    public Solution repair(Solution solution, Environment environment) {
        // Get a copy of the current solution
        Map<String, Order> orders = new HashMap<>(solution.getOrders());
        List<Route> routes = new ArrayList<>(solution.getRoutes());
        
        // Find all unassigned orders that need to be reinserted
        List<Order> unassignedOrders = new ArrayList<>();
        for (Order order : orders.values()) {
            boolean assigned = false;
            for (Route route : routes) {
                for (RouteStop stop : route.getStops()) {
                    if (stop instanceof OrderStop && ((OrderStop) stop).getEntityID().equals(order.getId())) {
                        assigned = true;
                        break;
                    }
                }
                if (assigned) break;
            }
            
            if (!assigned) {
                unassignedOrders.add(order);
            }
        }
        
        // No orders to insert, return the solution as is
        if (unassignedOrders.isEmpty()) {
            return solution;
        }
        
        // While there are still unassigned orders
        while (!unassignedOrders.isEmpty()) {
            // Calculate regret values for all unassigned orders
            OrderRegret bestRegret = null;
            
            for (Order order : unassignedOrders) {
                // Calculate insertion costs for all possible positions
                PriorityQueue<InsertionOption> options = new PriorityQueue<>(
                    Comparator.comparingDouble(InsertionOption::getCost)
                );
                
                // Try each route
                for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
                    Route route = routes.get(routeIndex);
                    List<RouteStop> stops = route.getStops();
                    
                    // Try each position in the route
                    for (int stopIndex = 0; stopIndex <= stops.size(); stopIndex++) {
                        double cost = calculateInsertionCost(order, route, stopIndex, environment.getCurrentTime());
                        
                        if (cost < Double.MAX_VALUE) { // If insertion is feasible
                            options.add(new InsertionOption(routeIndex, stopIndex, cost));
                        }
                    }
                }
                
                // Calculate regret value
                double regretValue = 0;
                double bestCost = Double.MAX_VALUE;
                InsertionOption bestOption = null;
                
                // Extract top k options (where k is the regret degree)
                List<InsertionOption> topOptions = new ArrayList<>();
                for (int i = 0; i < regretDegree && !options.isEmpty(); i++) {
                    InsertionOption option = options.poll();
                    topOptions.add(option);
                    
                    // Keep track of best option
                    if (i == 0) {
                        bestCost = option.cost;
                        bestOption = option;
                    }
                    
                    // Add to regret value (difference between this option and best option)
                    if (i > 0) {
                        regretValue += (option.cost - bestCost);
                    }
                }
                
                // If we have a feasible insertion
                if (bestOption != null) {
                    OrderRegret orderRegret = new OrderRegret(order, bestOption, regretValue);
                    
                    // Update best regret if this is better
                    if (bestRegret == null || orderRegret.regretValue > bestRegret.regretValue) {
                        bestRegret = orderRegret;
                    }
                }
            }
            
            // If no feasible insertion was found, break
            if (bestRegret == null) {
                break;
            }
            
            // Insert the order with the highest regret value
            Order orderToInsert = bestRegret.order;
            InsertionOption insertionOption = bestRegret.bestOption;
            
            Route route = routes.get(insertionOption.routeIndex);
            List<RouteStop> stops = new ArrayList<>(route.getStops());
            
            // Create the order stop
            OrderStop orderStop = new OrderStop(
                orderToInsert.getId(),
                orderToInsert.getPosition(),
                // For simplicity, use current time plus a travel time estimate
                environment.getCurrentTime().plusMinutes(30),
                10 // Assuming a default GLP amount
            );
            
            // Insert the order stop at the selected position
            stops.add(insertionOption.stopIndex, orderStop);
            
            // Create a new route with the updated stops
            Route newRoute = new Route(route.getId(), route.getVehicle(), stops);
            routes.set(insertionOption.routeIndex, newRoute);
            
            // Remove the inserted order from the unassigned list
            unassignedOrders.remove(orderToInsert);
        }
        
        // Return the repaired solution
        return new Solution(orders, routes);
    }
    
    /**
     * Calculate the cost of inserting an order at a specific position in a route.
     * 
     * @param order The order to insert
     * @param route The route to insert into
     * @param position The position to insert at
     * @param currentTime The current time
     * @return The insertion cost, or Double.MAX_VALUE if infeasible
     */
    private double calculateInsertionCost(Order order, Route route, int position, LocalDateTime currentTime) {
        List<RouteStop> stops = route.getStops();
        
        // Simple distance-based cost for now
        Position orderPos = order.getPosition();
        
        // Get the positions before and after the insertion point
        Position prevPos;
        if (position == 0) {
            // If inserting at the beginning, use depot position
            prevPos = new Position(0, 0); // Simplified
        } else {
            prevPos = stops.get(position - 1).getPosition();
        }
        
        Position nextPos;
        if (position == stops.size()) {
            // If inserting at the end, assume return to depot
            nextPos = new Position(0, 0); // Simplified
        } else {
            nextPos = stops.get(position).getPosition();
        }
        
        // Calculate distance increase
        double distanceIncrease = prevPos.distanceTo(orderPos) + 
                                   orderPos.distanceTo(nextPos) - 
                                   prevPos.distanceTo(nextPos);
        
        // Calculate time increase (simplified)
        double timeIncrease = distanceIncrease * 2; // Assuming 30km/h
        
        // Combined cost
        return alphaDistance * distanceIncrease + betaTime * timeIncrease;
    }
    
    /**
     * Helper class to store insertion option details.
     */
    private static class InsertionOption {
        final int routeIndex;
        final int stopIndex;
        final double cost;
        
        InsertionOption(int routeIndex, int stopIndex, double cost) {
            this.routeIndex = routeIndex;
            this.stopIndex = stopIndex;
            this.cost = cost;
        }
        
        double getCost() {
            return cost;
        }
    }
    
    /**
     * Helper class to store order regret information.
     */
    private static class OrderRegret {
        final Order order;
        final InsertionOption bestOption;
        final double regretValue;
        
        OrderRegret(Order order, InsertionOption bestOption, double regretValue) {
            this.order = order;
            this.bestOption = bestOption;
            this.regretValue = regretValue;
        }
    }
}
