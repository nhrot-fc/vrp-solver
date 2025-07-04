package com.vroute.api;

import com.vroute.models.*;
import com.vroute.orchest.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service launcher that starts the V-Route system with HTTP API server.
 * Provides a complete setup with sample data and REST endpoints.
 */
public class ApiServiceLauncher {
    
    private static final Logger logger = Logger.getLogger(ApiServiceLauncher.class.getName());
    
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_SIMULATION_SPEED = 500; // milliseconds between ticks
    
    private Environment environment;
    private Orchestrator orchestrator;
    private ApiServer apiServer;
    private DataReader dataReader;
    private List<Event> eventList;
    private Thread simulationThread;
    private AtomicBoolean simulationRunning;
    private int simulationSpeed;
    
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
    
    public ApiServiceLauncher() {
        this.eventList = new ArrayList<>();
        this.simulationRunning = new AtomicBoolean(false);
        this.simulationSpeed = DEFAULT_SIMULATION_SPEED;
    }
    
    public void initialize(int port) throws Exception {
        logger.info("Initializing V-Route API service...");
        
        // Initialize data reader
        dataReader = new DataReader();
        
        // Create environment with start date
        LocalDateTime simulationStartTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        createEnvironment(simulationStartTime);
        
        // Load data from files
        loadDataFromFiles(simulationStartTime);
        
        // Create orchestrator
        orchestrator = new Orchestrator(environment);
        orchestrator.addEvents(eventList);
        orchestrator.initialize();
        
        // Create API server
        apiServer = new ApiServer(environment, orchestrator, this, port);
        
        logger.info("V-Route API service initialized successfully");
    }
    
