package com.vroute.api;

import com.vroute.models.*;
import com.vroute.orchest.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Scanner;

/**
 * Service launcher that starts the V-Route system with HTTP API server.
 * Provides a complete setup with sample data and REST endpoints.
 */
public class ApiServiceLauncher {
    
    private static final Logger logger = Logger.getLogger(ApiServiceLauncher.class.getName());
    
    private static final int DEFAULT_PORT = 8080;
    
    private Environment environment;
    private Orchestrator orchestrator;
    private ApiServer apiServer;
    private DataReader dataReader;
    
    public static void main(String[] args) {
        ApiServiceLauncher launcher = new ApiServiceLauncher();
        
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
            launcher.initialize(port);
            launcher.start();
            launcher.waitForShutdown();
        } catch (Exception e) {
            logger.severe("Error starting API service: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void initialize(int port) throws Exception {
        logger.info("Initializing V-Route API service...");
        
        // Initialize data reader
        dataReader = new DataReader();
        
        // Create environment with sample data
        createEnvironment();
        
        // Load sample data
        loadSampleData();

        // Create orchestrator
        orchestrator = new Orchestrator(environment);
        orchestrator.initialize();
        
        // Create API server
        apiServer = new ApiServer(environment, orchestrator, port);
        
        logger.info("V-Route API service initialized successfully");
    }
    
    private void createEnvironment() {
        LocalDateTime startTime = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        
        // Create main depot
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 10000, true);
        mainDepot.refillGLP(); // Start with full capacity
        
        // Create auxiliary depots
        List<Depot> auxDepots = createAuxiliaryDepots();
        
        // Create vehicle fleet
        List<Vehicle> vehicles = createVehicleFleet();
        
        // Create environment
        environment = new Environment(vehicles, mainDepot, auxDepots, startTime);
        
        logger.info("Environment created with " + vehicles.size() + " vehicles and " + auxDepots.size() + " auxiliary depots");
    }
    
    private List<Depot> createAuxiliaryDepots() {
        List<Depot> depots = new ArrayList<>();
        
        // North intermediate storage
        Depot northDepot = new Depot("NORTH_DEPOT", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 5000, true);
        northDepot.refillGLP();
        depots.add(northDepot);
        
        // East intermediate storage
        Depot eastDepot = new Depot("EAST_DEPOT", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 3000, true);
        eastDepot.refillGLP();
        depots.add(eastDepot);
        
        return depots;
    }
    
    private List<Vehicle> createVehicleFleet() {
        List<Vehicle> vehicles = new ArrayList<>();
        Position startPosition = Constants.CENTRAL_STORAGE_LOCATION;
        
        // Create TA vehicles (2 units)
        for (int i = 1; i <= Constants.TA_UNITS; i++) {
            Vehicle vehicle = new Vehicle(String.format("TA%02d", i), VehicleType.TA, startPosition.clone());
            vehicle.refuel(); // Start with full fuel
            vehicles.add(vehicle);
        }
        
        // Create TB vehicles (4 units)
        for (int i = 1; i <= Constants.TB_UNITS; i++) {
            Vehicle vehicle = new Vehicle(String.format("TB%02d", i), VehicleType.TB, startPosition.clone());
            vehicle.refuel();
            vehicles.add(vehicle);
        }
        
        // Create TC vehicles (4 units)
        for (int i = 1; i <= Constants.TC_UNITS; i++) {
            Vehicle vehicle = new Vehicle(String.format("TC%02d", i), VehicleType.TC, startPosition.clone());
            vehicle.refuel();
            vehicles.add(vehicle);
        }
        
        // Create TD vehicles (10 units)
        for (int i = 1; i <= Constants.TD_UNITS; i++) {
            Vehicle vehicle = new Vehicle(String.format("TD%02d", i), VehicleType.TD, startPosition.clone());
            vehicle.refuel();
            vehicles.add(vehicle);
        }
        
        return vehicles;
    }
    
    private void loadSampleData() {
        LocalDateTime currentTime = environment.getCurrentTime();
        // Try to load data from files if available
        tryLoadDataFromFiles(currentTime);
        
        logger.info("Sample data loaded: " + environment.getOrderQueue().size() + " orders, " + 
                   environment.getActiveBlockages().size() + " blockages");
    }

    private void tryLoadDataFromFiles(LocalDateTime currentTime) {
        try {
            // Try to load orders from file
            String ordersPath = String.format("/home/nhrot/Programming/DP1/V-Route/data/pedidos.20250419/ventas%d%02d.txt", 
                                            currentTime.getYear(), currentTime.getMonthValue());
            java.io.File ordersFile = new java.io.File(ordersPath);
            if (ordersFile.exists()) {
                List<Order> fileOrders = dataReader.loadOrders(ordersPath, currentTime, 24, 20); // Limit to 20 orders
                environment.addOrders(fileOrders);
                logger.info("Loaded " + fileOrders.size() + " orders from file");
            }
            
            // Try to load blockages from file
            String blockagesPath = String.format("/home/nhrot/Programming/DP1/V-Route/data/bloqueos.20250419/%d%02d.bloqueos.txt", 
                                                currentTime.getYear(), currentTime.getMonthValue());
            java.io.File blockagesFile = new java.io.File(blockagesPath);
            if (blockagesFile.exists()) {
                List<Blockage> fileBlockages = dataReader.loadBlockages(blockagesPath, currentTime, 24, 10); // Limit to 10 blockages
                environment.addBlockages(fileBlockages);
                logger.info("Loaded " + fileBlockages.size() + " blockages from file");
            }
            
        } catch (Exception e) {
            logger.warning("Could not load data from files: " + e.getMessage());
        }
    }
    
    public void start() throws Exception {
        // Start API server
        apiServer.start();
        
        // Start a background thread to periodically update the simulation
        Thread simulationThread = new Thread(this::runSimulationLoop);
        simulationThread.setDaemon(true);
        simulationThread.start();
        
        logger.info("V-Route API service started successfully!");
        logger.info("Access the API at: http://localhost:" + DEFAULT_PORT);
        logger.info("API documentation: http://localhost:" + DEFAULT_PORT + "/");
        logger.info("System status: http://localhost:" + DEFAULT_PORT + "/api/status");
    }
    
    private void runSimulationLoop() {
        try {
            while (true) {
                // Run a simulation step every 0.5 seconds
                Thread.sleep(500);
                
                boolean continueSimulation = orchestrator.runSimulationStep();
                if (!continueSimulation) {
                    logger.info("Simulation completed");
                    break;
                }
            }
        } catch (InterruptedException e) {
            logger.info("Simulation loop interrupted");
        } catch (Exception e) {
            logger.severe("Error in simulation loop: " + e.getMessage());
        }
    }
    
    public void stop() {
        if (apiServer != null) {
            apiServer.stop();
        }
        logger.info("V-Route API service stopped");
    }
    
    private void waitForShutdown() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== V-Route API Service Running ===");
        System.out.println("Press Enter to stop the service...");
        scanner.nextLine();
        stop();
        scanner.close();
    }
}
