package com.vroute.orchest;

import com.vroute.models.Environment;
import com.vroute.models.MaintenanceTask;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleType;
import com.vroute.pathfinding.Grid;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for the MaintenanceScheduler
 */
public class MaintenanceSchedulerTest {
    public static void main(String[] args) {
        // Create a simple environment
        LocalDateTime startTime = LocalDateTime.of(2025, 5, 15, 8, 0);
        Environment env = createSimpleEnvironment(startTime);
        
        // Path to the maintenance file
        String maintenanceFilePath = "/home/nhrot/Programming/DP1/V-Route/data/mantpreventivo";
        
        // Load maintenance tasks
        System.out.println("Loading maintenance schedule from: " + maintenanceFilePath);
        int loaded = MaintenanceScheduler.loadMaintenanceSchedule(maintenanceFilePath, env, startTime.toLocalDate());
        
        // Print results
        System.out.println("Loaded " + loaded + " maintenance tasks");
        System.out.println("\nMaintenance tasks in environment:");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (MaintenanceTask task : env.getMaintenanceTasks()) {
            System.out.println("Vehicle: " + task.getVehicleId() + 
                              ", Date: " + task.getDate() +
                              ", Start: " + task.getStartTime().format(formatter) +
                              ", End: " + task.getEndTime().format(formatter));
        }
        
        // Test if a specific time is during maintenance
        LocalDateTime testTime = LocalDateTime.of(2025, 6, 10, 12, 0);
        System.out.println("\nChecking for maintenance at: " + testTime.format(formatter));
        
        for (Vehicle vehicle : env.getVehicles()) {
            boolean hasMaintenance = env.hasScheduledMaintenance(vehicle.getId(), testTime);
            if (hasMaintenance) {
                MaintenanceTask task = env.getMaintenanceTaskForVehicle(vehicle.getId(), testTime);
                System.out.println(vehicle.getId() + " has scheduled maintenance at this time: " + task);
            } else {
                System.out.println(vehicle.getId() + " has no scheduled maintenance at this time");
            }
        }
    }
    
    private static Environment createSimpleEnvironment(LocalDateTime startTime) {
        Grid grid = new Grid(10, 10);
        List<Vehicle> vehicles = new ArrayList<>();
        
        // Add vehicles that match our maintenance file
        vehicles.add(new Vehicle("TA01", VehicleType.TA, new Position(5, 5)));
        vehicles.add(new Vehicle("TB01", VehicleType.TB, new Position(5, 5)));
        vehicles.add(new Vehicle("TB02", VehicleType.TB, new Position(5, 5)));
        vehicles.add(new Vehicle("TC01", VehicleType.TC, new Position(5, 5)));
        vehicles.add(new Vehicle("TC02", VehicleType.TC, new Position(5, 5)));
        vehicles.add(new Vehicle("TD01", VehicleType.TD, new Position(5, 5)));
        vehicles.add(new Vehicle("TD02", VehicleType.TD, new Position(5, 5)));
        vehicles.add(new Vehicle("TD03", VehicleType.TD, new Position(5, 5)));
        vehicles.add(new Vehicle("TD04", VehicleType.TD, new Position(5, 5)));
        vehicles.add(new Vehicle("TD05", VehicleType.TD, new Position(5, 5)));
        
        return new Environment(grid, vehicles, null, new ArrayList<>(), startTime);
    }
}
