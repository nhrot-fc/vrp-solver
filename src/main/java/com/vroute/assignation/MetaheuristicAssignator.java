package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Vehicle;
import com.vroute.models.Order;
import com.vroute.models.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.time.LocalDateTime;

public class MetaheuristicAssignator implements Assignator {

    // Hyperparameters for the metaheuristic algorithm
    private static final int DEFAULT_MAX_ITERATIONS = 3000;
    private static final int DEFAULT_TABU_LIST_SIZE = 30;
    private static final int DEFAULT_NUM_NEIGHBORS = 100;
    private static final double DUE_DATE_PENALTY_WEIGHT = 2.0;
    private static final int MAX_MINUTES_EARLY_BONUS = 30;
    private static final double LATE_PENALTY_EXPONENT = 1.5;
    private static final int DIVERSIFICATION_FACTOR = 2;
    private static final int REPORT_INTERVAL = 100;
    private static final double TEMPERATURE_INITIAL = 100.0;
    private static final double TEMPERATURE_DECAY = 0.995;

    private final DeliveryDistribuitor deliveryDistribuitor;
    private final int maxIterations;
    private final int tabuListSize;
    private final Random random = new Random();
    private Environment environment;
    private double temperature;

    public MetaheuristicAssignator(Environment environment) {
        this.deliveryDistribuitor = new DeliveryDistribuitor(environment);
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
                TabuMove move = generateRandomMove(currentSolution);
                Solution neighborSolution = applyMove(currentSolution, move);

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
                    currentSolution = diversify(currentSolution);
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

        bestSolution = ensureAllOrdersDelivered(bestSolution);

        return bestSolution;
    }

    private TabuMove generateRandomMove(Solution solution) {
        Map<Vehicle, List<DeliveryInstruction>> assignments = solution.getVehicleOrderAssignments();
        List<Vehicle> vehicles = new ArrayList<>(assignments.keySet());
        
        // Safety check for empty assignments
        if (vehicles.isEmpty()) {
            System.err.println("Warning: No vehicles with assignments available for move generation");
            // Create a dummy TabuMove that will result in no changes when applied
            Vehicle dummyVehicle = environment.getAvailableVehicles().isEmpty() ?
                new Vehicle("DUMMY", null, null) :
                environment.getAvailableVehicles().get(0);
            return new TabuMove(dummyVehicle, 0, 0);
        }

        if (vehicles.size() < 2) {
            Vehicle vehicle = vehicles.get(0);
            List<DeliveryInstruction> instructions = assignments.get(vehicle);
            
            // Check if there are any instructions for this vehicle
            if (instructions == null || instructions.isEmpty()) {
                // No instructions at all
                return new TabuMove(vehicle, 0, 0);
            }
            
            if (instructions.size() < 2) {
                return new TabuMove(vehicle, 0, 0);
            }
            
            int idx1 = random.nextInt(instructions.size());
            int idx2;
            do {
                idx2 = random.nextInt(instructions.size());
            } while (idx1 == idx2);

            return new TabuMove(vehicle, idx1, idx2);
        }

        // Adjust probability of move types based on temperature to improve exploration
        int moveType;
        if (temperature > TEMPERATURE_INITIAL * 0.5) {
            // More diverse moves when temperature is high
            moveType = random.nextInt(3);
        } else {
            // More local rearrangement when temperature is low
            moveType = random.nextDouble() < 0.7 ? 2 : random.nextInt(2);
        }

        if (moveType <= 1) {
            Vehicle sourceVehicle, targetVehicle;
            do {
                sourceVehicle = vehicles.get(random.nextInt(vehicles.size()));
                targetVehicle = vehicles.get(random.nextInt(vehicles.size()));
            } while (sourceVehicle == targetVehicle);

            List<DeliveryInstruction> sourceInstructions = assignments.get(sourceVehicle);
            if (sourceInstructions == null || sourceInstructions.isEmpty()) {
                return generateRandomMove(solution);
            }

            int sourceIndex = random.nextInt(sourceInstructions.size());
            return new TabuMove(sourceVehicle, sourceIndex, targetVehicle);
        } else if (moveType == 2) {
            Vehicle sourceVehicle, targetVehicle;
            do {
                sourceVehicle = vehicles.get(random.nextInt(vehicles.size()));
                targetVehicle = vehicles.get(random.nextInt(vehicles.size()));
            } while (sourceVehicle == targetVehicle);

            List<DeliveryInstruction> sourceInstructions = assignments.get(sourceVehicle);
            List<DeliveryInstruction> targetInstructions = assignments.get(targetVehicle);

            if (sourceInstructions == null || sourceInstructions.isEmpty() ||
                    targetInstructions == null || targetInstructions.isEmpty()) {
                return generateRandomMove(solution);
            }

            int sourceIndex = random.nextInt(sourceInstructions.size());
            int targetIndex = random.nextInt(targetInstructions.size());

            return new TabuMove(sourceVehicle, sourceIndex, targetVehicle, targetIndex);
        } else {
            Vehicle vehicle = vehicles.get(random.nextInt(vehicles.size()));
            List<DeliveryInstruction> instructions = assignments.get(vehicle);

            if (instructions == null || instructions.size() < 2) {
                return generateRandomMove(solution);
            }

            int idx1 = random.nextInt(instructions.size());
            int idx2;
            do {
                idx2 = random.nextInt(instructions.size());
            } while (idx1 == idx2);

            return new TabuMove(vehicle, idx1, idx2);
        }
    }

