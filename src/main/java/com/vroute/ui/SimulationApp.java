package com.vroute.ui;

import com.vroute.models.*;
import com.vroute.pathfinding.Grid;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SimulationApp extends Application {
    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    private static final int CELL_SIZE = 20;

    private SimulationVisualizer visualizer;
    private Environment environment;
    private Pane mapPane;
    private VBox infoPanel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize the environment
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        initializeSimulationEnvironment(startTime);

        // Create UI layout
        BorderPane root = new BorderPane();

        // Map area (central region)
        mapPane = new Pane();
        mapPane.setPrefSize(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        ScrollPane mapScrollPane = new ScrollPane(mapPane);
        mapScrollPane.setPrefViewportHeight(720);
        mapScrollPane.setPrefViewportWidth(1280);
        mapScrollPane.setFitToHeight(true);
        mapScrollPane.setFitToWidth(true);

        // Info panel (right side)
        infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setPrefWidth(350);
        infoPanel.setMinWidth(300);
        ScrollPane infoScrollPane = new ScrollPane(infoPanel);
        infoScrollPane.setFitToWidth(true);
        infoScrollPane.setMinWidth(300);

        // Initialize the visualizer
        this.visualizer = new SimulationVisualizer(environment, mapPane, infoPanel, CELL_SIZE);

        // Control panel (bottom)
        HBox controlPanel = createControlPanel();
        // Add components to the layout
        root.setCenter(mapScrollPane);
        root.setRight(infoScrollPane);
        root.setBottom(controlPanel);

        // Create the scene
        Scene scene = new Scene(root, 1600, 900);
        primaryStage.setTitle("V-Route Simulation Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Display the initial environment state
        this.visualizer.updateVisualization();
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        controlPanel.setStyle("-fx-background-color: #f0f0f0;");

        Button loadDataButton = new Button("Load Data");
        loadDataButton.setOnAction(e -> loadSimulationData());

        Button startButton = new Button("Start Simulation");
        startButton.setOnAction(e -> visualizer.startSimulation());
        startButton.disableProperty().bind(visualizer.simulationActiveProperty());

        Button pauseResumeButton = new Button("Pause");
        pauseResumeButton.setOnAction(e -> {
            if (visualizer.isSimulationPaused()) {
                visualizer.resumeSimulation();
                pauseResumeButton.setText("Pause");
            } else {
                visualizer.pauseSimulation();
                pauseResumeButton.setText("Resume");
            }
        });
        pauseResumeButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> !visualizer.isSimulationRunning(),
                        visualizer.simulationActiveProperty()));

        Button stopButton = new Button("Stop Simulation");
        stopButton.setOnAction(e -> visualizer.stopSimulation());
        stopButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> !visualizer.isSimulationRunning(),
                        visualizer.simulationActiveProperty()));

        Slider speedSlider = new Slider(0.25, 10, 1.0);
        speedSlider.setMajorTickUnit(1.0);
        speedSlider.setMinorTickCount(3);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setPrefWidth(150);
        speedSlider.valueProperty()
                .addListener((obs, oldVal, newVal) -> visualizer.setSimulationSpeed(newVal.doubleValue()));

        Label speedLabel = new Label("Simulation Speed:");

        Label timeLabel = new Label("Current Time:");
        Label timeDisplay = new Label();
        timeDisplay.textProperty().bind(visualizer.currentTimeProperty());
        timeDisplay.setStyle("-fx-font-weight: bold;");

        Label statusLabel = new Label("Status:");
        Label statusDisplay = new Label();
        statusDisplay.textProperty().bind(visualizer.statusProperty());

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.progressProperty().bind(visualizer.simulationProgressProperty());

        VBox timeStatusBox = new VBox(5,
                new HBox(5, timeLabel, timeDisplay),
                new HBox(5, statusLabel, statusDisplay),
                progressBar);

        VBox speedControlBox = new VBox(5,
                speedLabel,
                speedSlider);

        controlPanel.getChildren().addAll(
                loadDataButton,
                startButton,
                pauseResumeButton,
                stopButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                speedControlBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                timeStatusBox);

        return controlPanel;
    }

    /**
     * Load simulation data from files
     */
    private void loadSimulationData() {
        // In a real application, we would show a file chooser dialog here
        // For now, we'll use hardcoded file paths

        LocalDateTime startDate = environment.getCurrentTime();
        String month = String.format("%02d", startDate.getMonthValue());
        String year = String.format("%04d", startDate.getYear());

        // Configure paths
        String ordersFilePath = "data/pedidos.20250419/ventas" + year + month + ".txt";
        String blockagesFilePath = "data/bloqueos.20250419/" + year + month + ".bloqueos.txt";
        String maintenanceFilePath = "data/mantpreventivo.txt";

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Load Simulation Data");
        confirmDialog.setHeaderText("Load data from files?");
        confirmDialog.setContentText(
                "Orders: " + ordersFilePath + "\n" +
                        "Blockages: " + blockagesFilePath + "\n" +
                        "Maintenance: " + maintenanceFilePath);

        confirmDialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                visualizer.initializeWithData(ordersFilePath, blockagesFilePath, maintenanceFilePath);
            }
        });
    }

    /**
     * Initialize the simulation environment
     */
    private void initializeSimulationEnvironment(LocalDateTime startTime) {
        Grid grid = new Grid(Constants.CITY_LENGTH_X, Constants.CITY_WIDTH_Y);

        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(new Vehicle("TA01", VehicleType.TA, new Position(12, 8)));
        vehicles.add(new Vehicle("TA02", VehicleType.TA, new Position(12, 8)));
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle(String.format("TB%02d", i), VehicleType.TB, new Position(12, 8)));
        }
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle(String.format("TC%02d", i), VehicleType.TC, new Position(12, 8)));
        }
        for (int i = 1; i <= 10; i++) {
            vehicles.add(new Vehicle(String.format("TD%02d", i), VehicleType.TD, new Position(12, 8)));
        }

        Depot mainDepot = new Depot("MAIN", new Position(12, 8), 500000, true);
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("NORTH", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 250, true));
        auxDepots.add(new Depot("EAST", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 250, true));
        this.environment = new Environment(grid, vehicles, mainDepot, auxDepots, startTime);
    }

    @Override
    public void stop() {
        if (visualizer != null && visualizer.isSimulationRunning()) {
            visualizer.stopSimulation();
        }
    }
}
