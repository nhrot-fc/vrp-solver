package com.vroute.solution;

import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Vehicle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Factory class to create solutions using different solvers.
 */
public class SolutionFactory {
    
    /**
     * Creates a solution using the Sequential Insertion Heuristic (SIH) algorithm.
     * 
     * @param environment The environment containing problem data
     * @param vehicles List of available vehicles
     * @param depots List of available depots
     * @param orders Map of orders to be delivered
     * @param currentDateTime Current date and time
     * @return A solution with routes
     */
    public static Solution createSIHSolution(
            Environment environment, 
            List<Vehicle> vehicles, 
            List<Depot> depots, 
            Map<String, Order> orders, 
            LocalDateTime currentDateTime) {
        
        SIHSolver solver = new SIHSolver(environment, vehicles, depots, currentDateTime);
        return solver.solve(orders);
    }
    
    /**
     * Validates and scores a solution.
     * 
     * @param solution The solution to evaluate
     * @return True if the solution is valid, false otherwise
     */
    public static boolean validateSolution(Solution solution) {
        boolean isValid = SolutionEvaluator.isSolutionValid(solution);
        
        if (isValid) {
            double cost = SolutionEvaluator.evaluateSolution(solution);
            double fulfillmentRate = SolutionEvaluator.calculateOrderFulfillmentRate(solution);
            double glpSatisfactionRate = SolutionEvaluator.calculateGlpSatisfactionRate(solution);
            
            System.out.println("Solution is valid!");
            System.out.println("Cost: " + cost);
            System.out.println("Order fulfillment rate: " + (fulfillmentRate * 100) + "%");
            System.out.println("GLP satisfaction rate: " + (glpSatisfactionRate * 100) + "%");
            
            List<SolutionEvaluator.CostComponent> breakdown = SolutionEvaluator.getDetailedCostBreakdown(solution);
            System.out.println("Cost breakdown:");
            for (SolutionEvaluator.CostComponent component : breakdown) {
                System.out.println("  " + component);
            }
        } else {
            System.out.println("Solution is invalid!");
        }
        
        return isValid;
    }
}
