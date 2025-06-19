package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Vehicle;
import com.vroute.models.Order;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
public class MetaheuristicAssignator implements Assignator {

    // Hyperparameters for the metaheuristic algorithm
    private static final int DEFAULT_MAX_ITERATIONS = 3000;
    private static final int DEFAULT_TABU_LIST_SIZE = 25;
    private static final int DEFAULT_NUM_NEIGHBORS = 200;
    private static final double DUE_DATE_PENALTY_WEIGHT = 2.0;
    private static final int MAX_MINUTES_EARLY_BONUS = 30;
    private static final double LATE_PENALTY_EXPONENT = 1.5;
    private static final int DIVERSIFICATION_FACTOR = 2;
    private static final int REPORT_INTERVAL = 100;
    private static final double TEMPERATURE_INITIAL = 100.0;
    private static final double TEMPERATURE_DECAY = 0.995;

    private final DeliveryDistribuitor deliveryDistribuitor;
    private final SolutionGenerator solutionGenerator;
    private final int maxIterations;
    private final int tabuListSize;
    private final Random random = new Random();
    private Environment environment;
    private double temperature;

    public MetaheuristicAssignator(Environment environment) {
        this.deliveryDistribuitor = new DeliveryDistribuitor(environment);
        this.solutionGenerator = new SolutionGenerator(environment);
        this.maxIterations = DEFAULT_MAX_ITERATIONS;
        this.tabuListSize = DEFAULT_TABU_LIST_SIZE;
        this.environment = environment;
    }

    @Override
    public Solution solve(Environment env) {
        this.environment = env;
        this.temperature = TEMPERATURE_INITIAL;
        
        // Check if there are pending orders
        List<Order> pendingOrders = environment.getPendingOrders();
        if (pendingOrders.isEmpty()) {
            System.err.println("MetaheuristicAssignator: No pending orders to assign.");
            return new Solution(new HashMap<>());
        }
        
        // Check if there are available vehicles
        List<Vehicle> availableVehicles = environment.getAvailableVehicles();
        if (availableVehicles.isEmpty()) {
            System.err.println("MetaheuristicAssignator: No available vehicles for assignment.");
            return new Solution(new HashMap<>());
        }

        Solution currentSolution = deliveryDistribuitor.createInitialRandomAssignments();
        Solution bestSolution = currentSolution;

        // Check if the initial solution has any assignments (it might be empty if there are no valid orders)
        if (currentSolution.getVehicleOrderAssignments().isEmpty() || 
            currentSolution.getTotalDistance() == 0) {
            System.err.println("MetaheuristicAssignator: Initial solution is empty. Nothing to optimize.");
            return currentSolution;
        }

        LinkedList<TabuMove> tabuList = new LinkedList<>();

        int iterationsWithoutImprovement = 0;
        int maxIterationsWithoutImprovement = maxIterations / DIVERSIFICATION_FACTOR;

        for (int i = 0; i < maxIterations; i++) {
            Solution bestNeighbor = null;
            double bestNeighborScore = Double.MAX_VALUE;
            TabuMove bestMove = null;

            for (int j = 0; j < DEFAULT_NUM_NEIGHBORS; j++) {
                TabuMove move = solutionGenerator.generateRandomMove(currentSolution);
                Solution neighborSolution = solutionGenerator.applyMove(currentSolution, move);

                double neighborScore = calculateSolutionScore(neighborSolution);

                // Accept worse solutions based on simulated annealing probability
                boolean acceptWorseMove = false;
                if (neighborScore > calculateSolutionScore(currentSolution)) {
                    double acceptanceProbability = Math
                            .exp(-(neighborScore - calculateSolutionScore(currentSolution)) / temperature);
                    acceptWorseMove = random.nextDouble() < acceptanceProbability;
                }

                if ((neighborScore < bestNeighborScore || acceptWorseMove) &&
                        (!isTabu(tabuList, move) || neighborScore < calculateSolutionScore(bestSolution))) {
                    bestNeighbor = neighborSolution;
                    bestNeighborScore = neighborScore;
                    bestMove = move;
                }
            }

            if (bestNeighbor != null) {
                currentSolution = bestNeighbor;

                tabuList.add(bestMove);
                if (tabuList.size() > tabuListSize) {
                    tabuList.removeFirst();
                }

                double currentScore = calculateSolutionScore(currentSolution);
                double bestScore = calculateSolutionScore(bestSolution);

                if (currentScore < bestScore) {
                    bestSolution = currentSolution;
                    iterationsWithoutImprovement = 0;
                } else {
                    iterationsWithoutImprovement++;
                }

                if (iterationsWithoutImprovement > maxIterationsWithoutImprovement) {
                    currentSolution = solutionGenerator.diversify(currentSolution);
                    iterationsWithoutImprovement = 0;
                    temperature = TEMPERATURE_INITIAL * 0.5; // Reset temperature partially to encourage exploration
                    System.out.println("Diversification applied at iteration " + i);
                }

                if (i % REPORT_INTERVAL == 0) {
                    System.out.printf(
                            "Iteration %d: Current distance = %.2f, Best distance = %.2f, Temperature = %.2f%n",
                            i, currentSolution.getTotalDistance(), bestSolution.getTotalDistance(), temperature);
                }
            }

            // Cool down temperature for simulated annealing
            temperature *= TEMPERATURE_DECAY;
        }

        bestSolution = solutionGenerator.ensureAllOrdersDelivered(bestSolution);

        return bestSolution;
    }

    private boolean isTabu(LinkedList<TabuMove> tabuList, TabuMove move) {
        return tabuList.contains(move);
    }

    private double calculateSolutionScore(Solution solution) {
        double distanceScore = solution.getTotalDistance();
        double dueDatePenalty = solutionGenerator.calculateDueDatePenalty(solution, LATE_PENALTY_EXPONENT, MAX_MINUTES_EARLY_BONUS);

        return distanceScore + (dueDatePenalty * DUE_DATE_PENALTY_WEIGHT);
    }
}
