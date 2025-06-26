package com.vroute.taboo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.vroute.models.Environment;
import com.vroute.solution.Evaluator;
import com.vroute.solution.SIHSolver;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;

/**
 * Implementation of the tabu search algorithm for vehicle routing problems
 * combined with simulated annealing for better exploration
 */
public class TabuSearch implements Solver {
    // Tabu search parameters
    private static final int MAX_ITERATIONS = 3000;
    private static final int TABU_TENURE = 100;
    private static final int NUM_CANDIDATES = 100;

    // Simulated annealing parameters
    private static final double INITIAL_TEMPERATURE = 100.0;
    private static final double COOLING_RATE = 0.995;
    private static final double FINAL_TEMPERATURE = 0.1;

    // Other parameters
    private static final double INTENSIFICATION_THRESHOLD = 0.1; // Probability of using best moves only
    private static final double DIVERSIFICATION_FACTOR = 1.2; // Used when no improvement for a while

    // Move operators
    private final RearrangeMove rearrangeMove;
    private final SwapVehicleMove swapVehicleMove;
    private final SwapStopMove swapStopMove;

    private final Random random;

    public TabuSearch() {
        this.rearrangeMove = new RearrangeMove();
        this.swapVehicleMove = new SwapVehicleMove();
        this.swapStopMove = new SwapStopMove();
        this.random = new Random();
    }

    @Override
    public Solution solve(Environment environment) {
        // Initialize with a null solution (in reality this would come from elsewhere)
        Solution currentSolution = new SIHSolver().solve(environment);

        // If no initial solution provided, we can't proceed
        if (currentSolution == null) {
            return null;
        }

        // Evaluate the initial solution
        double currentScore = Evaluator.evaluateSolution(environment, currentSolution);

        // Best solution tracking
        Solution bestSolution = currentSolution.clone();
        double bestScore = currentScore;

        // Tabu list as a simple set of solution hashes
        Set<Integer> tabuList = new HashSet<>();

        // Simulated annealing temperature
        double temperature = INITIAL_TEMPERATURE;

        // Track iterations without improvement for diversification
        int iterationsWithoutImprovement = 0;

        System.out.println("Starting Tabu Search with SA...");
        System.out.println("Initial solution score: " + currentScore);

        // Main loop
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            // Generate candidate solutions
            List<CandidateSolution> candidates = generateCandidates(environment, currentSolution);

            // If no valid candidates were found, terminate early
            if (candidates.isEmpty()) {
                System.out.println("No valid candidates found. Terminating search.");
                break;
            }

            // Sort candidates by score (best first)
            candidates.sort(Comparator.comparingDouble(CandidateSolution::getScore));

            // Apply intensification or diversification strategy
            boolean shouldIntensify = random.nextDouble() < INTENSIFICATION_THRESHOLD;
            boolean shouldDiversify = iterationsWithoutImprovement > MAX_ITERATIONS / 10;

            CandidateSolution selectedCandidate = null;

            if (shouldIntensify) {
                // Intensification: pick the best non-tabu candidate
                for (CandidateSolution candidate : candidates) {
                    if (!tabuList.contains(candidate.getSolution().hashCode())) {
                        selectedCandidate = candidate;
                        break;
                    }
                }
            } else if (shouldDiversify) {
                // Diversification: pick a random candidate with preference for worse solutions
                Collections.reverse(candidates); // Worst first
                double diversificationTemp = temperature * DIVERSIFICATION_FACTOR;
                for (CandidateSolution candidate : candidates) {
                    if (!tabuList.contains(candidate.getSolution().hashCode())) {
                        // Higher probability of selecting worse solutions during diversification
                        if (random.nextDouble() < Math
                                .exp((currentScore - candidate.getScore()) / diversificationTemp)) {
                            selectedCandidate = candidate;
                            break;
                        }
                    }
                }
            }

            // Default selection with simulated annealing if no candidate selected yet
            if (selectedCandidate == null) {
                for (CandidateSolution candidate : candidates) {
                    if (tabuList.contains(candidate.getSolution().hashCode())) {
                        continue; // Skip tabu solutions
                    }

                    double candidateScore = candidate.getScore();
                    double scoreDelta = currentScore - candidateScore;

                    // Accept if better, or with probability based on simulated annealing
                    if (candidateScore < currentScore ||
                            random.nextDouble() < Math.exp(scoreDelta / temperature)) {
                        selectedCandidate = candidate;
                        break;
                    }
                }
            }

            // If still no candidate found, pick the best non-tabu one
            if (selectedCandidate == null) {
                for (CandidateSolution candidate : candidates) {
                    if (!tabuList.contains(candidate.getSolution().hashCode())) {
                        selectedCandidate = candidate;
                        break;
                    }
                }
            }

            // If still no candidate, pick the best one and override tabu
            if (selectedCandidate == null && !candidates.isEmpty()) {
                selectedCandidate = candidates.get(0);
                System.out.println("Overriding tabu due to lack of candidates");
            }

            // If a candidate was selected, update current solution
            if (selectedCandidate != null) {
                currentSolution = selectedCandidate.getSolution();
                currentScore = selectedCandidate.getScore();

                // Add to tabu list
                tabuList.add(currentSolution.hashCode());

                // Maintain tabu list size
                if (tabuList.size() > TABU_TENURE) {
                    tabuList.remove(tabuList.iterator().next());
                }

                // Update best solution if improved
                if (currentScore < bestScore) {
                    bestSolution = currentSolution.clone();
                    bestScore = currentScore;
                    iterationsWithoutImprovement = 0;
                    System.out.println("Iteration " + iteration + ": New best score: " + bestScore);
                } else {
                    iterationsWithoutImprovement++;
                }
            }

            // Cool down temperature
            temperature = Math.max(FINAL_TEMPERATURE, temperature * COOLING_RATE);

            // Termination condition: if temperature is too low and no improvement for a
            // while
            if (temperature <= FINAL_TEMPERATURE && iterationsWithoutImprovement > MAX_ITERATIONS / 5) {
                System.out.println("Early termination at iteration " + iteration + ": No improvement for " +
                        iterationsWithoutImprovement + " iterations and temperature at " + temperature);
                break;
            }
        }

