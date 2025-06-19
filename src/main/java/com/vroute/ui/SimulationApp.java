package com.vroute.ui;

import com.vroute.models.*;
import com.vroute.operation.VehiclePlan;
import com.vroute.orchest.DataReader;
import com.vroute.orchest.Event;
import com.vroute.orchest.EventType;
import com.vroute.orchest.Orchestrator;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SimulationApp extends Application {
    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    private static final int CELL_SIZE = 20;
    private static final Logger logger = Logger.getLogger(SimulationApp.class.getName());

    // Simulation components
    private Environment environment;
    private Orchestrator orchestrator;
    private DataReader dataReader;

    // UI components
    private Pane mapPane;
    private VBox statusBox;
    private VBox planDetailsBox;
    private Label simulationTimeLabel;
    private Label statusLabel;
    private ProgressBar simulationProgress;
    private Timeline simulationTimeline;
    private StringProperty currentTimeProperty = new SimpleStringProperty();

    // Simulation speed (milliseconds between steps)
    private int simulationSpeed = 500;

    // event list
    private List<Event> eventList = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        // Initialize the data reader
        dataReader = new DataReader();

        // Initialize simulation environment with starting date
        LocalDateTime simulationStartTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        initializeEnvironment(simulationStartTime);
        // Load data from files (after UI is initialized)
        loadDataFromFiles(simulationStartTime);

        // Initialize the orchestrator
        orchestrator = new Orchestrator(environment);
        orchestrator.addEvents(eventList);
        // Create UI first
        BorderPane root = createUI();

        // Configure and show the stage
        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("V-Route Delivery Planning Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Update time display
        updateTimeDisplay();

        // Initial draw of the environment
        drawEnvironment();
    }

    private BorderPane createUI() {
        BorderPane root = new BorderPane();

        // Map area (center)
        mapPane = new Pane();
        mapPane.setPrefSize(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        ScrollPane mapScrollPane = new ScrollPane(mapPane);
        mapScrollPane.setPrefViewportHeight(720);
        mapScrollPane.setPrefViewportWidth(1080);
        root.setCenter(mapScrollPane);

        // Status and controls (top)
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        simulationTimeLabel = new Label();
        simulationTimeLabel.textProperty().bind(currentTimeProperty);
        simulationTimeLabel.setStyle("-fx-font-weight: bold;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: blue;");

        simulationProgress = new ProgressBar(0);
        simulationProgress.setPrefWidth(200);

        topBar.getChildren().addAll(new Label("Simulation Time:"), simulationTimeLabel,
                new Label("Status:"), statusLabel, simulationProgress);
        root.setTop(topBar);

        // Sidebar for plan details and stats (right)
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(300);

        // Status box
        statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");
        Label statusHeader = new Label("Simulation Status");
        statusHeader.setStyle("-fx-font-weight: bold;");
        statusBox.getChildren().add(statusHeader);

        // Plan details box
        planDetailsBox = new VBox(5);
        planDetailsBox.setPadding(new Insets(10));
        planDetailsBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");
        Label plansHeader = new Label("Vehicle Plans");
        plansHeader.setStyle("-fx-font-weight: bold;");
        planDetailsBox.getChildren().add(plansHeader);

        ScrollPane planDetailsScrollPane = new ScrollPane(planDetailsBox);
        planDetailsScrollPane.setFitToWidth(true);

        rightPanel.getChildren().addAll(statusBox, planDetailsScrollPane);
        root.setRight(rightPanel);

        // Controls (bottom)
        HBox controlsBox = new HBox(10);
        controlsBox.setPadding(new Insets(10));
        controlsBox.setAlignment(Pos.CENTER);

        Button startButton = new Button("Start Simulation");
        startButton.setOnAction(e -> startSimulation());

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> pauseSimulation());

        Button stopButton = new Button("Stop");
        stopButton.setOnAction(e -> stopSimulation());

        Slider speedSlider = new Slider(0.5, 10.0, 0.5);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simulationSpeed = (int) (500 / newVal.doubleValue());
            if (simulationTimeline != null && simulationTimeline.getStatus() == Animation.Status.RUNNING) {
                pauseSimulation();
                startSimulation();
            }
        });

        controlsBox.getChildren().addAll(
                new Label("Speed:"), speedSlider,
                startButton, pauseButton, stopButton);
        root.setBottom(controlsBox);

        return root;
    }

    private void initializeEnvironment(LocalDateTime startTime) {
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 10000, true);
        List<Depot> auxDepots = createAuxiliaryDepots();
        List<Vehicle> vehicles = createVehicleFleet(mainDepot.getPosition());

        this.environment = new Environment(vehicles, mainDepot, auxDepots, startTime);
    }

    private void loadDataFromFiles(LocalDateTime startTime) {
        try {
            // Base path for data files
            Path dataPath = Paths.get("/home/nhrot/Programming/DP1/V-Route/data");

            // Load orders from current month file (assuming January 2025)
            String ordersFileName = String.format("ventas%d%02d.txt", startTime.getYear(), startTime.getMonthValue());
            File ordersFile = dataPath.resolve("pedidos.20250419").resolve(ordersFileName).toFile();

            if (ordersFile.exists()) {
                List<Order> orders = dataReader.loadOrders(ordersFile.getPath(), startTime, 48, 0);
                List<Event> orderEvents = new ArrayList<>();
                
                // Create ORDER_ARRIVAL events for each order
                for (Order order : orders) {
                    Event orderEvent = new Event(EventType.ORDER_ARRIVAL, order.getArriveTime(), order.getId(), order);
                    orderEvents.add(orderEvent);
                }
                
                // Add events to orchestrator
                eventList.addAll(orderEvents);
                updateStatus("Created " + orders.size() + " order arrival events");
                logger.info("Loaded " + orders.size() + " orders from " + ordersFile.getPath());
            } else {
                logger.warning("Orders file not found: " + ordersFile.getPath());
            }

            // Load blockages from current month file
            String blockagesFileName = String.format("%d%02d.bloqueos.txt", startTime.getYear(),
                    startTime.getMonthValue());
            File blockagesFile = dataPath.resolve("bloqueos.20250419").resolve(blockagesFileName).toFile();

            if (blockagesFile.exists()) {
                List<Blockage> blockages = dataReader.loadBlockages(blockagesFile.getPath(), startTime, 48, 0);
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
                
                // Add events to orchestrator
                eventList.addAll(blockageEvents);
                environment.addBlockages(blockages);
                updateStatus("Created " + (blockages.size() * 2) + " blockage events for " + blockages.size() + " blockages");
                logger.info("Loaded " + blockages.size() + " blockages from " + blockagesFile.getPath());
            } else {
                logger.warning("Blockages file not found: " + blockagesFile.getPath());
            }

            // Load maintenance tasks
            File maintenanceFile = dataPath.resolve("mantpreventivo.txt").toFile();
            if (maintenanceFile.exists()) {
                List<MaintenanceTask> tasks = dataReader.loadMaintenanceSchedule(maintenanceFile.getPath(), startTime,
                        30, 0);
                List<Event> maintenanceEvents = new ArrayList<>();
                
                // Create MAINTENANCE_START and MAINTENANCE_END events for each task
                for (MaintenanceTask task : tasks) {
                    // Using vehicleId directly as the entity ID for maintenance events
                    
                    Event startEvent = new Event(EventType.MAINTENANCE_START, task.getStartTime(), task.getVehicleId(), task);
                    Event endEvent = new Event(EventType.MAINTENANCE_END, task.getEndTime(), task.getVehicleId(), null);
                    
                    maintenanceEvents.add(startEvent);
                    maintenanceEvents.add(endEvent);
                }
                
                // Add events to orchestrator
                eventList.addAll(maintenanceEvents);
                updateStatus("Created " + (tasks.size() * 2) + " maintenance events for " + tasks.size() + " tasks");
                logger.info("Loaded " + tasks.size() + " maintenance tasks from " + maintenanceFile.getPath());
            } else {
                logger.warning("Maintenance file not found: " + maintenanceFile.getPath());
            }
        } catch (Exception e) {
            logger.severe("Error loading data files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Vehicle> createVehicleFleet(Position startPosition) {
        List<Vehicle> vehicles = new ArrayList<>();

        for (int i = 1; i <= 2; i++) {
            vehicles.add(new Vehicle("TA" + i, VehicleType.TA, startPosition.clone()));
        }

        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle("TB" + i, VehicleType.TB, startPosition.clone()));
        }

        for (int i = 1; i <= 3; i++) {
            vehicles.add(new Vehicle("TC" + i, VehicleType.TC, startPosition.clone()));
        }
        // TD: 10 units
        for (int i = 1; i <= 10; i++) {
            vehicles.add(new Vehicle(String.format("TD%02d", i), VehicleType.TD, startPosition.clone()));
        }

        logger.info("Created " + vehicles.size() + " vehicles");
        return vehicles;
    }

    private List<Depot> createAuxiliaryDepots() {
        List<Depot> auxDepots = new ArrayList<>();

        // Add fuel-only depot in the north
        auxDepots.add(new Depot("NORTH", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 250, false));
        auxDepots.add(new Depot("EAST", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 250, false));

        logger.info("Created " + auxDepots.size() + " auxiliary depots");
        return auxDepots;
    }

    private void startSimulation() {
        if (simulationTimeline != null && simulationTimeline.getStatus() == Animation.Status.RUNNING) {
            return; // Simulation already running
        }

        // Initialize the orchestrator if needed
        if (orchestrator.getVehiclePlans().isEmpty()) {
            updateStatus("Running initial planning...");
            try {
                orchestrator.initialize(); // Make sure the orchestrator is initialized
            } catch (Exception e) {
                updateStatus("Error during planning: " + e.getMessage());
                logger.severe("Planning error: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        updateStatus("Simulation running");

        // Create a timeline for simulation steps
        simulationTimeline = new Timeline(new KeyFrame(Duration.millis(simulationSpeed), e -> {
            try {
                updateSimulationStep();
            } catch (Exception ex) {
                updateStatus("Error during simulation: " + ex.getMessage());
                logger.severe("Simulation error: " + ex.getMessage());
                ex.printStackTrace();
                pauseSimulation();
            }
        }));
        simulationTimeline.setCycleCount(Animation.INDEFINITE);
        simulationTimeline.play();
    }

    private void pauseSimulation() {
        if (simulationTimeline != null && simulationTimeline.getStatus() == Animation.Status.RUNNING) {
            simulationTimeline.pause();
            updateStatus("Simulation paused");
        }
    }

    private void stopSimulation() {
        if (simulationTimeline != null) {
            simulationTimeline.stop();
        }
        updateStatus("Simulation stopped");
    }

    private void updateSimulationStep() {
        // Run a single step of the simulation
        boolean simulationContinues = orchestrator.runSimulationStep();

        // Update UI
        updateTimeDisplay();
        drawEnvironment();
        updateStats();
        
        // If simulation has reached its end, stop it
        if (!simulationContinues) {
            stopSimulation();
            updateStatus("Simulation completed - reached maximum duration");
        }
    }

    private void updateTimeDisplay() {
        LocalDateTime simulationTime = environment.getCurrentTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        currentTimeProperty.set(simulationTime.format(formatter));
    }

    private void updateStatus(String status) {
        // Check if UI is already initialized
        if (statusLabel != null) {
            statusLabel.setText(status);

            // Add to status history if the box is initialized
            if (statusBox != null) {
                Label statusHistoryLabel = new Label(
                        environment.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + status);
                if (statusBox.getChildren().size() > 20) {
                    statusBox.getChildren().remove(1); // Keep the header
                }
                statusBox.getChildren().add(statusHistoryLabel);
            }
        }

        // Always log the status
        logger.info(status);
    }

    private void updateStats() {
        // Update simulation progress - assume we're showing a day's worth of progress
        double progressValue = (environment.getCurrentTime().getHour() * 60 + environment.getCurrentTime().getMinute())
                / (24.0 * 60);
        simulationProgress.setProgress(Math.min(1.0, progressValue));
        
        // Update plan details box with current order and blockage statistics
        if (planDetailsBox != null) {
            planDetailsBox.getChildren().clear();
            
            Label plansHeader = new Label("Simulation Statistics");
            plansHeader.setStyle("-fx-font-weight: bold;");
            planDetailsBox.getChildren().add(plansHeader);
            
            // Order stats
            int totalOrders = environment.getOrderQueue().size();
            int pendingOrders = environment.getPendingOrders().size();
            int deliveredOrders = totalOrders - pendingOrders;
            int overdueOrders = environment.getOverdueOrders().size();
            
            planDetailsBox.getChildren().add(new Label(String.format("Orders: %d total, %d delivered, %d pending, %d overdue",
                    totalOrders, deliveredOrders, pendingOrders, overdueOrders)));
            
            // Blockage stats
            int activeBlockages = environment.getActiveBlockagesAt(environment.getCurrentTime()).size();
            planDetailsBox.getChildren().add(new Label(String.format("Active blockages: %d", activeBlockages)));
            
            // Vehicle stats
            int availableVehicles = environment.getAvailableVehicles().size();
            int totalVehicles = environment.getVehicles().size();
            planDetailsBox.getChildren().add(new Label(String.format("Vehicles: %d/%d available", 
                    availableVehicles, totalVehicles)));
            
            // Add each vehicle plan summary
            planDetailsBox.getChildren().add(new Label("Vehicle Plans:"));
            for (Map.Entry<Vehicle, VehiclePlan> entry : orchestrator.getVehiclePlans().entrySet()) {
                Vehicle vehicle = entry.getKey();
                VehiclePlan plan = entry.getValue();
                
                String status = vehicle.getStatus().toString();
                String planSummary = plan != null ? 
                        String.format("%d actions, %d orders", plan.getActions().size(), plan.getOrderCount()) : "No plan";
                
                planDetailsBox.getChildren().add(new Label(String.format("  %s (%s): %s", 
                        vehicle.getId(), status, planSummary)));
            }
        }
    }

    private void drawEnvironment() {
        mapPane.getChildren().clear();

        // Draw the grid
        MapRenderer.drawGrid(mapPane, GRID_WIDTH, GRID_HEIGHT, CELL_SIZE);

        // Draw depots
        MapRenderer.drawDepots(mapPane, environment.getAuxDepots(), environment.getMainDepot(), CELL_SIZE);

        // Draw orders
        MapRenderer.drawOrders(mapPane, environment.getPendingOrders(), CELL_SIZE);

        // Draw blockages
        MapRenderer.drawBlockages(mapPane, environment, environment.getCurrentTime(), CELL_SIZE);

        // Draw vehicles
        MapRenderer.drawVehicles(mapPane, environment.getAvailableVehicles(), CELL_SIZE);

        // Draw vehicle plans if available
        for (VehiclePlan plan : orchestrator.getVehiclePlans().values()) {
            MapRenderer.drawCurrentVehiclePath(mapPane, plan, environment.getCurrentTime(), CELL_SIZE);
        }
    }
}
