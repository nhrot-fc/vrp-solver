package com.vroute.solution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

/**
 * Class responsible for evaluating and scoring solutions and routes
 */
public class SolutionEvaluator {
    
    // Constants for penalty calculation
    private static final double LATE_DELIVERY_PENALTY_PER_HOUR = 500.0;
    private static final double UNDELIVERED_ORDER_PENALTY = 10000.0;
    private static final double DISTANCE_COST_PER_KM = 10.0;
    
    /**
     * Evaluates a solution and returns its total cost.
     * The cost includes:
     * - Penalties for late deliveries
     * - Penalties for undelivered orders
     * - Cost for distance traveled
     * 
     * @param solution The solution to evaluate
     * @return The total cost of the solution
     */
    public static double evaluateSolution(Solution solution) {
        if (solution == null) return Double.POSITIVE_INFINITY;
        
        Map<String, Order> orders = solution.getOrders();
        List<Route> routes = solution.getRoutes();
        
        // If there are no orders, the cost is 0
        if (orders.isEmpty()) return 0.0;
        
        // If there are orders but no routes, the cost is infinity
        if (routes.isEmpty()) return Double.POSITIVE_INFINITY;
        
        double totalCost = 0.0;
        
        // Calculate cost for each route
        for (Route route : routes) {
            double routeCost = evaluateRoute(route, orders);
            if (routeCost == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY; // If any route is invalid, the entire solution is invalid
            }
            totalCost += routeCost;
        }
        
        // Add penalties for undelivered orders
        for (Order order : orders.values()) {
            if (!order.isDelivered()) {
                totalCost += UNDELIVERED_ORDER_PENALTY;
            }
        }
        
        return totalCost;
    }
    
    /**
     * Evaluates a route and returns its cost.
     * The cost includes:
     * - Penalties for late deliveries
     * - Cost for distance traveled
     * 
     * @param route The route to evaluate
     * @param orders Map of all orders in the solution
     * @return The cost of the route, or POSITIVE_INFINITY if the route is invalid
     */
    public static double evaluateRoute(Route route, Map<String, Order> orders) {
        if (route == null || route.getStops().isEmpty()) return 0.0;
        
        Vehicle vehicle = route.getVehicle();
        List<RouteStop> stops = route.getStops();
        double totalCost = 0.0;
        
        // Clone vehicle to simulate the route
        Vehicle simulatedVehicle = vehicle.clone();
        Position currentPosition = simulatedVehicle.getCurrentPosition();
        int currentGlp = simulatedVehicle.getCurrentGlpM3();
        double currentFuel = simulatedVehicle.getCurrentFuelGal();
        
        for (int i = 0; i < stops.size(); i++) {
            RouteStop stop = stops.get(i);
            Position stopPosition = stop.getPosition();
            
            // Calculate distance from current position to stop
            double distance = currentPosition.distanceTo(stopPosition);
            totalCost += distance * DISTANCE_COST_PER_KM;
            
            // Simulate fuel consumption
            double fuelNeeded = simulatedVehicle.calculateFuelNeeded(distance);
            if (fuelNeeded > currentFuel) {
                // Not enough fuel to reach this stop
                return Double.POSITIVE_INFINITY;
            }
            currentFuel -= fuelNeeded;
            
            // Process stop based on its type
            if (stop instanceof OrderStop) {
                OrderStop orderStop = (OrderStop) stop;
                String orderId = orderStop.getEntityID();
                int glpDelivery = orderStop.getGlpDelivery();
                
                // Check if there's enough GLP to deliver
                if (glpDelivery > currentGlp) {
                    // Not enough GLP to fulfill order
                    return Double.POSITIVE_INFINITY;
                }
                currentGlp -= glpDelivery;
                
                // Check if delivery is late
                Order order = orders.get(orderId);
                if (order != null) {
                    LocalDateTime dueDate = order.getDueTime();
                    LocalDateTime actualDelivery = orderStop.getArrivalTime();
                    
                    if (actualDelivery.isAfter(dueDate)) {
                        Duration delay = Duration.between(dueDate, actualDelivery);
                        long hoursLate = delay.toHours() + (delay.toMinutes() % 60 > 0 ? 1 : 0); // Round up to the nearest hour
                        totalCost += hoursLate * LATE_DELIVERY_PENALTY_PER_HOUR;
                    }
                }
            } else if (stop instanceof DepotStop) {
                DepotStop depotStop = (DepotStop) stop;
                int glpRecharge = depotStop.getGlpRecharge();
                
                // Refuel and recharge GLP
                currentFuel = simulatedVehicle.getFuelCapacityGal(); // Full refuel at depot
                currentGlp += glpRecharge;
                
                // Check if GLP capacity is exceeded
                if (currentGlp > simulatedVehicle.getGlpCapacityM3()) {
                    // GLP capacity exceeded
                    return Double.POSITIVE_INFINITY;
                }
            }
            
            // Update current position for next iteration
            currentPosition = stopPosition;
        }
        
        return totalCost;
    }
    
