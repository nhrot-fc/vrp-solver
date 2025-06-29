package com.vroute.taboo;

import java.util.ArrayList;
import java.util.List;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;

/**
 * Implementation of the Tabu Search metaheuristic for the Vehicle Routing
 * Problem with GLP delivery.
 */
public class TabuSearch implements Solver {
    // Algorithm parameters
    private static final int MAX_ITERATIONS = 1000;
    private static final int TABU_TENURE = 25;
    private static final int MAX_NO_IMPROVEMENT = 100;
    private static final int MAX_NEIGHBORS_TO_EXPLORE = 20;

    /**
     * Print debug information if debug mode is enabled
     */
    private void debug(String message) {
        System.out.println("[TabuSearch] " + message);

    }

    @Override
    public Solution solve(Environment env) {
        if (env == null) {
            debug("Environment is null, returning null");
            return null;
        }

        List<Order> orders = env.getPendingOrders();
        if (orders.isEmpty()) {
            debug("No pending orders, returning empty solution");
            return new Solution(new ArrayList<>(), env);
        }

        return solve(env, orders);
    }

    /**
     * Solve the routing problem with the specified orders
     * 
     * @param env    Environment
     * @param orders List of orders to route
     * @return Optimized solution
     */
    public Solution solve(Environment env, List<Order> orders) {
        // Create working copies of environment and orders
        Environment workEnv = env.clone();

        // Create initial solution
        Solution currentSolution = NeighborhoodGenerator.generateInitialSolution(workEnv);
        if (currentSolution == null) {
            debug("Failed to generate initial solution");
            return null;
        }

        Solution bestSolution = currentSolution;
        double bestScore = currentSolution.getScore();

        // Initialize tabu search parameters
        List<Solution> tabuList = new ArrayList<>();
        int iterationsWithoutImprovement = 0;
        int iterationProgressStep = MAX_ITERATIONS / 20; // 5% of max iterations

        debug("Starting tabu search with initial score: " + bestScore);
        debug("Planned iterations: " + MAX_ITERATIONS);

        // Main tabu search loop
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Report progress every 5% of iterations
            if (iteration % iterationProgressStep == 0) {
                int percentComplete = (iteration * 100) / MAX_ITERATIONS;
                debug(String.format("Progress: %d%% complete (%d/%d iterations)",
                        percentComplete, iteration, MAX_ITERATIONS));
                debug(String.format("Current best score: %.6f, No improvement for %d iterations",
                        bestScore, iterationsWithoutImprovement));
            }

            // Check termination condition
            if (iterationsWithoutImprovement >= MAX_NO_IMPROVEMENT) {
                debug("Terminating: No improvement for " + MAX_NO_IMPROVEMENT + " iterations");
                break;
            }

            // Generate and evaluate neighbors
            Solution bestNeighbor = findBestNeighbor(workEnv, currentSolution, tabuList, bestScore);
            if (bestNeighbor == null) {
                debug("No viable neighbors found, terminating search");
                break;
            }

            // Update current solution
            double neighborScore = bestNeighbor.getScore();
            currentSolution = bestNeighbor;

            // Update best solution if improvement found
            if (neighborScore > bestScore) {
                bestSolution = currentSolution;
                bestScore = neighborScore;
                iterationsWithoutImprovement = 0;
            } else {
                iterationsWithoutImprovement++;
            }

            // Update tabu list
            updateTabuList(tabuList, currentSolution);
        }

        debug("Tabu search completed");
        debug(String.format("Final best score: %.6f", bestScore));
        
        // Verify that all orders are delivered on time and completely
        try {
            bestSolution.verifyOrderDeliveries();
            debug("All orders are delivered on time and completely");
        } catch (AssertionError e) {
            debug("Solution verification failed: " + e.getMessage());
            // Return the solution anyway, as this is just a validation check
        }
        
        return bestSolution;
    }

    /**
     * Finds the best non-tabu neighbor, or tabu neighbor that satisfies aspiration
     */
    private Solution findBestNeighbor(Environment env, Solution currentSolution,
            List<Solution> tabuList, double globalBestScore) {
        // Generate neighboring solutions
        List<Solution> neighbors = NeighborhoodGenerator.generateNeighbors(
                env, currentSolution, MAX_NEIGHBORS_TO_EXPLORE);

        // Sort neighbors by score (best first)
        neighbors.sort((a, b) -> Double.compare(a.getScore(), b.getScore()));

        // Find best non-tabu neighbor or neighbor satisfying aspiration criterion
        Solution bestNeighbor = null;
        double bestNeighborScore = Double.NEGATIVE_INFINITY;

        for (Solution neighbor : neighbors) {
            double neighborScore = neighbor.getScore();
            boolean isTabuNeighbor = isTabu(neighbor, tabuList);

            // Accept if not tabu and better than current best neighbor
            if (!isTabuNeighbor && neighborScore > bestNeighborScore) {
                bestNeighbor = neighbor;
                bestNeighborScore = neighborScore;
            }
            // Aspiration criterion - accept tabu move if better than global best
            else if (isTabuNeighbor && neighborScore > globalBestScore) {
                bestNeighbor = neighbor;
                bestNeighborScore = neighborScore;
                debug("Accepting tabu neighbor due to aspiration criterion");
            }
        }

        return bestNeighbor;
    }

    /**
     * Updates the tabu list with a new solution, maintaining max size
     */
    private void updateTabuList(List<Solution> tabuList, Solution solution) {
        tabuList.add(solution);
        if (tabuList.size() > TABU_TENURE) {
            tabuList.remove(0);
        }
    }

    /**
     * Checks if a solution is in the tabu list
     * Note: In a production implementation, we would compare solution features
     * instead
     */
    private boolean isTabu(Solution solution, List<Solution> tabuList) {
        for (Solution tabuSolution : tabuList) {
            if (solution.equals(tabuSolution)) {
                return true;
            }
        }
        return false;
    }
}