    private Solution applyMove(Solution solution, TabuMove move) {
        // Safety check for empty solutions
        if (solution.getVehicleOrderAssignments().isEmpty()) {
            System.err.println("Warning: Attempting to apply move to empty solution");
            return solution;
        }
        
        Map<Vehicle, List<DeliveryInstruction>> currentAssignments = solution.getVehicleOrderAssignments();
        Map<Vehicle, List<DeliveryInstruction>> newAssignments = new HashMap<>();
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : currentAssignments.entrySet()) {
            newAssignments.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        Vehicle sourceVehicle = move.getSourceVehicle();
        Vehicle targetVehicle = move.getTargetVehicle();
        
        // Check if vehicles exist in assignments
        if (!newAssignments.containsKey(sourceVehicle)) {
            System.err.println("Warning: Source vehicle " + sourceVehicle.getId() + " not found in assignments");
            return solution;
        }
        
        List<DeliveryInstruction> sourceInstructions = newAssignments.get(sourceVehicle);
        List<DeliveryInstruction> targetInstructions = newAssignments.get(targetVehicle);

        // Validate source instructions
        if (sourceInstructions == null || sourceInstructions.isEmpty() ||
                move.getSourceInstructionIndex() >= sourceInstructions.size()) {
            System.err.println("Warning: Invalid source instruction index or empty source instructions");
            return solution;
        }

        // Create target instructions list if it doesn't exist
        if (targetInstructions == null) {
            targetInstructions = new ArrayList<>();
            newAssignments.put(targetVehicle, targetInstructions);
        }

        switch (move.getMoveType()) {
            case TRANSFER:
                DeliveryInstruction instructionToTransfer = sourceInstructions.remove(move.getSourceInstructionIndex());
                targetInstructions.add(instructionToTransfer);
                break;

            case SWAP:
                if (move.getTargetInstructionIndex() >= targetInstructions.size()) {
                    return solution;
                }

                DeliveryInstruction sourceInstruction = sourceInstructions.get(move.getSourceInstructionIndex());
                DeliveryInstruction targetInstruction = targetInstructions.get(move.getTargetInstructionIndex());

                sourceInstructions.set(move.getSourceInstructionIndex(), targetInstruction);
                targetInstructions.set(move.getTargetInstructionIndex(), sourceInstruction);
                break;

            case REORDER:
                if (move.getTargetInstructionIndex() >= sourceInstructions.size()) {
                    return solution;
                }

                DeliveryInstruction instructionToReorder = sourceInstructions.remove(move.getSourceInstructionIndex());
                sourceInstructions.add(move.getTargetInstructionIndex(), instructionToReorder);
                break;
        }

        return new Solution(newAssignments);
    }

    private boolean isTabu(LinkedList<TabuMove> tabuList, TabuMove move) {
        return tabuList.contains(move);
    }

    private double calculateSolutionScore(Solution solution) {
        double distanceScore = solution.getTotalDistance();
        double dueDatePenalty = calculateDueDatePenalty(solution);

        return distanceScore + (dueDatePenalty * DUE_DATE_PENALTY_WEIGHT);
    }

    private double calculateDueDatePenalty(Solution solution) {
        double totalPenalty = 0.0;
        LocalDateTime now = environment.getCurrentTime();
        Map<Vehicle, List<DeliveryInstruction>> assignments = solution.getVehicleOrderAssignments();

        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : assignments.entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<DeliveryInstruction> instructions = entry.getValue();

            Position currentPosition = vehicle.getCurrentPosition();
            double travelTimeMinutes = 0.0;

            for (DeliveryInstruction instruction : instructions) {
                double distance = currentPosition.distanceTo(instruction.getCustomerPosition());
                double travelTimeForThisLeg = (distance / 60.0) * 60.0;
                travelTimeMinutes += travelTimeForThisLeg;

                LocalDateTime estimatedArrival = now.plusMinutes((long) travelTimeMinutes);
                LocalDateTime dueDate = instruction.getDueDate();

                if (estimatedArrival.isAfter(dueDate)) {
                    long minutesLate = java.time.Duration.between(dueDate, estimatedArrival).toMinutes();
                    totalPenalty += Math.pow(minutesLate, LATE_PENALTY_EXPONENT);
                } else {
                    long minutesEarly = java.time.Duration.between(estimatedArrival, dueDate).toMinutes();
                    totalPenalty -= Math.min(minutesEarly, MAX_MINUTES_EARLY_BONUS);
                }

                currentPosition = instruction.getCustomerPosition();
            }
        }

