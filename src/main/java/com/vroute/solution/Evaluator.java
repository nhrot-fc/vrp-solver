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
public class Evaluator {
    
    // Constants for penalty calculation
    private static final double LATE_DELIVERY_PENALTY_PER_HOUR = 500.0;
    private static final double UNDELIVERED_ORDER_PENALTY = 10000.0;
    private static final double DISTANCE_COST_PER_KM = 10.0;
    
    // Debug mode flag
    private static boolean debugMode = false;
    
    // Validation mode flags
    private static boolean allowPartialGlpDelivery = true;
    private static boolean enforceGlpCapacityLimit = true;
    private static boolean validateGlpNotExceedsCapacity = true;
    
    /**
     * Enables or disables debug mode
     * @param enable true to enable debug mode, false to disable
     */
    public static void setDebugMode(boolean enable) {
        debugMode = enable;
        debug("Debug mode " + (enable ? "enabled" : "disabled"));
    }
    
    /**
     * Returns the current state of debug mode
     * @return true if debug mode is enabled, false otherwise
     */
    public static boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Controls whether partial GLP delivery is allowed
     * @param allow true to allow partial deliveries, false to require full deliveries
     */
    public static void setAllowPartialGlpDelivery(boolean allow) {
        allowPartialGlpDelivery = allow;
        debug("Partial GLP delivery " + (allow ? "allowed" : "disallowed"));
    }
    
    /**
     * Returns whether partial GLP delivery is allowed
     * @return true if partial delivery is allowed, false otherwise
     */
    public static boolean isAllowPartialGlpDelivery() {
        return allowPartialGlpDelivery;
    }
    
    /**
     * Controls whether GLP capacity limits are enforced at depot stops
     * @param enforce true to enforce capacity limits, false to allow exceeding
     */
    public static void setEnforceGlpCapacityLimit(boolean enforce) {
        enforceGlpCapacityLimit = enforce;
        debug("GLP capacity limits " + (enforce ? "enforced" : "not enforced"));
    }
    
    /**
     * Returns whether GLP capacity limits are enforced at depot stops
     * @return true if capacity limits are enforced, false otherwise
     */
    public static boolean isEnforceGlpCapacityLimit() {
        return enforceGlpCapacityLimit;
    }
    
    /**
     * Controls validation that current GLP doesn't exceed vehicle capacity
     * @param validate true to validate, false to skip validation
     */
    public static void setValidateGlpNotExceedsCapacity(boolean validate) {
        validateGlpNotExceedsCapacity = validate;
        debug("Validation of GLP not exceeding capacity " + (validate ? "enabled" : "disabled"));
    }
    
    /**
     * Returns whether validation that current GLP doesn't exceed vehicle capacity is enabled
     * @return true if validation is enabled, false otherwise
     */
    public static boolean isValidateGlpNotExceedsCapacity() {
        return validateGlpNotExceedsCapacity;
    }
    
    /**
     * Print debug message if debug mode is enabled
     * @param message The message to print
     */
    private static void debug(String message) {
        if (debugMode) {
            System.out.println("[EVALUATOR-DEBUG] " + message);
        }
    }
    
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
        if (solution == null) {
            debug("Solution is null, returning POSITIVE_INFINITY");
            return Double.POSITIVE_INFINITY;
        }
        
        Map<String, Order> orders = solution.getOrders();
        List<Route> routes = solution.getRoutes();
        
        debug("Evaluating solution with " + orders.size() + " orders and " + routes.size() + " routes");
        
        // If there are no orders, the cost is 0
        if (orders.isEmpty()) {
            debug("No orders in solution, cost is 0");
            return 0.0;
        }
        
        // If there are orders but no routes, the cost is infinity
        if (routes.isEmpty()) {
            debug("Solution has orders but no routes, returning POSITIVE_INFINITY");
            return Double.POSITIVE_INFINITY;
        }
        
        double totalCost = 0.0;
        
        // Calculate cost for each route
        for (int i = 0; i < routes.size(); i++) {
            Route route = routes.get(i);
            debug("Evaluating route " + i + " assigned to vehicle " + route.getVehicle().getId());
            double routeCost = evaluateRoute(route, orders);
            
            if (routeCost == Double.POSITIVE_INFINITY) {
                debug("Route " + i + " is invalid, entire solution is invalid");
                return Double.POSITIVE_INFINITY; // If any route is invalid, the entire solution is invalid
            }
            
            debug("Route " + i + " cost: " + routeCost);
            totalCost += routeCost;
        }
        
        // Add penalties for undelivered orders
        int undeliveredCount = 0;
        for (Order order : orders.values()) {
            if (!order.isDelivered()) {
                undeliveredCount++;
                totalCost += UNDELIVERED_ORDER_PENALTY;
            }
        }
        
