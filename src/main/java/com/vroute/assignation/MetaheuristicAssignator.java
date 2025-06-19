package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Vehicle;
import com.vroute.models.Order;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MetaheuristicAssignator implements Assignator {

    private static final int DEFAULT_MAX_ITERATIONS = 3000;
    private static final int DEFAULT_TABU_LIST_SIZE = 25;
    private static final int DEFAULT_NUM_NEIGHBORS = 100;
    private static final int DIVERSIFICATION_FACTOR = 2;
    private static final int REPORT_INTERVAL = 100;
    private static final int DETAILED_REPORT_INTERVAL = 500;
    private static final double TEMPERATURE_INITIAL = 100.0;
    private static final double TEMPERATURE_DECAY = 0.995;
    private static final double MINIMUM_SOLUTION_IMPROVEMENT = 0.001;

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

        // Create initial solution and ensure all orders are fully assigned
        Solution currentSolution = deliveryDistribuitor.createInitialRandomAssignments();
        currentSolution = solutionGenerator.ensureFullOrderAssignment(currentSolution);

        // Evaluar y mostrar la solución inicial
        double initialScore = SolutionEvaluator.evaluateSolution(currentSolution, environment);
        System.out.println("Initial solution score: " + initialScore);
        System.out.println("Initial distance: " + currentSolution.getTotalDistance());
        System.out.println("Detailed evaluation of initial solution:");
        System.out.println(SolutionEvaluator.getDetailedEvaluation(currentSolution, environment));

        Solution bestSolution = currentSolution;

        // Check if the initial solution has any assignments (it might be empty if there
        // are no valid orders)
        if (currentSolution.getVehicleOrderAssignments().isEmpty() ||
                currentSolution.getTotalDistance() == 0) {
            System.err.println("MetaheuristicAssignator: Initial solution is empty. Nothing to optimize.");
            return currentSolution;
        }

        LinkedList<TabuMove> tabuList = new LinkedList<>();

        int iterationsWithoutImprovement = 0;
        int maxIterationsWithoutImprovement = maxIterations / DIVERSIFICATION_FACTOR;

        double previousBestScore = initialScore;
        int bestSolutionIteration = 0;

        for (int i = 0; i < maxIterations; i++) {
            Solution bestNeighbor = null;
            double bestNeighborScore = Double.NEGATIVE_INFINITY;
            TabuMove bestMove = null;

            for (int j = 0; j < DEFAULT_NUM_NEIGHBORS; j++) {
                TabuMove move = solutionGenerator.generateRandomMove(currentSolution);
                Solution neighborSolution = solutionGenerator.applyMove(currentSolution, move);
                
                // Asegurar que la solución vecina tiene todas las órdenes asignadas completamente
                neighborSolution = solutionGenerator.ensureFullOrderAssignment(neighborSolution);
                
                double neighborScore = SolutionEvaluator.evaluateSolution(neighborSolution, environment);

                // Accept worse solutions based on simulated annealing probability
                boolean acceptWorseMove = false;
                double currentSolutionScore = SolutionEvaluator.evaluateSolution(currentSolution, environment);
                if (neighborScore < currentSolutionScore) {
                    double acceptanceProbability = Math.exp((neighborScore - currentSolutionScore) / temperature);
                    acceptWorseMove = random.nextDouble() < acceptanceProbability;
                }

                // Actualizar al mejor vecino si es mejor o se acepta un movimiento peor con SA
                double bestSolutionScore = SolutionEvaluator.evaluateSolution(bestSolution, environment);
                boolean isBetterThanCurrentBest = neighborScore > bestSolutionScore;
                
                if ((neighborScore > bestNeighborScore || acceptWorseMove) &&
                        (!isTabu(tabuList, move) || isBetterThanCurrentBest)) {
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

                double currentScore = SolutionEvaluator.evaluateSolution(currentSolution, environment);
                double bestScore = SolutionEvaluator.evaluateSolution(bestSolution, environment);

                // Actualizar mejor solución si el score es mejor (ahora mayor es mejor)
                if (currentScore > bestScore) {
                    bestSolution = currentSolution;
                    bestSolutionIteration = i;

                    // Check if the improvement is significant
                    double improvementPercentage = (currentScore - previousBestScore) / previousBestScore;
                    if (improvementPercentage > MINIMUM_SOLUTION_IMPROVEMENT) {
                        iterationsWithoutImprovement = 0;
                        previousBestScore = bestScore;
                        
                        // Log mejora significativa
                        System.out.println("Significant improvement at iteration " + i + 
                                          ": New best score = " + bestScore + 
                                          ", Improvement = " + String.format("%.2f%%", improvementPercentage * 100));
                    } else {
                        iterationsWithoutImprovement++;
                    }
                } else {
                    iterationsWithoutImprovement++;
                }

                // Aplicar diversificación si no hay mejora por un largo tiempo
                if (iterationsWithoutImprovement > maxIterationsWithoutImprovement) {
                    System.out.println("Diversification applied at iteration " + i + 
                                      " after " + iterationsWithoutImprovement + 
                                      " iterations without significant improvement");
                    
                    currentSolution = solutionGenerator.diversify(currentSolution);
                    // Ensure the diversified solution has all orders fully assigned
                    currentSolution = solutionGenerator.ensureFullOrderAssignment(currentSolution);
                    
                    iterationsWithoutImprovement = 0;
                    temperature = TEMPERATURE_INITIAL * 0.5; // Reset temperature partially to encourage exploration
                }

                // Reportes periódicos
                if (i % REPORT_INTERVAL == 0) {
                    System.out.printf(
                            "Iteration %d: Current Score = %.2f, Best Score = %.2f, Best found at it.%d, Distance = %.2f, Temp = %.2f%n",
                            i, currentScore, bestScore, bestSolutionIteration, bestSolution.getTotalDistance(), temperature);
                }
                
                // Reporte detallado ocasional
                if (i % DETAILED_REPORT_INTERVAL == 0) {
                    System.out.println("Detailed evaluation at iteration " + i + ":");
                    System.out.println(SolutionEvaluator.getDetailedEvaluation(bestSolution, environment));
                }
            }

            // Cool down temperature for simulated annealing
            temperature *= TEMPERATURE_DECAY;
        }

        // Final check to ensure all orders are delivered and assignments are valid
        bestSolution = solutionGenerator.ensureAllOrdersDelivered(bestSolution);
        bestSolution = solutionGenerator.ensureFullOrderAssignment(bestSolution);

        // Evaluación final
        System.out.println("Final solution found at iteration " + bestSolutionIteration);
        System.out.println("Final distance: " + bestSolution.getTotalDistance());
        System.out.println("Final evaluation:");
        System.out.println(SolutionEvaluator.getDetailedEvaluation(bestSolution, environment));

        return bestSolution;
    }

    private boolean isTabu(LinkedList<TabuMove> tabuList, TabuMove move) {
        return tabuList.contains(move);
    }
}