    /**
     * Checks if a solution is valid (all routes are feasible).
     * 
     * @param solution The solution to validate
     * @return true if the solution is valid, false otherwise
     */
    public static boolean isSolutionValid(Solution solution) {
        if (solution == null) return false;
        
        for (Route route : solution.getRoutes()) {
            if (!isRouteValid(route, solution.getOrders())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a route is valid/feasible.
     * 
     * @param route The route to validate
     * @param orders Map of all orders in the solution
     * @return true if the route is valid, false otherwise
     */
    public static boolean isRouteValid(Route route, Map<String, Order> orders) {
        return evaluateRoute(route, orders) != Double.POSITIVE_INFINITY;
    }
    
    /**
     * Calculates the percentage of orders that have been delivered.
     * 
     * @param solution The solution to evaluate
     * @return The percentage of delivered orders (0 to 1)
     */
    public static double calculateOrderFulfillmentRate(Solution solution) {
        if (solution == null || solution.getOrders().isEmpty()) {
            return 0.0;
        }
        
        int totalOrders = solution.getOrders().size();
        int fulfilledOrders = 0;
        
        for (Order order : solution.getOrders().values()) {
            if (order.isDelivered()) {
                fulfilledOrders++;
            }
        }
        
        return (double) fulfilledOrders / totalOrders;
    }
    
    /**
     * Calculates the percentage of GLP demand that has been satisfied.
     * 
     * @param solution The solution to evaluate
     * @return The percentage of GLP demand satisfied (0 to 1)
     */
    public static double calculateGlpSatisfactionRate(Solution solution) {
        if (solution == null || solution.getOrders().isEmpty()) {
            return 0.0;
        }
        
        int totalGlpDemand = 0;
        int satisfiedGlpDemand = 0;
        
        for (Order order : solution.getOrders().values()) {
            totalGlpDemand += order.getGlpRequestM3();
            satisfiedGlpDemand += (order.getGlpRequestM3() - order.getRemainingGlpM3());
        }
        
        return totalGlpDemand > 0 ? (double) satisfiedGlpDemand / totalGlpDemand : 0.0;
    }
    
    /**
     * Gets a detailed breakdown of costs for a solution.
     * 
     * @param solution The solution to analyze
     * @return A list of cost components with descriptions
     */
    public static List<CostComponent> getDetailedCostBreakdown(Solution solution) {
        List<CostComponent> costComponents = new ArrayList<>();
        
        if (solution == null) {
            costComponents.add(new CostComponent("Invalid solution", Double.POSITIVE_INFINITY));
            return costComponents;
        }
        
        Map<String, Order> orders = solution.getOrders();
        
        // If there are no orders, the cost is 0
        if (orders.isEmpty()) {
            costComponents.add(new CostComponent("Empty solution (no orders)", 0.0));
            return costComponents;
        }
        
        // If there are no routes but orders exist, all orders are undelivered
        if (solution.getRoutes().isEmpty()) {
            double undeliveredPenalty = orders.size() * UNDELIVERED_ORDER_PENALTY;
            costComponents.add(new CostComponent("Undelivered orders penalty", undeliveredPenalty));
            return costComponents;
        }
        
        double distanceCost = 0.0;
        double lateDeliveryCost = 0.0;
        double undeliveredCost = 0.0;
        
        // Process each route
        for (Route route : solution.getRoutes()) {
            Vehicle vehicle = route.getVehicle();
            List<RouteStop> stops = route.getStops();
            
            Position currentPosition = vehicle.getCurrentPosition();
            
            for (RouteStop stop : stops) {
                Position stopPosition = stop.getPosition();
                double distance = currentPosition.distanceTo(stopPosition);
                distanceCost += distance * DISTANCE_COST_PER_KM;
                
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    String orderId = orderStop.getEntityID();
                    Order order = orders.get(orderId);
                    
                    if (order != null) {
                        LocalDateTime dueDate = order.getDueTime();
                        LocalDateTime actualDelivery = orderStop.getArrivalTime();
                        
                        if (actualDelivery.isAfter(dueDate)) {
                            Duration delay = Duration.between(dueDate, actualDelivery);
                            long hoursLate = delay.toHours() + (delay.toMinutes() % 60 > 0 ? 1 : 0);
                            lateDeliveryCost += hoursLate * LATE_DELIVERY_PENALTY_PER_HOUR;
                        }
                    }
                }
                
                currentPosition = stopPosition;
            }
        }
        
        // Count undelivered orders
        int undeliveredCount = 0;
        for (Order order : orders.values()) {
            if (!order.isDelivered()) {
                undeliveredCount++;
            }
        }
        undeliveredCost = undeliveredCount * UNDELIVERED_ORDER_PENALTY;
        
        // Add all cost components
        costComponents.add(new CostComponent("Distance cost", distanceCost));
        costComponents.add(new CostComponent("Late delivery penalties", lateDeliveryCost));
        costComponents.add(new CostComponent("Undelivered orders penalties", undeliveredCost));
        costComponents.add(new CostComponent("Total cost", distanceCost + lateDeliveryCost + undeliveredCost));
        
        return costComponents;
    }
    
    /**
     * Class to represent a cost component and its value
     */
    public static class CostComponent {
        private final String description;
        private final double cost;
        
        public CostComponent(String description, double cost) {
            this.description = description;
            this.cost = cost;
        }
        
        public String getDescription() {
            return description;
        }
        
        public double getCost() {
            return cost;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %.2f", description, cost);
        }
    }
}
