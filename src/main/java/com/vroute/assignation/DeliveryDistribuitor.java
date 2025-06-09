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

        if (allVehicles.isEmpty()) {
            System.err.println("Warning: No available vehicles for assignment.");
            return new Solution(new HashMap<>());
        }

        for (Vehicle vehicle : allVehicles) {
            assignments.put(vehicle, new ArrayList<>());
        }

        pendingOrders.sort(Comparator.comparing(Order::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));

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
                int remainingToAssignCurrently = totalGlpToAssignForThisOrder - glpAssignedSoFarForThisOrder;
                int amountForThisInstruction;

                if (remainingToAssignCurrently <= MIN_PRACTICAL_SPLIT_THRESHOLD) {
                    amountForThisInstruction = remainingToAssignCurrently;
                } else {
                    amountForThisInstruction = MIN_PRACTICAL_SPLIT_THRESHOLD +
                            random.nextInt(remainingToAssignCurrently - MIN_PRACTICAL_SPLIT_THRESHOLD + 1);
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
