package com.vroute.solution;

import java.util.List;
import java.util.Map;

import com.vroute.models.Order;

public class Solution {
    private final Map<String, Order> orders;
    private final List<Route> routes;
    private double cost;

    /**
     * Creates a solution with auto-calculated cost.
     *
     * @param orders The orders to be delivered
     * @param routes The routes for vehicles to deliver the orders
     */
    public Solution(Map<String, Order> orders, List<Route> routes) {
        this.orders = orders;
        this.routes = routes;
        this.cost = Evaluator.evaluateSolution(this);
    }

    /**
     * Creates a solution with a pre-calculated cost.
     *
     * @param orders The orders to be delivered
     * @param routes The routes for vehicles to deliver the orders
     * @param cost   The pre-calculated cost of the solution
     */
    public Solution(Map<String, Order> orders, List<Route> routes, double cost) {
        this.orders = orders;
        this.routes = routes;
        this.cost = cost;
    }

    public Map<String, Order> getOrders() {
        return orders;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public double getCost() {
        return cost;
    }
    
    /**
     * Recalculate the cost of the solution using the evaluator.
     * This can be useful after making changes to the solution.
     * 
     * @return The updated cost
     */
    public double recalculateCost() {
        this.cost = Evaluator.evaluateSolution(this);
        return this.cost;
    }
    
    /**
     * Gets a detailed breakdown of the costs for this solution.
     * 
     * @return A list of cost components
     */
    public List<Evaluator.CostComponent> getDetailedCostBreakdown() {
        return Evaluator.getDetailedCostBreakdown(this);
    }
    
    /**
     * Checks if this solution is valid (all routes are feasible).
     * 
     * @return true if the solution is valid, false otherwise
     */
    public boolean isValid() {
        return Evaluator.isSolutionValid(this);
    }
    
    /**
     * Gets the percentage of orders that have been fulfilled.
     * 
     * @return A value between 0 and 1 representing the fulfillment rate
     */
    public double getOrderFulfillmentRate() {
        return Evaluator.calculateOrderFulfillmentRate(this);
    }
    
    /**
     * Gets the percentage of requested GLP that has been delivered.
     * 
     * @return A value between 0 and 1 representing the GLP satisfaction rate
     */
    public double getGlpSatisfactionRate() {
        return Evaluator.calculateGlpSatisfactionRate(this);
    }
}
