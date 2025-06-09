package com.vroute.orchest;

import com.vroute.assignation.DeliveryInstruction;
import com.vroute.assignation.Solution;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleStatus;
import com.vroute.operation.ActionType;
import com.vroute.operation.VehicleAction;
import com.vroute.operation.VehiclePlan;
import com.vroute.operation.VehiclePlanCreator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Orchestrator {
    private List<Order> ordersQueue;
    private Environment environment;
    private Map<Vehicle, VehiclePlan> vehiclePlans;
    private LocalDateTime simulationTime;
    private boolean simulationRunning;
    private int simulationStepMinutes;

    public Orchestrator(Environment environment, int simulationStepMinutes) {
        this.environment = environment;
        this.vehiclePlans = new HashMap<>();
        this.simulationTime = environment.getCurrentTime();
        this.simulationRunning = false;
        this.simulationStepMinutes = simulationStepMinutes;
    }

    public Map<Vehicle, VehiclePlan> generateVehiclePlans(Solution solution) {
        VehiclePlanCreator planGenerator = new VehiclePlanCreator(this.environment);
        Map<Vehicle, List<DeliveryInstruction>> assignments = solution.getVehicleOrderAssignments();

        System.out.println("\n======= GENERATING VEHICLE PLANS =======");

        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : assignments.entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<DeliveryInstruction> instructions = entry.getValue();

            if (!instructions.isEmpty()) {
                System.out.println("\n--- Generating plan for " + vehicle.getId() + " with " +
                        instructions.size() + " instructions ---");

                VehiclePlan plan = planGenerator.createPlan(vehicle, instructions, this.simulationTime);

                if (plan != null) {
                    this.vehiclePlans.put(vehicle, plan);
                    System.out.println(plan);
                } else {
                    System.out.println("Failed to create plan for " + vehicle.getId());
                }
            } else {
                System.out.println("No instructions for " + vehicle.getId());
            }
        }

        System.out.println("\n=== PLAN GENERATION SUMMARY ===");
        System.out.println("Total vehicles: " + assignments.size());
        System.out.println("Vehicles with plans: " + this.vehiclePlans.size());
        System.out.println("======================================");

        return new HashMap<>(this.vehiclePlans);
    }

    public void runAssignation() {

    }
}