        debug("Undelivered orders: " + undeliveredCount + " with penalty: " + (undeliveredCount * UNDELIVERED_ORDER_PENALTY));
        debug("Total solution cost: " + totalCost);
        
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
        if (route == null || route.getStops().isEmpty()) {
            debug("Route is null or empty, cost is 0");
            return 0.0;
        }
        
        Vehicle vehicle = route.getVehicle();
        List<RouteStop> stops = route.getStops();
        double totalCost = 0.0;
        
        debug("Evaluating route for vehicle " + vehicle.getId() + " with " + stops.size() + " stops");
        debug("Vehicle initial state: GLP=" + vehicle.getCurrentGlpM3() + "/" + vehicle.getGlpCapacityM3() + 
              "m³, Fuel=" + vehicle.getCurrentFuelGal() + "/" + vehicle.getFuelCapacityGal() + " gal");
        
        // Validate that current GLP does not exceed capacity if validation is enabled
        if (validateGlpNotExceedsCapacity && vehicle.getCurrentGlpM3() > vehicle.getGlpCapacityM3()) {
            debug("INVALID ROUTE: Current GLP " + vehicle.getCurrentGlpM3() + " m³ exceeds capacity " +
                  vehicle.getGlpCapacityM3() + " m³");
            return Double.POSITIVE_INFINITY;
        }
        
        // Clone vehicle to simulate the route
        Vehicle simulatedVehicle = vehicle.clone();
        Position currentPosition = simulatedVehicle.getCurrentPosition();
        int currentGlp = simulatedVehicle.getCurrentGlpM3();
        double currentFuel = simulatedVehicle.getCurrentFuelGal();
        
        debug("Starting at position: " + currentPosition);
        
        for (int i = 0; i < stops.size(); i++) {
            RouteStop stop = stops.get(i);
            Position stopPosition = stop.getPosition();
            
            debug("Stop " + i + " at position: " + stopPosition);
            
            // Calculate distance from current position to stop
            double distance = currentPosition.distanceTo(stopPosition);
            double stopDistanceCost = distance * DISTANCE_COST_PER_KM;
            totalCost += stopDistanceCost;
            
            debug("Distance to stop: " + distance + " km, cost: " + stopDistanceCost);
            
            // Simulate fuel consumption
            double fuelNeeded = simulatedVehicle.calculateFuelNeeded(distance);
            debug("Fuel needed: " + fuelNeeded + " gal (current: " + currentFuel + " gal)");
            
            if (fuelNeeded > currentFuel) {
                // Not enough fuel to reach this stop
                debug("INVALID ROUTE: Not enough fuel to reach stop " + i + 
                      ". Need " + fuelNeeded + " gal but have only " + currentFuel + " gal");
                return Double.POSITIVE_INFINITY;
            }
            currentFuel -= fuelNeeded;
            debug("Remaining fuel after travel: " + currentFuel + " gal");
            
            // Process stop based on its type
            if (stop instanceof OrderStop) {
                OrderStop orderStop = (OrderStop) stop;
                String orderId = orderStop.getEntityID();
                int glpDelivery = orderStop.getGlpDelivery();
                
                debug("Order stop for order " + orderId + ", delivering " + glpDelivery + " m³ GLP");
                debug("Current GLP in vehicle: " + currentGlp + " m³");
                
                // Check if there's enough GLP to deliver
                if (glpDelivery > currentGlp) {
                    if (!allowPartialGlpDelivery) {
                        // If partial delivery is not allowed, this route is invalid
                        debug("INVALID ROUTE: Not enough GLP to fulfill order " + orderId + 
                              ". Need " + glpDelivery + " m³ but have only " + currentGlp + " m³");
                        return Double.POSITIVE_INFINITY;
                    }
                    
                    // Otherwise, deliver what we can
                    debug("Not enough GLP to fully fulfill order " + orderId + 
                          ". Need " + glpDelivery + " m³ but have only " + currentGlp + " m³");
                    glpDelivery = currentGlp;
                    debug("Will deliver partial amount: " + glpDelivery + " m³");
                }
                currentGlp -= glpDelivery;
                
                debug("GLP remaining after delivery: " + currentGlp + " m³");
                
                // Check if delivery is late
                Order order = orders.get(orderId);
                if (order != null) {
                    LocalDateTime dueDate = order.getDueTime();
                    LocalDateTime actualDelivery = orderStop.getArrivalTime();
                    
                    debug("Order due time: " + dueDate + ", actual delivery time: " + actualDelivery);
                    
                    if (actualDelivery.isAfter(dueDate)) {
                        Duration delay = Duration.between(dueDate, actualDelivery);
                        long hoursLate = delay.toHours() + (delay.toMinutes() % 60 > 0 ? 1 : 0); // Round up to the nearest hour
                        double latePenalty = hoursLate * LATE_DELIVERY_PENALTY_PER_HOUR;
                        totalCost += latePenalty;
                        
                        debug("Late delivery: " + hoursLate + " hours, penalty: " + latePenalty);
                    } else {
                        debug("Delivery on time");
                    }
                } else {
                    debug("WARNING: Order " + orderId + " not found in orders map");
                }
            } else if (stop instanceof DepotStop) {
                DepotStop depotStop = (DepotStop) stop;
                int glpRecharge = depotStop.getGlpRecharge();
                
                debug("Depot stop at " + depotStop.getEntityID() + ", recharging " + glpRecharge + " m³ GLP");
                
                // Refuel and recharge GLP
                double prevFuel = currentFuel;
                currentFuel = simulatedVehicle.getFuelCapacityGal(); // Full refuel at depot
                debug("Refueling at depot from " + prevFuel + " to " + currentFuel + " gal");
                
                int prevGlp = currentGlp;
                
                if (enforceGlpCapacityLimit) {
                    // Respect GLP capacity limits during recharging
                    int newGlp = currentGlp + glpRecharge;
                    if (newGlp > simulatedVehicle.getGlpCapacityM3()) {
                        debug("Limiting GLP recharge due to capacity constraints. Capacity: " + 
                              simulatedVehicle.getGlpCapacityM3() + " m³");
                        currentGlp = simulatedVehicle.getGlpCapacityM3();
                    } else {
                        currentGlp = newGlp;
                    }
                } else {
                    // Check if GLP capacity is exceeded
                    currentGlp += glpRecharge;
                    if (currentGlp > simulatedVehicle.getGlpCapacityM3()) {
                        debug("INVALID ROUTE: GLP capacity exceeded at depot stop. " + 
                              currentGlp + " m³ exceeds capacity of " + simulatedVehicle.getGlpCapacityM3() + " m³");
                        return Double.POSITIVE_INFINITY;
                    }
                }
                
                debug("GLP before recharge: " + prevGlp + " m³, after recharge: " + currentGlp + " m³");
            }
            
            // Update current position for next iteration
            currentPosition = stopPosition;
        }
        
