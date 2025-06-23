package com.vroute.alns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.vroute.alns.operators.DestroyOperator;
import com.vroute.alns.operators.GreedyInsertionOperator;
import com.vroute.alns.operators.RandomRemovalOperator;
import com.vroute.alns.operators.RegretInsertionOperator;
import com.vroute.alns.operators.RepairOperator;
import com.vroute.alns.operators.WorstRemovalOperator;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.solution.Evaluator;
import com.vroute.solution.SIHSolver;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;

/**
 * Implementation of the Adaptive Large Neighborhood Search (ALNS) algorithm.
 * This algorithm iteratively destroys and repairs a solution to find better ones.
 */
public class AlnsAlgorithm implements Solver {
    // Simulated Annealing parameters
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.9975;
    private static final double FINAL_TEMPERATURE = 0.01;
    
    // ALNS parameters
    private static final int MAX_ITERATIONS = 1000;
    private static final double SIGMA_1 = 33.0; // Score for finding a new global best
    private static final double SIGMA_2 = 9.0;  // Score for finding a new local best
    private static final double SIGMA_3 = 3.0;  // Score for finding a better solution
    private static final double RHO = 0.1;      // Weight adjustment factor
    
    // Operator weights - updated adaptively
    private final List<DestroyOperator> destroyOperators;
    private final List<RepairOperator> repairOperators;
    private final List<Double> destroyWeights;
    private final List<Double> repairWeights;
    
    private final Random random;
    private final SimulatedAnnealing simulatedAnnealing;
    
    /**
     * Creates a new ALNS algorithm instance.
     */
    public AlnsAlgorithm() {
        this.random = new Random();
        this.simulatedAnnealing = new SimulatedAnnealing(
            INITIAL_TEMPERATURE, COOLING_RATE, FINAL_TEMPERATURE);
        
        // Initialize operators
        this.destroyOperators = new ArrayList<>();
        this.repairOperators = new ArrayList<>();
        this.destroyWeights = new ArrayList<>();
        this.repairWeights = new ArrayList<>();
        
        initializeOperators();
    }
    
    /**
     * Initialize the destroy and repair operators.
     */
    private void initializeOperators() {
        // Create destroy operators
        destroyOperators.add(new RandomRemovalOperator());
        destroyOperators.add(new WorstRemovalOperator(0.8));
        
        // Create repair operators
        repairOperators.add(new GreedyInsertionOperator(0.6, 0.3, 0.1));
        repairOperators.add(new RegretInsertionOperator(2, 0.7, 0.3));
        
        // Initialize weights
        for (int i = 0; i < destroyOperators.size(); i++) {
            destroyWeights.add(1.0);
        }
        
        for (int i = 0; i < repairOperators.size(); i++) {
            repairWeights.add(1.0);
        }
    }
    