    private void createEnvironment(LocalDateTime startTime) {
        // Create main depot
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 100000, true);
        mainDepot.refillGLP(); // Start with full capacity
        List<Depot> auxDepots = createAuxiliaryDepots();
        List<Vehicle> vehicles = createVehicleFleet(mainDepot.getPosition());
        environment = new Environment(vehicles, mainDepot, auxDepots, startTime);
        logger.info("Environment created with " + vehicles.size() + " vehicles and " + auxDepots.size() + " auxiliary depots");
    }
    
    private List<Depot> createAuxiliaryDepots() {
        List<Depot> depots = new ArrayList<>();
        
        // North intermediate storage
        Depot northDepot = new Depot("NORTH_DEPOT", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true);
        northDepot.refillGLP();
        depots.add(northDepot);
        
        // East intermediate storage
        Depot eastDepot = new Depot("EAST_DEPOT", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true);
        eastDepot.refillGLP();
        depots.add(eastDepot);
        
        return depots;
    }
    
    private List<Vehicle> createVehicleFleet(Position startPosition) {
        List<Vehicle> vehicles = new ArrayList<>();
        
        // Create TA vehicles (3 units)
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
        
        // Create TC vehicles (3 units)
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
    
    private void loadDataFromFiles(LocalDateTime startTime) {
        try {
            // Base path for data files
            Path dataPath = Paths.get("/home/nhrot/Programming/DP1/V-Route/data");

            // Load orders from current month file
            String ordersFileName = String.format("ventas%d%02d.txt", startTime.getYear(), startTime.getMonthValue());
            File ordersFile = dataPath.resolve("pedidos.20250419").resolve(ordersFileName).toFile();

            if (ordersFile.exists()) {
                List<Order> orders = dataReader.loadOrders(ordersFile.getPath(), startTime, 24*7, 1000000);
                List<Event> orderEvents = new ArrayList<>();
                
                // Create ORDER_ARRIVAL events for each order
                for (Order order : orders) {
                    Event orderEvent = new Event(EventType.ORDER_ARRIVAL, order.getArriveTime(), order.getId(), order);
                    orderEvents.add(orderEvent);
                }
                
                // Add events to event list
                eventList.addAll(orderEvents);
                logger.info("Loaded " + orders.size() + " orders from " + ordersFile.getPath());
            } else {
                logger.warning("Orders file not found: " + ordersFile.getPath());
            }

            // Load blockages from current month file
            String blockagesFileName = String.format("%d%02d.bloqueos.txt", startTime.getYear(), startTime.getMonthValue());
            File blockagesFile = dataPath.resolve("bloqueos.20250419").resolve(blockagesFileName).toFile();

            if (blockagesFile.exists()) {
                List<Blockage> blockages = dataReader.loadBlockages(blockagesFile.getPath(), startTime, 24*7, 100000); // Load all blockages for the week
                List<Event> blockageEvents = new ArrayList<>();
                
                // Create BLOCKAGE_START and BLOCKAGE_END events for each blockage
                for (Blockage blockage : blockages) {
                    String blockageId = "BLK-" + blockage.getStartTime().toLocalDate() + "-" + 
                                        blockage.getStartTime().toLocalTime().getHour() + "-" +
                                        blockage.getStartTime().toLocalTime().getMinute();
                    
                    Event startEvent = new Event(EventType.BLOCKAGE_START, blockage.getStartTime(), blockageId, blockage);
                    Event endEvent = new Event(EventType.BLOCKAGE_END, blockage.getEndTime(), blockageId, null);
                    
                    blockageEvents.add(startEvent);
                    blockageEvents.add(endEvent);
                }
                
                // Add events to event list
                eventList.addAll(blockageEvents);
                environment.addBlockages(blockages);
                logger.info("Loaded " + blockages.size() + " blockages from " + blockagesFile.getPath());
            } else {
                logger.warning("Blockages file not found: " + blockagesFile.getPath());
            }

            // Load maintenance tasks
            File maintenanceFile = dataPath.resolve("mantpreventivo.txt").toFile();
            if (maintenanceFile.exists()) {
                List<MaintenanceTask> tasks = dataReader.loadMaintenanceSchedule(maintenanceFile.getPath(), startTime, 30, 10);
                List<Event> maintenanceEvents = new ArrayList<>();
                
                // Create MAINTENANCE_START and MAINTENANCE_END events for each task
                for (MaintenanceTask task : tasks) {
                    // Using vehicleId directly as the entity ID for maintenance events
                    Event startEvent = new Event(EventType.MAINTENANCE_START, task.getStartTime(), task.getVehicleId(), task);
                    Event endEvent = new Event(EventType.MAINTENANCE_END, task.getEndTime(), task.getVehicleId(), null);
                    
                    maintenanceEvents.add(startEvent);
                    maintenanceEvents.add(endEvent);
                }
                
                // Add events to event list
                eventList.addAll(maintenanceEvents);
                logger.info("Loaded " + tasks.size() + " maintenance tasks from " + maintenanceFile.getPath());
            } else {
                logger.warning("Maintenance file not found: " + maintenanceFile.getPath());
            }
            
            // Add simulation end event
            Event endEvent = new Event(EventType.SIMULATION_END, startTime.plusDays(7), null, null);
            eventList.add(endEvent);
            
        } catch (Exception e) {
            logger.severe("Error loading data files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void start() throws Exception {
        // Start API server
        apiServer.start();
        
        // Start a background thread to periodically update the simulation
        startSimulationLoop();
        
        logger.info("V-Route API service started successfully!");
        logger.info("Access the API at: http://localhost:" + DEFAULT_PORT);
        logger.info("API documentation: http://localhost:" + DEFAULT_PORT + "/");
        logger.info("System status: http://localhost:" + DEFAULT_PORT + "/api/status");
        logger.info("Environment snapshot: http://localhost:" + DEFAULT_PORT + "/api/environment");
    }
    
    private void startSimulationLoop() {
        simulationRunning.set(true);
        orchestrator.prepareSimulation();
        
        simulationThread = new Thread(() -> {
            try {
                logger.info("Starting simulation loop with speed: " + simulationSpeed + "ms between ticks");
                while (simulationRunning.get()) {
                    // Run a simulation step
                    boolean continueSimulation = orchestrator.advanceTick();
                    
                    if (!continueSimulation) {
                        logger.info("Simulation completed");
                        simulationRunning.set(false);
                        break;
                    }
                    
                    // Sleep for the simulation speed interval
                    Thread.sleep(simulationSpeed);
                }
            } catch (InterruptedException e) {
                logger.info("Simulation loop interrupted");
            } catch (Exception e) {
                logger.severe("Error in simulation loop: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        simulationThread.setDaemon(true);
        simulationThread.start();
    }
    
    public void stop() {
        // Stop simulation thread
        simulationRunning.set(false);
        if (simulationThread != null && simulationThread.isAlive()) {
            simulationThread.interrupt();
            try {
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                logger.warning("Interrupted while waiting for simulation thread to stop");
            }
        }
        
        // Stop API server
        if (apiServer != null) {
            apiServer.stop();
        }
        
        logger.info("V-Route API service stopped");
    }
    
    public void setSimulationSpeed(int milliseconds) {
        if (milliseconds < 50) {
            milliseconds = 50; // Minimum 50ms to prevent excessive CPU usage
        } else if (milliseconds > 5000) {
            milliseconds = 5000; // Maximum 5 seconds
        }
        
        this.simulationSpeed = milliseconds;
        logger.info("Simulation speed set to: " + milliseconds + "ms between ticks");
        
        // Restart the simulation thread if it's already running
        if (simulationRunning.get() && simulationThread != null && simulationThread.isAlive()) {
            simulationRunning.set(false);
            try {
                simulationThread.join(1000);
            } catch (InterruptedException e) {
                logger.warning("Interrupted while waiting for simulation thread to stop");
            }
            startSimulationLoop();
        }
    }
    
    public void pauseSimulation() {
        simulationRunning.set(false);
        logger.info("Simulation paused");
    }
    
    public void resumeSimulation() {
        if (!simulationRunning.get()) {
            startSimulationLoop();
            logger.info("Simulation resumed");
        }
    }
    
    public boolean isSimulationRunning() {
        return simulationRunning.get();
    }
    
    /**
     * Gets the current simulation speed in milliseconds between ticks
     * @return The simulation speed
     */
    public int getSimulationSpeed() {
        return simulationSpeed;
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