        return totalPenalty;
    }

    private Solution diversify(Solution solution) {
        // Improved diversification strategy
        Map<Vehicle, List<DeliveryInstruction>> currentAssignments = solution.getVehicleOrderAssignments();
        Map<Vehicle, List<DeliveryInstruction>> newAssignments = new HashMap<>();

        List<DeliveryInstruction> allInstructions = new ArrayList<>();
        for (List<DeliveryInstruction> instructions : currentAssignments.values()) {
            allInstructions.addAll(instructions);
        }

        // Shuffle all instructions for randomization
        Collections.shuffle(allInstructions, random);

        List<Vehicle> vehicles = new ArrayList<>(currentAssignments.keySet());

        // Use more advanced distribution strategy
        if (random.nextDouble() < 0.5) {
            // Strategy 1: Distribute evenly
            for (Vehicle vehicle : vehicles) {
                newAssignments.put(vehicle, new ArrayList<>());
            }

            int vehicleIndex = 0;
            for (DeliveryInstruction instruction : allInstructions) {
                newAssignments.get(vehicles.get(vehicleIndex)).add(instruction);
                vehicleIndex = (vehicleIndex + 1) % vehicles.size();
            }
        } else {
            // Strategy 2: Group by proximity when possible
            Map<Position, List<DeliveryInstruction>> clusters = new HashMap<>();

            for (DeliveryInstruction instruction : allInstructions) {
                Position customerPosition = instruction.getCustomerPosition();
                boolean foundCluster = false;

                for (Position center : clusters.keySet()) {
                    if (center.distanceTo(customerPosition) < 20.0) { // Arbitrary distance threshold
                        clusters.get(center).add(instruction);
                        foundCluster = true;
                        break;
                    }
                }

                if (!foundCluster) {
                    List<DeliveryInstruction> newCluster = new ArrayList<>();
                    newCluster.add(instruction);
                    clusters.put(customerPosition, newCluster);
                }
            }

            for (Vehicle vehicle : vehicles) {
                newAssignments.put(vehicle, new ArrayList<>());
            }

            List<List<DeliveryInstruction>> clusterList = new ArrayList<>(clusters.values());
            Collections.shuffle(clusterList, random);

            int vehicleIndex = 0;
            for (List<DeliveryInstruction> cluster : clusterList) {
                newAssignments.get(vehicles.get(vehicleIndex)).addAll(cluster);
                vehicleIndex = (vehicleIndex + 1) % vehicles.size();
            }
        }

        return new Solution(newAssignments);
    }

    private Solution ensureAllOrdersDelivered(Solution solution) {
        Map<Vehicle, List<DeliveryInstruction>> currentAssignments = solution.getVehicleOrderAssignments();

        // If there are no assignments at all (no vehicles), return as is
        if (currentAssignments.isEmpty()) {
            System.err.println("No vehicle assignments available to ensure order delivery.");
            return solution;
        }

        List<Order> pendingOrders = environment.getPendingOrders();
        
        // If there are no pending orders, return as is
        if (pendingOrders.isEmpty()) {
            return solution;
        }

        Set<String> assignedOrderIds = new HashSet<>();
        for (List<DeliveryInstruction> instructions : currentAssignments.values()) {
            for (DeliveryInstruction instruction : instructions) {
                assignedOrderIds.add(instruction.getOrderId());
            }
        }

        List<Order> unassignedOrders = new ArrayList<>();
        for (Order order : pendingOrders) {
            if (!assignedOrderIds.contains(order.getId()) && !order.isDelivered()) {
                unassignedOrders.add(order);
            }
        }

        // If all orders are already assigned, return as is
        if (unassignedOrders.isEmpty()) {
            return solution;
        }

        Map<Vehicle, List<DeliveryInstruction>> newAssignments = new HashMap<>();
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : currentAssignments.entrySet()) {
            newAssignments.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        List<Vehicle> sortedVehicles = new ArrayList<>(newAssignments.keySet());
        // If there are no vehicles with assignments, return original solution
        if (sortedVehicles.isEmpty()) {
            System.err.println("No vehicles available to assign remaining orders.");
            return solution;
        }
        
        Collections.sort(sortedVehicles, Comparator.comparingInt(v -> newAssignments.get(v).size()));

        for (Order order : unassignedOrders) {
            DeliveryInstruction instruction = new DeliveryInstruction(order, order.getRemainingGlpM3());

            Vehicle leastLoadedVehicle = sortedVehicles.get(0);
            newAssignments.get(leastLoadedVehicle).add(instruction);

            Collections.sort(sortedVehicles, Comparator.comparingInt(v -> newAssignments.get(v).size()));
        }

        return new Solution(newAssignments);
    }
}