    /**
     * Solves the vehicle routing problem using the ALNS algorithm.
     * 
     * @param env The environment containing the problem data
     * @return A solution
     */
    @Override
    public Solution solve(Environment env) {
        // Initialize the solution with the SIH algorithm
        // Convert list of orders to a map
        Map<String, Order> orderMap = new HashMap<>();
        for (Order order : env.getPendingOrders()) {
            orderMap.put(order.getId(), order);
        }
        
        Solution currentSolution = new SIHSolver().solve(env);
        
        if (currentSolution == null) {
            System.out.println("Failed to create initial solution");
            return null;
        }
        
        // If there are no orders to deliver, return the empty solution
        if (currentSolution.getOrders().isEmpty()) {
            return currentSolution;
        }
        
        Solution bestSolution = new Solution(
            currentSolution.getOrders(), 
            currentSolution.getRoutes()
        );
        
        // Initialize scoring parameters for adaptive weight adjustment
        double[] destroyScores = new double[destroyOperators.size()];
        double[] repairScores = new double[repairOperators.size()];
        int[] destroyUsage = new int[destroyOperators.size()];
        int[] repairUsage = new int[repairOperators.size()];
        
        // Main ALNS loop
        int iterations = 0;
        int iterationsWithoutImprovement = 0;
        int segmentStart = 0;
        int segmentSize = 100; // Update weights every 100 iterations
        
        while (iterations < MAX_ITERATIONS && iterationsWithoutImprovement < MAX_ITERATIONS / 2) {
            // Select destroy and repair operators based on weights
            int destroyIdx = selectOperator(destroyWeights);
            int repairIdx = selectOperator(repairWeights);
            
            DestroyOperator destroyOperator = destroyOperators.get(destroyIdx);
            RepairOperator repairOperator = repairOperators.get(repairIdx);
            
            // Determine the number of orders to remove
            int removalCount = calculateRemovalCount(currentSolution, iterations);
            
            // Apply the operators to create a new solution
            Solution destroyedSolution = destroyOperator.destroy(currentSolution, env, removalCount);
            Solution newSolution = repairOperator.repair(destroyedSolution, env);
            
            // Evaluate the new solution
            double currentCost = Evaluator.evaluateSolution(env, currentSolution);
            double newCost = Evaluator.evaluateSolution(env, newSolution);
            
            double score = 0.0;
            
            // Accept or reject the new solution using Simulated Annealing
            if (simulatedAnnealing.accept(currentCost, newCost)) {
                currentSolution = newSolution;
                
                // Update scores based on solution quality
                if (newCost < Evaluator.evaluateSolution(env, bestSolution)) {
                    bestSolution = new Solution(
                        newSolution.getOrders(), 
                        newSolution.getRoutes()
                    );
                    score = SIGMA_1; // New global best
                    iterationsWithoutImprovement = 0;
                    System.out.println("New best solution found at iteration " + iterations + 
                                      " with cost " + newCost);
                } else if (newCost < currentCost) {
                    score = SIGMA_2; // Better than current
                    iterationsWithoutImprovement = 0;
                } else {
                    score = SIGMA_3; // Accepted but not better
                    iterationsWithoutImprovement++;
                }
            } else {
                iterationsWithoutImprovement++;
            }
            
            // Update scores for the selected operators
            destroyScores[destroyIdx] += score;
            repairScores[repairIdx] += score;
            destroyUsage[destroyIdx]++;
            repairUsage[repairIdx]++;
            
            // Update weights periodically
            if (iterations - segmentStart >= segmentSize) {
                updateWeights(destroyScores, destroyUsage, destroyWeights);
                updateWeights(repairScores, repairUsage, repairWeights);
                
                // Reset scores and usage counts
                for (int i = 0; i < destroyScores.length; i++) {
                    destroyScores[i] = 0.0;
                    destroyUsage[i] = 0;
                }
                
                for (int i = 0; i < repairScores.length; i++) {
                    repairScores[i] = 0.0;
                    repairUsage[i] = 0;
                }
                
                segmentStart = iterations;
            }
            
            // Cool down the temperature
            simulatedAnnealing.cool();
            iterations++;
            
            // Debug output
            if (iterations % 100 == 0) {
                System.out.println("Iteration " + iterations + ", temperature " + 
                                  simulatedAnnealing.getTemperature() + 
                                  ", best cost " + Evaluator.evaluateSolution(env, bestSolution));
            }
        }
        
        System.out.println("ALNS completed after " + iterations + " iterations");
        System.out.println("Final solution cost: " + Evaluator.evaluateSolution(env, bestSolution));
        
        return bestSolution;
    }
    
    /**
     * Select an operator using a roulette wheel selection based on weights.
     * 
     * @param weights The weights of the operators
     * @return The index of the selected operator
     */
    private int selectOperator(List<Double> weights) {
        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        
        double cumulativeWeight = 0.0;
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue < cumulativeWeight) {
                return i;
            }
        }
        
        // Fallback (should not happen)
        return 0;
    }
    
    /**
     * Calculate the number of orders to remove in the current iteration.
     * 
     * @param solution The current solution
     * @param iteration The current iteration
     * @return The number of orders to remove
     */
    private int calculateRemovalCount(Solution solution, int iteration) {
        // Calculate total number of orders in the solution
        int totalOrders = countOrdersInSolution(solution);
        
        // Start with removing 10-20% of orders, gradually increasing
        double removalPercentage = 0.1 + (0.3 * iteration / MAX_ITERATIONS);
        
        // Ensure we remove at least one order and at most 50% of orders
        int removalCount = Math.max(1, (int)(totalOrders * removalPercentage));
        return Math.min(removalCount, totalOrders / 2);
    }
    
    /**
     * Count the number of orders in the solution.
     * 
     * @param solution The solution
     * @return The number of orders
     */
    private int countOrdersInSolution(Solution solution) {
        return solution.getOrders().size();
    }
    
    /**
     * Update the weights of operators based on their performance.
     * 
     * @param scores The scores of the operators
     * @param usage The usage counts of the operators
     * @param weights The weights to update
     */
    private void updateWeights(double[] scores, int[] usage, List<Double> weights) {
        for (int i = 0; i < weights.size(); i++) {
            if (usage[i] > 0) {
                double newWeight = weights.get(i) * (1 - RHO) + RHO * (scores[i] / usage[i]);
                weights.set(i, newWeight);
            }
        }
    }
}
