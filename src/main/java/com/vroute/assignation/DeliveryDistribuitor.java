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
        List<Vehicle> availableVehicles = environment.getAvailableVehicles();
        List<Order> pendingOrders = new ArrayList<>(environment.getPendingOrders());

        // If there are no pending orders, return an empty solution
        if (pendingOrders.isEmpty()) {
            System.err.println("Warning: No pending orders to assign.");
            // Still create empty lists for available vehicles
            for (Vehicle vehicle : availableVehicles) {
                assignments.put(vehicle, new ArrayList<>());
            }
            return new Solution(assignments);
        }

        // If there are no available vehicles, return an empty solution
        if (availableVehicles.isEmpty()) {
            System.err.println("Warning: No available vehicles for assignment.");
            return new Solution(new HashMap<>());
        }

        // Initialize assignment lists for each vehicle
        for (Vehicle vehicle : availableVehicles) {
            assignments.put(vehicle, new ArrayList<>());
        }

        // Sort orders by due time for better initial assignments
        pendingOrders.sort(Comparator.comparing(Order::getDueTime, Comparator.nullsLast(Comparator.naturalOrder())));

        // Assign each order
        for (Order order : pendingOrders) {
            int remainingGlpToAssign = order.getRemainingGlpM3();
            
            // Skip if order has no remaining GLP
            if (remainingGlpToAssign <= 0) {
                continue;
            }
            
            // Create a probability distribution for vehicles based on proximity
            List<Vehicle> sortedVehicles = getVehiclesSortedByProximity(availableVehicles, order);
            
            // Assign splits until the order is fully assigned
            while (remainingGlpToAssign > 0) {
                // Select a vehicle based on proximity (with some randomness)
                Vehicle selectedVehicle = selectVehicleWithBias(sortedVehicles);
                
                // Determine a split amount
                int maxSplit = Math.min(remainingGlpToAssign, selectedVehicle.getType().getCapacityM3());
                int splitAmount;
                
                if (maxSplit <= MIN_PRACTICAL_SPLIT_THRESHOLD) {
                    // If remaining amount is small, assign all of it
                    splitAmount = maxSplit;
                } else {
                    // Random split between MIN_PRACTICAL_SPLIT_THRESHOLD and maxSplit
                    splitAmount = MIN_PRACTICAL_SPLIT_THRESHOLD + 
                                 random.nextInt(maxSplit - MIN_PRACTICAL_SPLIT_THRESHOLD + 1);
                }
                
                // Create and add the delivery instruction
                DeliveryInstruction instruction = new DeliveryInstruction(order.clone(), splitAmount);
                assignments.get(selectedVehicle).add(instruction);
                
                // Update remaining GLP to assign
                remainingGlpToAssign -= splitAmount;
            }
        }
        
        return new Solution(assignments);
    }
    
    /**
     * Select a vehicle with bias towards those at the beginning of the list
     * (which are assumed to be closer to the order)
     */
    private Vehicle selectVehicleWithBias(List<Vehicle> sortedVehicles) {
        if (sortedVehicles.isEmpty()) {
            throw new IllegalArgumentException("No vehicles available for selection");
        }
        
        // Use a simple exponential bias toward the front of the list
        double random = Math.pow(this.random.nextDouble(), 2); // Square to bias toward 0
        int index = (int)(random * sortedVehicles.size());
        return sortedVehicles.get(Math.min(index, sortedVehicles.size() - 1));
    }
    
    /**
     * Get vehicles sorted by proximity to order
     */
    private List<Vehicle> getVehiclesSortedByProximity(List<Vehicle> vehicles, Order order) {
        List<Vehicle> sortedVehicles = new ArrayList<>(vehicles);
        final Position orderPosition = order.getPosition();
        
        if (orderPosition == null) {
            // If order has no position, shuffle randomly
            Collections.shuffle(sortedVehicles, random);
            return sortedVehicles;
        }
        
        // Sort by distance to the order
        sortedVehicles.sort(Comparator.comparingDouble(v -> {
            Position vehiclePosition = v.getCurrentPosition();
            return (vehiclePosition != null) ? vehiclePosition.distanceTo(orderPosition) : Double.MAX_VALUE;
        }));
        
        return sortedVehicles;
    }
}
