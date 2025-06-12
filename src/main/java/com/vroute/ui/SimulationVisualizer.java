package com.vroute.ui;

import com.vroute.models.*;
import com.vroute.operation.VehiclePlan;
import com.vroute.orchest.AlgorithmConfig;
import com.vroute.orchest.DataReader;
import com.vroute.orchest.Event;
import com.vroute.orchest.Orchestrator;
import com.vroute.orchest.SimulationStats;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * SimulationVisualizer integrates the Orchestrator with JavaFX UI to display
 * the simulation state in real-time.
 */
public class SimulationVisualizer {
    private static final Logger logger = Logger.getLogger(SimulationVisualizer.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    // Core components
    private final Orchestrator orchestrator;
    private final Environment environment;
    private final Pane mapPane;
    private final VBox infoBox;
    private final int cellSize;

    // Controls
    private boolean simulationRunning = false;
    private boolean simulationPaused = false;
    private double simulationSpeed = 1.0; // Default speed (1x)
    private ScheduledExecutorService simulationExecutor;
    private AnimationTimer animationTimer;

    // UI state
    private final StringProperty currentTimeProperty = new SimpleStringProperty();
    private final StringProperty statusProperty = new SimpleStringProperty("Ready");
    private final DoubleProperty simulationProgressProperty = new SimpleDoubleProperty(0);
    private final BooleanProperty simulationActiveProperty = new SimpleBooleanProperty(false);

    // Data loading
    private final DataReader dataReader = new DataReader();

    public SimulationVisualizer(Environment environment, Pane mapPane, VBox infoBox, int cellSize) {
        this.environment = environment;
        this.mapPane = mapPane;
        this.infoBox = infoBox;
        this.cellSize = cellSize;
        this.orchestrator = new Orchestrator(environment);

        // Configure orchestrator with default settings
        AlgorithmConfig config = AlgorithmConfig.createDefault();
        config.setSimulationStepMinutes(5); // Update visualization every 5 simulation minutes
        orchestrator.setConfig(config);

        // Initialize statistics to avoid null values
        initializeStats();

        // Initialize the animation timer to update the UI
        this.animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateVisualization();
            }
        };