        debug("Route evaluation complete. Total cost: " + totalCost);
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
            int currentGlp = vehicle.getCurrentGlpM3();
            
            for (RouteStop stop : stops) {
                Position stopPosition = stop.getPosition();
                double distance = currentPosition.distanceTo(stopPosition);
                distanceCost += distance * DISTANCE_COST_PER_KM;
                
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    String orderId = orderStop.getEntityID();
                    int glpDelivery = orderStop.getGlpDelivery();
                    
                    // Adjust for partial delivery if needed
                    if (glpDelivery > currentGlp) {
                        glpDelivery = currentGlp;
                    }
                    currentGlp -= glpDelivery;
                    
                    Order order = orders.get(orderId);
                    if (order != null) {
                        LocalDateTime dueDate = order.getDueTime();
                        LocalDateTime actualDelivery = orderStop.getArrivalTime();
                        
                        if (actualDelivery.isAfter(dueDate)) {
                            Duration delay = Duration.between(dueDate, actualDelivery);
                            long hoursLate = delay.toHours() + (delay.toMinutes() % 60 > 0 ? 1 : 0);
                            double thisLateCost = hoursLate * LATE_DELIVERY_PENALTY_PER_HOUR;
                            lateDeliveryCost += thisLateCost;
                            debug("Cost breakdown - late delivery for order " + orderId + 
                                  ": " + hoursLate + " hours, penalty: " + thisLateCost);
                        }
                    }
                } else if (stop instanceof DepotStop) {
                    DepotStop depotStop = (DepotStop) stop;
                    int glpRecharge = depotStop.getGlpRecharge();
                    
                    // Respect GLP capacity limits during recharging
                    int newGlp = currentGlp + glpRecharge;
                    if (newGlp > vehicle.getGlpCapacityM3()) {
                        currentGlp = vehicle.getGlpCapacityM3();
                    } else {
                        currentGlp = newGlp;
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
        
        debug("Cost breakdown - Distance cost: " + distanceCost);
        debug("Cost breakdown - Late delivery penalties: " + lateDeliveryCost);
        debug("Cost breakdown - Undelivered orders penalties: " + undeliveredCost);
        
        // Add all cost components
        costComponents.add(new CostComponent("Distance cost", distanceCost));
        if (lateDeliveryCost > 0) {
            costComponents.add(new CostComponent("Late delivery penalties", lateDeliveryCost));
        }
        if (undeliveredCost > 0) {
            costComponents.add(new CostComponent("Undelivered orders penalties", undeliveredCost));
        }
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
