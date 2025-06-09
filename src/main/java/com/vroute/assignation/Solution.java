package com.vroute.assignation;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.vroute.models.Position;
import com.vroute.models.Vehicle;

public class Solution {
    private final Map<Vehicle, List<DeliveryInstruction>> vehicleOrderAssignments;
    private final double totalDistance;

    public Solution(Map<Vehicle, List<DeliveryInstruction>> vehicleOrderAssignments) {
        this.vehicleOrderAssignments = new HashMap<>(vehicleOrderAssignments);
        this.totalDistance = calculateDistance();
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public Map<Vehicle, List<DeliveryInstruction>> getVehicleOrderAssignments() {
        return vehicleOrderAssignments;
    }

    private double calculateDistance() {
        double distance = 0.0;
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : vehicleOrderAssignments.entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<DeliveryInstruction> instructions = entry.getValue();

            Position start = vehicle.getCurrentPosition();
            for (DeliveryInstruction instruction : instructions) {
                Position end = instruction.getCustomerPosition();
                distance += start.distanceTo(end);
                start = end;
            }
        }
        return distance;
    }

    @Override
    public String toString() {
        int totalOrdersAssignedCount = 0;
        for (List<DeliveryInstruction> instructions : vehicleOrderAssignments.values()) {
            totalOrdersAssignedCount += instructions.size();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(
                String.format("Solution: %d delivery instructions assigned, total distance: %.2f km\n",
                        totalOrdersAssignedCount, totalDistance));

        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : vehicleOrderAssignments.entrySet()) {
            sb.append(String.format("  Vehicle %s: %d instructions\n",
                    entry.getKey().getId(), entry.getValue().size()));
        }

        return sb.toString();
    }
}
