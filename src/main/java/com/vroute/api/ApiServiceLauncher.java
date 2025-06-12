package com.vroute.api;

import com.vroute.models.*;
import com.vroute.orchest.*;
import com.vroute.pathfinding.Grid;

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
    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    
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
        
        // Create orchestrator
        orchestrator = new Orchestrator(environment);
        orchestrator.initialize();
        
        // Load sample data
        loadSampleData();
        
        // Create API server
        apiServer = new ApiServer(environment, orchestrator, port);
        
        logger.info("V-Route API service initialized successfully");
    }
    
    private void createEnvironment() {
        LocalDateTime startTime = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        
        // Create grid
        Grid grid = new Grid(GRID_WIDTH, GRID_HEIGHT);
        
        // Create main depot
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 10000, true);
        mainDepot.refillGLP(); // Start with full capacity
        
        // Create auxiliary depots
        List<Depot> auxDepots = createAuxiliaryDepots();
        
        // Create vehicle fleet
        List<Vehicle> vehicles = createVehicleFleet();
        
        // Create environment
        environment = new Environment(grid, vehicles, mainDepot, auxDepots, startTime);
        
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
        
        // Load sample orders
        createSampleOrders(currentTime);
        
        // Load sample blockages
        createSampleBlockages(currentTime);
        
        // Try to load data from files if available
        tryLoadDataFromFiles(currentTime);
        
        logger.info("Sample data loaded: " + environment.getOrderQueue().size() + " orders, " + 
                   environment.getActiveBlockages().size() + " blockages");
    }
    
    private void createSampleOrders(LocalDateTime currentTime) {
        List<Order> orders = new ArrayList<>();
        
        // Create some sample orders with different priorities and locations
        orders.add(new Order("ORDER_001", currentTime.plusMinutes(30), currentTime.plusHours(4), 15, new Position(25, 15)));
        orders.add(new Order("ORDER_002", currentTime.plusMinutes(45), currentTime.plusHours(6), 8, new Position(35, 25)));
        orders.add(new Order("ORDER_003", currentTime.plusHours(1), currentTime.plusHours(8), 12, new Position(45, 35)));
        orders.add(new Order("ORDER_004", currentTime.plusMinutes(15), currentTime.plusHours(2), 20, new Position(15, 20))); // High priority
        orders.add(new Order("ORDER_005", currentTime.plusHours(2), currentTime.plusHours(12), 5, new Position(55, 10)));
        orders.add(new Order("ORDER_006", currentTime.plusMinutes(90), currentTime.plusHours(5), 18, new Position(30, 40)));
        orders.add(new Order("ORDER_007", currentTime.plusHours(3), currentTime.plusHours(15), 7, new Position(60, 30)));
        orders.add(new Order("ORDER_008", currentTime.plusMinutes(20), currentTime.plusHours(3), 25, new Position(20, 35))); // Large order
        
        for (Order order : orders) {
            environment.addOrder(order);
        }
    }
    
    private void createSampleBlockages(LocalDateTime currentTime) {
        List<Blockage> blockages = new ArrayList<>();
        
        // Create horizontal blockage
        List<Position> horizontalPoints = new ArrayList<>();
        for (int x = 30; x <= 35; x++) {
            horizontalPoints.add(new Position(x, 20));
        }
        blockages.add(new Blockage(currentTime.plusMinutes(30), currentTime.plusHours(4), horizontalPoints));
        
        // Create vertical blockage
        List<Position> verticalPoints = new ArrayList<>();
        for (int y = 25; y <= 30; y++) {
            verticalPoints.add(new Position(40, y));
        }
        blockages.add(new Blockage(currentTime.plusHours(1), currentTime.plusHours(6), verticalPoints));
        
        // Create diagonal-like blockage
        List<Position> diagonalPoints = new ArrayList<>();
        diagonalPoints.add(new Position(50, 15));
        diagonalPoints.add(new Position(51, 15));
        diagonalPoints.add(new Position(51, 16));
        diagonalPoints.add(new Position(52, 16));
        blockages.add(new Blockage(currentTime.plusHours(2), currentTime.plusHours(8), diagonalPoints));
        
        for (Blockage blockage : blockages) {
            environment.addBlockage(blockage);
        }
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
        
        // Run initial assignation
        logger.info("Running initial vehicle assignation...");
        orchestrator.runAssignation();
        
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
                // Run a simulation step every 30 seconds
                Thread.sleep(30000);
                
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
    }
}
