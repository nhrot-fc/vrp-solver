package com.vroute.taboo;

import java.util.List;

import com.vroute.models.Environment;
import com.vroute.solution.Evaluator;
import com.vroute.solution.SIHSolver;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;

/**
 * Implementation of the Tabu Search algorithm for the V-Route problem.
 */
public class TabuSearch implements Solver {
    // Algorithm parameters
    private final int MAX_ITERATIONS = 1000;
    private final int TABU_TENURE = 10;
    private final int MAX_NEIGHBORHOOD_SIZE = 50;
    private final double ASPIRATION_FACTOR = 0.95;
    private final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;

    // Operational objects
    private final TabuList tabuList;
    private final MoveGenerator moveGenerator;

    // Search state
    private Solution bestSolution;
    private double bestCost;
    private int iterationsWithoutImprovement;

    public TabuSearch() {
        this.tabuList = new TabuList(TABU_TENURE);
        this.moveGenerator = new MoveGenerator();
        this.bestCost = Double.POSITIVE_INFINITY;
        this.iterationsWithoutImprovement = 0;
    }

    @Override
    public Solution solve(Environment environment) {
        // Create initial solution using SIH
        System.out.println("Creating initial solution using SIH...");
        Solution initialSolution = new SIHSolver().solve(environment);
        
        System.out.println("Initial solution created with cost: " + initialSolution.getCost());
        
        // Run the Tabu Search with the initial solution
        System.out.println("Starting Tabu Search optimization...");
        Solution improvedSolution = solve(initialSolution, environment);
        
        System.out.println("Tabu Search completed.");
        System.out.println("Initial solution cost: " + initialSolution.getCost());
        System.out.println("Final solution cost: " + improvedSolution.getCost());
        System.out.println("Improvement: " + 
                         (100.0 * (initialSolution.getCost() - improvedSolution.getCost()) / initialSolution.getCost()) + "%");
        
        return improvedSolution;
    }

    public Solution solve(Solution initialSolution, Environment environment) {
        // Set environment for path calculations in move operations
        moveGenerator.setEnvironment(environment);
        
        // Initialize the search
        Solution currentSolution = initialSolution;
        this.bestSolution = initialSolution;
        this.bestCost = Evaluator.evaluateSolution(initialSolution);

        System.out.println("Starting Tabu Search with initial cost: " + bestCost);

        // Main tabu search loop
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Generate the neighborhood
            List<TabuMove> neighborhood = moveGenerator.generateAllMoves(currentSolution, MAX_NEIGHBORHOOD_SIZE);

            // Find the best non-tabu move or one that satisfies aspiration criterion
            TabuMove bestMove = null;
            double bestMoveCost = Double.POSITIVE_INFINITY;

            for (TabuMove move : neighborhood) {
                // Apply the move to get the candidate solution
                Solution candidateSolution = move.apply(currentSolution);
                double candidateCost = Evaluator.evaluateSolution(candidateSolution);

                boolean isTabu = tabuList.isTabu(move, iteration);

                // Accept the move if:
                // 1. It's not tabu, or
                // 2. It satisfies the aspiration criterion (improves the best solution)
                if ((!isTabu || candidateCost < bestCost * ASPIRATION_FACTOR) &&
                        candidateCost < bestMoveCost) {
                    bestMove = move;
                    bestMoveCost = candidateCost;
                }
            }

            // If no valid move found, diversify
            if (bestMove == null) {
                System.out.println("Iteration " + iteration + ": No valid move found, diversifying");
                currentSolution = diversify(currentSolution);
                tabuList.clear();
                continue;
            }

            // Apply the best move
            currentSolution = bestMove.apply(currentSolution);
            double currentCost = bestMoveCost;

            // Add the move to the tabu list
            tabuList.addMove(bestMove, iteration);

            // Update best solution if improved
            if (currentCost < bestCost) {
                bestSolution = currentSolution;
                bestCost = currentCost;
                iterationsWithoutImprovement = 0;
                System.out.println("Iteration " + iteration + ": New best solution found with cost " + bestCost);
            } else {
                iterationsWithoutImprovement++;
                if (iteration % 100 == 0) {
                    System.out.println("Iteration " + iteration + ": Current cost " + currentCost +
                            ", Best cost " + bestCost);
                }
            }

            // Update the tabu list
            tabuList.update(iteration);

            // Check termination criteria
            if (iterationsWithoutImprovement >= MAX_ITERATIONS_WITHOUT_IMPROVEMENT) {
                System.out.println("Stopping: " + MAX_ITERATIONS_WITHOUT_IMPROVEMENT +
                        " iterations without improvement");
                break;
            }
        }

        System.out.println("Tabu Search completed. Best solution cost: " + bestCost);
        return bestSolution;
    }

    private Solution diversify(Solution solution) {
        // Generate a number of random swap moves
        List<SwapMove> swapMoves = moveGenerator.generateSwapMoves(solution, 10);

        // Apply the moves in sequence
        Solution diversifiedSolution = solution;
        for (SwapMove move : swapMoves) {
            diversifiedSolution = move.apply(diversifiedSolution);
        }

        return diversifiedSolution;
    }
}