        // Set initial time display
        currentTimeProperty.set(environment.getCurrentTime().format(DATE_FORMAT));
    }

    /**
     * Initialize the simulation statistics to ensure we don't get null values
     */
    private void initializeStats() {
        // Make sure the stats object is initialized with the current time
        if (orchestrator != null && environment != null) {
            SimulationStats stats = orchestrator.getStats();
            if (stats != null) {
                stats.resetStats();  // First reset to ensure clean state
                stats.startSimulation(environment.getCurrentTime());
            }
        }
    }

    /**
     * Initialize the simulation with data from files
     * 
     * @param ordersFilePath      Path to orders file
     * @param blockagesFilePath   Path to blockages file
     * @param maintenanceFilePath Path to maintenance file
     */
    public void initializeWithData(String ordersFilePath, String blockagesFilePath, String maintenanceFilePath) {
        LocalDateTime startTime = environment.getCurrentTime();

        // Load orders from file
        if (ordersFilePath != null) {
            List<Order> orders = dataReader.loadOrders(ordersFilePath, startTime, 48, 0);
            for (Order order : orders) {
                addOrderAndScheduleEvent(order);
            }
            logger.info("Loaded " + orders.size() + " orders");
        }

        // Load blockages from file
        if (blockagesFilePath != null) {
            List<Blockage> blockages = dataReader.loadBlockages(blockagesFilePath, startTime, 48, 0);
            for (Blockage blockage : blockages) {
                environment.addBlockage(blockage);
            }
            logger.info("Loaded " + blockages.size() + " blockages");
        }

        // Load maintenance schedule from file
        if (maintenanceFilePath != null) {
            List<MaintenanceTask> tasks = dataReader.loadMaintenanceSchedule(
                    maintenanceFilePath, startTime, 30, 0);
            for (MaintenanceTask task : tasks) {
                environment.addMaintenanceTask(task);
            }
            logger.info("Loaded " + tasks.size() + " maintenance tasks");
        }

        // Initialize the orchestrator
        orchestrator.initialize();

        // Initialize statistics
        initializeStats();

        // Draw the initial state
        updateVisualization();
    }

    /**
     * Start the simulation
     */
    public void startSimulation() {
        if (simulationRunning)
            return;

        // Make sure statistics are initialized before starting
        initializeStats();
        
        simulationRunning = true;
        simulationPaused = false;
        simulationActiveProperty.set(true);

        // Start the animation timer
        animationTimer.start();

        // Create a scheduled executor to run simulation steps at regular intervals
        simulationExecutor = Executors.newSingleThreadScheduledExecutor();
        long stepDelayMs = calculateStepDelay();

        simulationExecutor.scheduleAtFixedRate(() -> {
            if (!simulationPaused) {
                CompletableFuture.runAsync(() -> {
                    // Run a single step of the simulation
                    boolean stillRunning = orchestrator.runSimulationStep();

                    if (!stillRunning) {
                        stopSimulation();
                    }
                });
            }
        }, 0, stepDelayMs, TimeUnit.MILLISECONDS);

        statusProperty.set("Simulation running");
        logger.info("Simulation started");
    }

    /**
     * Pause the simulation
     */
    public void pauseSimulation() {
        if (!simulationRunning || simulationPaused)
            return;

        simulationPaused = true;
        statusProperty.set("Simulation paused");
        logger.info("Simulation paused at " + environment.getCurrentTime().format(DATE_FORMAT));
    }

    /**
     * Resume the simulation after pause
     */
    public void resumeSimulation() {
        if (!simulationRunning || !simulationPaused)
            return;

        simulationPaused = false;
        statusProperty.set("Simulation running");
        logger.info("Simulation resumed");
    }

    /**
     * Stop the simulation completely
     */
    public void stopSimulation() {
        if (!simulationRunning)
            return;

        // Stop the animation timer and executor
        animationTimer.stop();
        simulationExecutor.shutdown();

        simulationRunning = false;
        simulationPaused = false;
        simulationActiveProperty.set(false);
        statusProperty.set("Simulation stopped");
        logger.info("Simulation stopped at " + environment.getCurrentTime().format(DATE_FORMAT));

        // Get and display final statistics
        SimulationStats stats = orchestrator.getStats();
        updateStatistics(stats);
    }

    /**
     * Set the simulation speed
     * 
     * @param speed Speed multiplier (0.5 = half speed, 2.0 = double speed, etc.)
     */
    public void setSimulationSpeed(double speed) {
        if (speed <= 0)
            return;

        if (simulationRunning) {
            // Need to restart the executor with new timing
            simulationExecutor.shutdown();
            long stepDelayMs = calculateStepDelay();

            simulationExecutor = Executors.newSingleThreadScheduledExecutor();
            simulationExecutor.scheduleAtFixedRate(() -> {
                if (!simulationPaused) {
                    CompletableFuture.runAsync(() -> {
                        boolean stillRunning = orchestrator.runSimulationStep();
                        if (!stillRunning) {
                            stopSimulation();
                        }
                    });
                }
            }, 0, stepDelayMs, TimeUnit.MILLISECONDS);
        }

        simulationSpeed = speed;
        logger.info("Simulation speed set to " + speed + "x");
    }

    /**
     * Calculate the delay between simulation steps based on the speed
     * 
     * @return Delay in milliseconds
     */
    private long calculateStepDelay() {
        // Base delay = 1000ms (1 second) per step at 1x speed
        return (long) (1000 / simulationSpeed);
    }

    /**
     * Update the visualization with the current state of the simulation
     * This method is public so it can be called from outside the class
     */
    public void updateVisualization() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::updateVisualization);
            return;
        }

        // Clear the map
        mapPane.getChildren().clear();

        // Draw the grid
        MapRenderer.drawGrid(mapPane, environment.getGrid().getWidth(),
                environment.getGrid().getHeight(), cellSize);

        // Draw depots
        MapRenderer.drawDepots(mapPane, environment.getAuxDepots(),
                environment.getMainDepot(), cellSize);

        // Draw blockages for current time
        MapRenderer.drawBlockages(mapPane, environment,
                environment.getCurrentTime(), cellSize);

        // Draw orders
        MapRenderer.drawOrders(mapPane, environment.getPendingOrders(), cellSize);

        // Draw vehicles
        MapRenderer.drawVehicles(mapPane, environment.getAvailableVehicles(), cellSize);

        // Draw vehicle plans
        Map<Vehicle, VehiclePlan> plans = orchestrator.getVehiclePlans();
        for (VehiclePlan plan : plans.values()) {
            MapRenderer.drawVehiclePlan(mapPane, plan, cellSize);
        }

        // Update time display
        currentTimeProperty.set(environment.getCurrentTime().format(DATE_FORMAT));

        // Update info box with simulation statistics
        updateStatistics(orchestrator.getStats());

        // Update progress bar if applicable
        if (orchestrator.getConfig() != null) {
            int maxDays = orchestrator.getConfig().getSimulationMaxDays();
            LocalDateTime startTime = orchestrator.getStats().getSimulationStartTime();
            LocalDateTime currentTime = environment.getCurrentTime();

            // Make sure startTime is not null before calling plusDays
            if (startTime != null && currentTime != null) {
                LocalDateTime endTime = startTime.plusDays(maxDays);

                double totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
                double elapsedMinutes = java.time.Duration.between(startTime, currentTime).toMinutes();

                if (totalMinutes > 0) {
                    simulationProgressProperty.set(elapsedMinutes / totalMinutes);
                }
            }
        }
    }

    /**
     * Update the statistics display
     */
    private void updateStatistics(SimulationStats stats) {
        if (stats == null || infoBox == null)
            return;

        // Clear previous info
        infoBox.getChildren().clear();

        // Add simulation time
        infoBox.getChildren().add(new Label("Simulation Time: " + environment.getCurrentTime().format(DATE_FORMAT)));

        // Add basic statistics
        infoBox.getChildren().add(new Label(""));
        infoBox.getChildren().add(new Label("Statistics:"));
        infoBox.getChildren().add(new Label("Orders: " + stats.getTotalOrders() + " received, " +
                stats.getDeliveredOrders() + " delivered"));
        infoBox.getChildren().add(new Label("Late Deliveries: " + stats.getLateDeliveries()));
        infoBox.getChildren().add(new Label("Distance Traveled: " +
                String.format("%.2f", stats.getTotalDistanceTraveled()) + " km"));
        infoBox.getChildren().add(new Label("Fuel Consumed: " +
                String.format("%.2f", stats.getTotalFuelConsumed()) + " gal"));
        infoBox.getChildren().add(new Label("Vehicle Breakdowns: " + stats.getTotalVehicleBreakdowns()));
        infoBox.getChildren().add(new Label("Maintenance Events: " + stats.getTotalMaintenanceEvents()));

        // Add next events
        infoBox.getChildren().add(new Label(""));
        infoBox.getChildren().add(new Label("Next Events:"));
        List<Event> nextEvents = new ArrayList<>();
        Event nextEvent = orchestrator.peekNextEvent();
        int count = 0;
        while (nextEvent != null && count < 5) {
            nextEvents.add(nextEvent);
            count++;
            // We can't get the actual next-next event without modifying Orchestrator,
            // so we'll just show the next available one for now
            break;
        }

        if (nextEvents.isEmpty()) {
            infoBox.getChildren().add(new Label("No scheduled events"));
        } else {
            for (Event event : nextEvents) {
                String eventDetails = event.getType() + " at " + event.getTime().format(DATE_FORMAT);
                if (event.getEntityId() != null) {
                    eventDetails += " (" + event.getEntityId() + ")";
                }
                infoBox.getChildren().add(new Label(eventDetails));
            }
        }
    }

    /**
     * Add an order to the environment and schedule the corresponding event
     * 
     * @param order The order to add
     */
    public void addOrderAndScheduleEvent(Order order) {
        environment.addOrder(order);
        orchestrator.addEvent(new Event(Event.EventType.ORDER_ARRIVAL, order.getArriveDate(), order.getId(), order));
    }

    // Getters for properties
    public StringProperty currentTimeProperty() {
        return currentTimeProperty;
    }

    public StringProperty statusProperty() {
        return statusProperty;
    }

    public DoubleProperty simulationProgressProperty() {
        return simulationProgressProperty;
    }

    public BooleanProperty simulationActiveProperty() {
        return simulationActiveProperty;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    public boolean isSimulationPaused() {
        return simulationPaused;
    }
}
