package com.vroute.taboo;

import java.util.ArrayList;
import java.util.List;

import com.vroute.models.Environment;
import com.vroute.solution.Evaluator;
import com.vroute.solution.SIHSolver;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;

/**
 * Implementation of the tabu search algorithm for vehicle routing problems
 */
public class TabuSearch implements Solver {
    private static final int MAX_ITERATIONS = 1000;
    private static final int TABU_TENURE = 30;
    
    private final MoveGenerator moveGenerator;
    private final TabuList tabuList;
    private final SIHSolver initialSolver;
    
    public TabuSearch() {
        this.moveGenerator = new MoveGenerator();
        this.tabuList = new TabuList(TABU_TENURE);
        this.initialSolver = new SIHSolver();
    }
    
    @Override
    public Solution solve(Environment environment) {
        // Use SIHSolver to generate an initial feasible solution
        Solution initialSolution = initialSolver.solve(environment);
        
        return tabuSearch(environment, initialSolution);
    }
    
    /**
     * Perform tabu search to improve an initial solution
     * @param environment Environment containing problem data
     * @param initialSolution Initial feasible solution
     * @return Improved solution
     */
    private Solution tabuSearch(Environment environment, Solution initialSolution) {
        // Make a deep copy of the initial solution
        Solution currentSolution = cloneSolution(initialSolution);
        Solution bestSolution = cloneSolution(initialSolution);
        
        // Evaluate initial solutions
        double currentCost = Evaluator.evaluateSolution(environment, currentSolution);
        double bestCost = currentCost;
        
        // Main tabu search loop
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Generate candidate moves
            List<TabuMove> candidateMoves = generateCandidateMoves(currentSolution);
            
            // Find best non-tabu move or aspiration move
            TabuMove bestMove = null;
            double bestMoveCost = Double.MAX_VALUE;
            
            for (TabuMove move : candidateMoves) {
                // Apply the move to a temporary solution
                Solution tempSolution = cloneSolution(currentSolution);
                if (move.apply(tempSolution)) {
                    double tempCost = Evaluator.evaluateSolution(environment, tempSolution);
                    
                    // Accept if:
                    // 1. Move is not tabu, and it's the best non-tabu move so far
                    // 2. Move is tabu, but satisfies aspiration (better than best known solution)
                    if ((!tabuList.isTabu(move) && tempCost < bestMoveCost) || 
                            (tempCost < bestCost)) {
                        bestMove = move;
                        bestMoveCost = tempCost;
                    }
                }
            }
            
            // If no improving move was found
            if (bestMove == null) {
                // Could implement diversification strategies here
                continue;
            }
            
            // Apply the best move
            bestMove.apply(currentSolution);
            currentCost = bestMoveCost;
            
            // Update tabu list
            tabuList.addMove(bestMove, iteration);
            tabuList.updateTabuList(iteration);
            
            // Update best solution if improved
            if (currentCost < bestCost) {
                bestSolution = cloneSolution(currentSolution);
                bestCost = currentCost;
            }
        }
        
        return bestSolution;
    }
    
    /**
     * Generate a list of candidate moves for the current solution
     * @param solution Current solution
     * @return List of candidate moves
     */
    private List<TabuMove> generateCandidateMoves(Solution solution) {
        List<TabuMove> moves = new ArrayList<>();
        
        // Generate a fixed number of candidate moves
        int numCandidates = 20;
        for (int i = 0; i < numCandidates; i++) {
            TabuMove move = moveGenerator.generateRandomMove(solution);
            if (move != null) {
                moves.add(move);
            }
        }
        
        return moves;
    }
    
    /**
     * Create a deep copy of a solution
     * @param solution Solution to clone
     * @return New Solution instance with the same data
     */
    private Solution cloneSolution(Solution solution) {
        // This is a simplified deep copy - in a real implementation you would need
        // to properly clone all nested objects (routes, stops, etc.)
        return new Solution(solution.getOrders(), new ArrayList<>(solution.getRoutes()));
    }
} 