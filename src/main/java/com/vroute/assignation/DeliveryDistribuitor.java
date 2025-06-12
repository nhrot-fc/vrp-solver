package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DeliveryDistribuitor {

    private final Environment environment;
    private final Random random = new Random();
    private static final int MIN_PRACTICAL_SPLIT_THRESHOLD = 1;

    public DeliveryDistribuitor(Environment environment) {
        this.environment = environment;
    }

    public Solution createInitialRandomAssignments() {
        Map<Vehicle, List<DeliveryInstruction>> assignments = new HashMap<>();
        List<Vehicle> allVehicles = environment.getAvailableVehicles();
        List<Order> pendingOrders = new ArrayList<>(environment.getPendingOrders());

        // If there are no pending orders, return an empty solution
        if (pendingOrders.isEmpty()) {
            System.err.println("Warning: No pending orders to assign.");
            // Still create empty lists for available vehicles
            for (Vehicle vehicle : allVehicles) {
                assignments.put(vehicle, new ArrayList<>());
            }
            return new Solution(assignments);
        }

        // If there are no available vehicles, return an empty solution
        if (allVehicles.isEmpty()) {
            System.err.println("Warning: No available vehicles for assignment.");
            return new Solution(new HashMap<>());
        }

        for (Vehicle vehicle : allVehicles) {
            assignments.put(vehicle, new ArrayList<>());
        }

        pendingOrders.sort(Comparator.comparing(Order::getDueTime, Comparator.nullsLast(Comparator.naturalOrder())));

        for (Order order : pendingOrders) {
            int totalGlpToAssignForThisOrder = order.getRemainingGlpM3();
            int glpAssignedSoFarForThisOrder = 0;

            if (totalGlpToAssignForThisOrder <= 0) {
                continue;
            }

            List<Vehicle> vehiclesSortedByProximity = new ArrayList<>(allVehicles);
            final Position orderPosition = order.getPosition();

            if (orderPosition == null) {
                System.err.println("Warning: Order " + order.getId() +
                        " has no position. Using random vehicle order for it.");
                Collections.shuffle(vehiclesSortedByProximity, random);
            } else {
                vehiclesSortedByProximity.sort(Comparator.comparingDouble(v -> {
                    Position vehiclePos = v.getCurrentPosition();
                    return (vehiclePos != null) ? vehiclePos.distanceTo(orderPosition) : Double.MAX_VALUE;
                }));
            }

            int currentVehicleIndexInSortedList = 0;

            while (glpAssignedSoFarForThisOrder < totalGlpToAssignForThisOrder) {
                if (vehiclesSortedByProximity.isEmpty()
                        || currentVehicleIndexInSortedList >= vehiclesSortedByProximity.size()) {
                    System.err.println("Warning: Unable to assign full amount for order " + order.getId() +
                            ". Assigned " + glpAssignedSoFarForThisOrder + "/" + totalGlpToAssignForThisOrder +
                            ". No more vehicles to try from the sorted list.");
                    break;
                }

                Vehicle targetVehicle = vehiclesSortedByProximity.get(currentVehicleIndexInSortedList);
                
                // Get the vehicle's maximum capacity
                int vehicleMaxCapacity = targetVehicle.getType().getCapacityM3();
                
                // Check how many assignments this vehicle already has
                int currentVehicleAssignmentCount = assignments.get(targetVehicle).size();
                
                // If this vehicle already has too many assignments, try the next one
                if (currentVehicleAssignmentCount >= 10) { // Arbitrary limit to avoid overloading vehicles
                    currentVehicleIndexInSortedList++;
                    continue;
                }
                
                int remainingToAssignCurrently = totalGlpToAssignForThisOrder - glpAssignedSoFarForThisOrder;
                int amountForThisInstruction;

                // Consider vehicle capacity when assigning
                if (remainingToAssignCurrently <= MIN_PRACTICAL_SPLIT_THRESHOLD) {
                    amountForThisInstruction = remainingToAssignCurrently;
                } else {
                    int maxPossibleForVehicle = Math.min(vehicleMaxCapacity, remainingToAssignCurrently);
                    
                    // Ensure we're not going below our minimum threshold
                    int effectiveMin = Math.min(MIN_PRACTICAL_SPLIT_THRESHOLD, maxPossibleForVehicle);
                    
                    // Calculate a random amount between min and max
                    if (maxPossibleForVehicle > effectiveMin) {
                        amountForThisInstruction = effectiveMin +
                                random.nextInt(maxPossibleForVehicle - effectiveMin + 1);
                    } else {
                        amountForThisInstruction = maxPossibleForVehicle;
                    }
                }

                amountForThisInstruction = Math.min(amountForThisInstruction, remainingToAssignCurrently);
                if (remainingToAssignCurrently > 0 && amountForThisInstruction <= 0) {
                    amountForThisInstruction = Math.min(1, remainingToAssignCurrently);
                }

                if (amountForThisInstruction > 0) {
                    DeliveryInstruction instruction = new DeliveryInstruction(order.clone(), amountForThisInstruction);
                    assignments.get(targetVehicle).add(instruction);
                    glpAssignedSoFarForThisOrder += amountForThisInstruction;
                }
                currentVehicleIndexInSortedList++;
            }
        }
        return new Solution(assignments);
    }
}