        System.out.println("Tabu Search completed.");
        System.out.println("Best solution score: " + bestScore);

        return bestSolution;
    }

    /**
     * Generates candidate solutions by applying different moves to the current
     * solution
     */
    private List<CandidateSolution> generateCandidates(Environment env, Solution currentSolution) {
        List<CandidateSolution> candidates = new ArrayList<>();

        // Try to generate the requested number of candidates
        for (int i = 0; i < NUM_CANDIDATES; i++) {
            TabuMove move = selectRandomMove();
            Solution candidateSolution = move.apply(env, currentSolution);

            // Only add valid solutions that are different from the current one
            if (candidateSolution != null &&
                    !candidateSolution.equals(currentSolution)) {

                double score = Evaluator.evaluateSolution(env, candidateSolution);
                if (score != Double.NEGATIVE_INFINITY) {
                    candidates.add(new CandidateSolution(candidateSolution, score));
                }
            }
        }

        return candidates;
    }

    /**
     * Selects a random move type with equal probability
     */
    private TabuMove selectRandomMove() {
        int selection = random.nextInt(3);
        switch (selection) {
            case 0:
                return rearrangeMove;
            case 1:
                return swapVehicleMove;
            default:
                return swapStopMove;
        }
    }

    /**
     * Simple class to hold a candidate solution and its score
     */
    private static class CandidateSolution {
        private final Solution solution;
        private final double score;

        public CandidateSolution(Solution solution, double score) {
            this.solution = solution;
            this.score = score;
        }

        public Solution getSolution() {
            return solution;
        }

        public double getScore() {
            return score;
        }
    }
}