package com.vroute;

import com.vroute.models.*;
import com.vroute.ui.SimulationApp;
import com.vroute.orchest.DataReader;
import com.vroute.orchest.Orchestrator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create environment using data files and get orchestrator
        Orchestrator orchestrator = createEnvironmentFromFiles();
        
        // Launch UI directly and pass the orchestrator to it
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            SimulationApp app = new SimulationApp();
            app.setEnvironment(orchestrator.getEnvironment());
            app.setOrchestrator(orchestrator);
            // Make the app visible
            app.setVisible(true);
        });
    }
    
    private static Orchestrator createEnvironmentFromFiles() {
        // Initial date time
        LocalDateTime startDateTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        
        // Initialize data reader
        DataReader dataReader = new DataReader();
        
        // Main depot
        Depot mainDepot = new Depot(
            Constants.MAIN_PLANT_ID, 
            Constants.CENTRAL_STORAGE_LOCATION,
            500, true);
        mainDepot.refillGLP();
        
        // Aux depots
        List<Depot> auxDepots = new ArrayList<>();
        Depot northDepot = new Depot("NORTH_DEPOT", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true);
        northDepot.refillGLP();
        auxDepots.add(northDepot);
        
        Depot eastDepot = new Depot("EAST_DEPOT", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true);
        eastDepot.refillGLP();
        auxDepots.add(eastDepot);
        
        // Load vehicles (method doesn't require a file path)
        List<Vehicle> vehicles = dataReader.loadVehicles("");
        
        // Create environment
        Environment environment = new Environment(vehicles, mainDepot, auxDepots, startDateTime);
        
        // Initialize orchestrator with environment
        Orchestrator orchestrator = new Orchestrator(environment);
        
        // Check if data files exist and log appropriate messages
        String ordersFilePath = String.format("data/pedidos.20250419/ventas%s.txt",
                startDateTime.format(DateTimeFormatter.ofPattern("yyyyMM")));
        String blockagesFilePath = String.format("data/bloqueos.20250419/%s.bloqueos.txt",
                startDateTime.format(DateTimeFormatter.ofPattern("yyyyMM")));
        String maintenanceFilePath = "data/mantpreventivo.txt";
        
        // Check if files exist and provide appropriate feedback
        boolean ordersFileExists = new File(ordersFilePath).exists();
        boolean blockagesFileExists = new File(blockagesFilePath).exists();
        boolean maintenanceFileExists = new File(maintenanceFilePath).exists();
        
        if (!ordersFileExists) {
            System.err.println("Warning: Orders file not found: " + ordersFilePath);
            System.err.println("Using default orders.");
        }
        
        if (!blockagesFileExists) {
            System.err.println("Warning: Blockages file not found: " + blockagesFilePath);
            System.err.println("No blockages will be loaded.");
        }
        
        if (!maintenanceFileExists) {
            System.err.println("Warning: Maintenance file not found: " + maintenanceFilePath);
            System.err.println("No maintenance tasks will be loaded.");
        }
        
        // Load events (orders, blockages, maintenance tasks)
        try {
            orchestrator.loadEvents(
                ordersFilePath,
                blockagesFilePath, 
                maintenanceFilePath);
            System.out.println("Environment initialized with data from files.");
        } catch (Exception e) {
            System.err.println("Error loading data files: " + e.getMessage());
            System.err.println("Using default environment settings.");
            e.printStackTrace();
        }
        
        return orchestrator;
    }
}
