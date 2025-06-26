package com.vroute.alns.operators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.solution.DepotStop;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A repair operator that greedily inserts orders into routes.
 * Orders are inserted at the position that minimizes the insertion cost.
 */
public class GreedyInsertionOperator extends AbstractOperator implements RepairOperator {
    // Weights for the insertion cost function
    private final double alphaDistance;
    private final double betaTime;
    private final double gammaWait;
    
    /**
     * Creates a new greedy insertion operator.
     * 
     * @param alphaDistance Weight for distance increase
     * @param betaTime Weight for time delay
     * @param gammaWait Weight for waiting time
     */
    public GreedyInsertionOperator(double alphaDistance, double betaTime, double gammaWait) {
        super("GreedyInsertion");
        this.alphaDistance = alphaDistance;
        this.betaTime = betaTime;
        this.gammaWait = gammaWait;
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
        
        // Sort orders by urgency (closest deadline first)
        unassignedOrders.sort((o1, o2) -> o1.getDueTime().compareTo(o2.getDueTime()));
        
        // Try to insert each order into the best position
        for (Order order : unassignedOrders) {
            InsertionPosition bestPosition = null;
            double bestCost = Double.MAX_VALUE;
            
            // Try each route
            for (int routeIndex = 0; routeIndex < routes.size(); routeIndex++) {
                Route route = routes.get(routeIndex);
                Vehicle vehicle = route.getVehicle();
                
                // Skip this route if vehicle cannot handle the order's GLP requirement
                if (vehicle.getGlpCapacityM3() < order.getGlpRequestM3()) {
                    continue;
                }
                
                List<RouteStop> stops = route.getStops();
                
                // Try each position in the route
                for (int stopIndex = 0; stopIndex <= stops.size(); stopIndex++) {
                    InsertionCost cost = calculateInsertionCost(order, route, stopIndex, environment.getCurrentTime());
                    
                    if (cost != null && cost.getTotalCost() < bestCost) {
                        bestCost = cost.getTotalCost();
                        bestPosition = new InsertionPosition(routeIndex, stopIndex);
                    }
                }
            }
            
            // If a feasible position was found, insert the order
            if (bestPosition != null) {
                Route route = routes.get(bestPosition.routeIndex);
                List<RouteStop> stops = new ArrayList<>(route.getStops());
                
                // Create the order stop
                OrderStop orderStop = new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    calculateEstimatedArrivalTime(route, bestPosition.stopIndex, environment.getCurrentTime()),
                    order.getGlpRequestM3()
                );
                
                // Insert the order stop at the best position
                stops.add(bestPosition.stopIndex, orderStop);
                
                // Create a new route with the updated stops
                Route newRoute = new Route(route.getVehicle(), stops, route.getStartTime());
                routes.set(bestPosition.routeIndex, newRoute);
            } else {
                // If no feasible position was found, create a new route if possible
                for (Vehicle vehicle : environment.getVehicles()) {
                    // Skip vehicles that are already assigned or cannot handle the order
                    if (routes.stream().anyMatch(r -> r.getVehicle().getId().equals(vehicle.getId())) ||
                        vehicle.getGlpCapacityM3() < order.getGlpRequestM3()) {
                        continue;
                    }
                    
                    // Create a new route with just this order
                    List<RouteStop> stops = new ArrayList<>();
                    
                    // Add the order stop
                    OrderStop orderStop = new OrderStop(
                        order.getId(),
                        order.getPosition(),
                        environment.getCurrentTime().plusMinutes(
                            calculateTravelTime(environment.getMainDepot().getPosition(), order.getPosition())
                        ),
                        order.getGlpRequestM3()
                    );
                    stops.add(orderStop);
                    
                    // Create a new route
                    Route newRoute = new Route(vehicle, stops, environment.getCurrentTime());
                    routes.add(newRoute);
                    break;
                }
            }
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
     * @return An InsertionCost object, or null if the insertion is not feasible
     */
    private InsertionCost calculateInsertionCost(Order order, Route route, int position, LocalDateTime currentTime) {
        List<RouteStop> stops = route.getStops();
        
        // Get the positions before and after the insertion point
        Position prevPos = (position == 0) 
            ? route.getVehicle().getCurrentPosition() // Start from vehicle's position if first stop
            : stops.get(position - 1).getPosition();
        
        Position nextPos = (position == stops.size()) 
            ? prevPos // If inserting at the end, consider return to depot
            : stops.get(position).getPosition();
        
        // Calculate distances and times
        double distanceIncrease = prevPos.distanceTo(order.getPosition()) + 
                                  order.getPosition().distanceTo(nextPos) - 
                                  prevPos.distanceTo(nextPos);
        
        int travelTimeIncrease = calculateTravelTime(prevPos, order.getPosition()) + 
                                calculateTravelTime(order.getPosition(), nextPos) - 
                                calculateTravelTime(prevPos, nextPos);
        
        // Estimate arrival time at this order
        LocalDateTime estimatedArrival = calculateEstimatedArrivalTime(route, position, currentTime);
        
        // Check if arrival time is within the order's time window
        if (estimatedArrival.isBefore(order.getDueTime())) {
            // Need to wait until the earliest delivery time
            int waitTime = (int) java.time.Duration.between(estimatedArrival, order.getDueTime()).toMinutes();
            return new InsertionCost(distanceIncrease, travelTimeIncrease, waitTime);
        } else if (estimatedArrival.isAfter(order.getDueTime())) {
            // Arrival is too late, insertion is not feasible
            return null;
        }
        
        // No waiting time needed
        return new InsertionCost(distanceIncrease, travelTimeIncrease, 0);
    }
    
    /**
     * Estimate the arrival time at a specific position in a route.
     * 
     * @param route The route
     * @param position The position
     * @param currentTime The current time
     * @return The estimated arrival time
     */
    private LocalDateTime calculateEstimatedArrivalTime(Route route, int position, LocalDateTime currentTime) {
        if (position == 0) {
            // If inserting at the beginning, start from current time
            return currentTime;
        }
        
        List<RouteStop> stops = route.getStops();
        
        // For simplicity, just add up the travel times between stops
        LocalDateTime estimatedTime = currentTime;
        Position lastPos = route.getVehicle().getCurrentPosition();
        
        for (int i = 0; i < position; i++) {
            RouteStop stop = stops.get(i);
            Position currentPos = stop.getPosition();
            
            // Add travel time
            int travelTime = calculateTravelTime(lastPos, currentPos);
            estimatedTime = estimatedTime.plusMinutes(travelTime);
            
            // Add service time if it's an order stop
            if (stop instanceof OrderStop) {
                estimatedTime = estimatedTime.plusMinutes(15); // Default service time
            } else if (stop instanceof DepotStop) {
                estimatedTime = estimatedTime.plusMinutes(5); // Default depot time
            }
            
            lastPos = currentPos;
        }
        
        return estimatedTime;
    }
    
    /**
     * Calculate travel time in minutes between two positions.
     * 
     * @param from Starting position
     * @param to Ending position
     * @return Travel time in minutes
     */
    private int calculateTravelTime(Position from, Position to) {
        double distance = from.distanceTo(to);
        // Assuming average speed of 50 km/h
        // Time (hours) = Distance / Speed
        // Time (minutes) = Time (hours) * 60
        return (int) Math.ceil(distance / 50.0 * 60.0);
    }
    
    /**
     * Helper class to store insertion position (route index and position within route).
     */
    private static class InsertionPosition {
        final int routeIndex;
        final int stopIndex;
        
        InsertionPosition(int routeIndex, int stopIndex) {
            this.routeIndex = routeIndex;
            this.stopIndex = stopIndex;
        }
    }
    
    /**
     * Helper class to store insertion cost components.
     */
    private class InsertionCost {
        private final double distanceIncrease;
        private final double timeIncrease;
        private final double waitingTime;
        
        InsertionCost(double distanceIncrease, double timeIncrease, double waitingTime) {
            this.distanceIncrease = distanceIncrease;
            this.timeIncrease = timeIncrease;
            this.waitingTime = waitingTime;
        }
        
        double getTotalCost() {
            return alphaDistance * distanceIncrease + 
                   betaTime * timeIncrease + 
                   gammaWait * waitingTime;
        }
    }
}
