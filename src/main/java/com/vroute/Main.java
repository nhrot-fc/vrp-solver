package com.vroute;

import com.vroute.assignation.MetaheuristicAssignator;
import com.vroute.assignation.Solution;
import com.vroute.models.*;
import com.vroute.operation.VehiclePlan;
import com.vroute.operation.VehiclePlanCreator;
import com.vroute.pathfinding.Grid;
import com.vroute.ui.MapRenderer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends Application {

    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    private static final int CELL_SIZE = 20;

    private Environment environment;
    private Map<Vehicle, VehiclePlan> vehiclePlans;
    private Pane mapPane;
    private VBox planDetailsBox;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // set up the time to 01-01-2025 00:00:00
        LocalDateTime simulationStartTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        initializeSimulationEnvironment(simulationStartTime);

        // Create UI components
        BorderPane root = new BorderPane();

        // Map area
        mapPane = new Pane();
        mapPane.setPrefSize(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE);
        ScrollPane mapScrollPane = new ScrollPane(mapPane);
        mapScrollPane.setPrefViewportHeight(720);
        mapScrollPane.setPrefViewportWidth(1080);

        // Sidebar for plan details
        planDetailsBox = new VBox(10);
        planDetailsBox.setPadding(new Insets(10));
        planDetailsBox.setPrefWidth(300);
        ScrollPane detailsScrollPane = new ScrollPane(planDetailsBox);

        // Button controls
        HBox controlsBox = new HBox(10);
        controlsBox.setPadding(new Insets(10));
        Button runButton = new Button("Run Assignation");
        runButton.setOnAction(e -> runAssignation());
        controlsBox.getChildren().add(runButton);

        // Assemble layout
        root.setCenter(mapScrollPane);
        root.setRight(detailsScrollPane);
        root.setBottom(controlsBox);

        // Initial draw of environment
        drawEnvironment();

        // Configure and show the stage
        Scene scene = new Scene(root, 1100, 700);
        primaryStage.setTitle("V-Route Delivery Planning System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void runAssignation() {
        // Clear previous plans
        vehiclePlans = new HashMap<>();
        planDetailsBox.getChildren().clear();

        MetaheuristicAssignator assignator = new MetaheuristicAssignator(environment);

        // Get solution
        Solution solution = assignator.solve(environment);
        planDetailsBox.getChildren().add(new Label("Solution Summary:"));
        planDetailsBox.getChildren().add(new Label(solution.toString()));

        // Create plans for each vehicle
        VehiclePlanCreator planCreator = new VehiclePlanCreator(environment);

        solution.getVehicleOrderAssignments().forEach((vehicle, instructions) -> {
            if (!instructions.isEmpty()) {
                VehiclePlan plan = planCreator.createPlan(vehicle, instructions, environment.getCurrentTime());
                if (plan != null) {
                    vehiclePlans.put(vehicle, plan);
                    System.out.println(plan);
                    // Add plan details to sidebar
                    Label vehicleLabel = new Label("Plan for " + vehicle.getId() + ":");
                    planDetailsBox.getChildren().addAll(
                            vehicleLabel,
                            new Label(String.format("  Distance: %.2f km", plan.getTotalDistanceKm())),
                            new Label(String.format("  GLP Delivered: %.2f m³", plan.getTotalGlpDeliveredM3())),
                            new Label(String.format("  Fuel Used: %.2f gal", plan.getTotalFuelConsumedGal())),
                            new Label("  Orders: " + plan.getServedOrders().size()));
                }
            }
        });

        // Redraw the map with the plans
        drawEnvironmentWithPlans();
    }

    private void drawEnvironment() {
        mapPane.getChildren().clear();

        // Draw the grid
        MapRenderer.drawGrid(mapPane, GRID_WIDTH, GRID_HEIGHT, CELL_SIZE);

        // Draw depots
        MapRenderer.drawDepots(mapPane, environment.getAuxDepots(), environment.getMainDepot(), CELL_SIZE);

        // Draw vehicles
        MapRenderer.drawVehicles(mapPane, environment.getAvailableVehicles(), CELL_SIZE);

        // Draw orders
        MapRenderer.drawOrders(mapPane, environment.getPendingOrders(), CELL_SIZE);

        // Draw blockages
        MapRenderer.drawBlockages(mapPane, environment, environment.getCurrentTime(), CELL_SIZE);
    }

    private void drawEnvironmentWithPlans() {
        drawEnvironment();

        // Draw each vehicle's plan
        for (VehiclePlan plan : vehiclePlans.values()) {
            MapRenderer.drawVehiclePlan(mapPane, plan, CELL_SIZE);
        }
    }

    private void initializeSimulationEnvironment(LocalDateTime simulationStartTime) {
        Grid grid = new Grid(GRID_WIDTH, GRID_HEIGHT);
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 10000, true);
        List<Depot> auxDepots = createAuxiliaryDepots();
        List<Vehicle> vehicles = createSampleVehicles(mainDepot.getPosition());

        this.environment = new Environment(grid, vehicles, mainDepot, auxDepots, simulationStartTime);
        createAndAddSampleOrders(this.environment, simulationStartTime);
        // Usar una semilla específica para la generación de bloqueos
        createAndAddRandomBlockages(this.environment, simulationStartTime);
    }

    private void createAndAddRandomBlockages(Environment environment, LocalDateTime simulationStartTime) {
        Random random = new Random(123);
        int numberOfBlockages = 20;

        for (int i = 1; i <= numberOfBlockages; i++) {
            List<Position> blockagePoints = new ArrayList<>();
            String blockageType;
            String description = ""; // Inicializar description

            int startX, startY, length;
            int maxAttempts = 10; // Evitar bucles infinitos si es difícil colocar
            boolean placed = false;

            for (int attempt = 0; attempt < maxAttempts && !placed; attempt++) {
                blockagePoints.clear();
                if (random.nextBoolean()) { // Bloqueo horizontal
                    blockageType = "Horizontal";
                    length = 3 + random.nextInt(8); // Longitud entre 3 y 10
                    startX = random.nextInt(GRID_WIDTH - length + 1); // Asegurar que startX + length no exceda
                                                                      // GRID_WIDTH
                    startY = random.nextInt(GRID_HEIGHT);
                    for (int x = 0; x < length; x++) {
                        blockagePoints.add(new Position(startX + x, startY));
                    }
                    description = String.format("Blockage %d (%s): y=%d, from x=%d to x=%d",
                            i, blockageType, startY, startX, startX + length - 1);
                } else { // Bloqueo vertical
                    blockageType = "Vertical";
                    length = 3 + random.nextInt(8); // Longitud entre 3 y 10
                    startX = random.nextInt(GRID_WIDTH);
                    startY = random.nextInt(GRID_HEIGHT - length + 1); // Asegurar que startY + length no exceda
                                                                       // GRID_HEIGHT
                    for (int y = 0; y < length; y++) {
                        blockagePoints.add(new Position(startX, startY + y));
                    }
                    description = String.format("Blockage %d (%s): x=%d, from y=%d to y=%d",
                            i, blockageType, startX, startY, startY + length - 1);
                }

                if (!blockagePoints.isEmpty()) {
                    placed = true;
                }
            }

            if (!placed) {
                System.out.println("Could not place blockage " + i + " after " + maxAttempts + " attempts.");
                continue;
            }

            LocalDateTime blockageStartTime = simulationStartTime;
            LocalDateTime blockageEndTime = blockageStartTime.plusHours(10 + random.nextInt(91));

            Blockage blockage = new Blockage(blockageStartTime, blockageEndTime, blockagePoints);
            environment.addBlockage(blockage);

            System.out.println(description);
            System.out.println("  Active from: " + blockage.getStartTime() + " to " + blockage.getEndTime());
        }
        System.out.println("=======================================\\n");
    }

    private static void createAndAddSampleOrders(Environment environment, LocalDateTime simulationStartTime) {
        List<Order> orders = new ArrayList<>();
        Random random = new Random(123);
        int maxX = GRID_WIDTH - 3;
        int maxY = GRID_HEIGHT - 3;

        for (int i = 1; i <= 10; i++) {
            String orderId = String.format("ORD-%03d", i);
            int posX = 2 + random.nextInt(maxX);
            int posY = 2 + random.nextInt(maxY);
            Position position = new Position(posX, posY);
            int dueHours = 1 + random.nextInt(24);
            int glpVolume = 1 + random.nextInt(20);

            Order order = new Order(
                    orderId,
                    simulationStartTime,
                    simulationStartTime.plusHours(dueHours),
                    glpVolume,
                    position);

            orders.add(order);
        }

        orders.sort((o1, o2) -> o1.getDueDate().compareTo(o2.getDueDate()));

        for (Order order : orders) {
            environment.addOrder(order);
        }

        System.out.println("\n=== CREATED ORDERS ===");
        System.out.println("Total orders: " + orders.size());
        System.out.println("Earliest due date: " + orders.get(0).getDueDate());
        System.out.println("Latest due date: " + orders.get(orders.size() - 1).getDueDate());
        System.out.println("=====================\n");
    }

    private static List<Vehicle> createSampleVehicles(Position startPosition) {
        List<Vehicle> vehicles = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            vehicles.add(new Vehicle("TA" + i, VehicleType.TA, startPosition.clone()));
        }

        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle("TB" + i, VehicleType.TB, startPosition.clone()));
        }

        for (int i = 1; i <= 3; i++) {
            vehicles.add(new Vehicle("TC" + i, VehicleType.TC, startPosition.clone()));
        }

        System.out.println("\n=== CREATED VEHICLES ===");
        for (Vehicle vehicle : vehicles) {
            System.out.println(vehicle);
        }
        System.out.println("=======================\n");

        return vehicles;
    }

    private static List<Depot> createAuxiliaryDepots() {
        List<Depot> auxDepots = new ArrayList<>();

        auxDepots.add(new Depot("FUEL-N", new Position(15, 3), 0, true));
        auxDepots.add(new Depot("DEPOT-E", new Position(25, 10), 200, true));
        auxDepots.add(new Depot("DEPOT-S", new Position(15, 17), 300, true));
        auxDepots.add(new Depot("GLP-W", new Position(5, 10), 150, false));
        auxDepots.add(new Depot("INT-NE", new Position(22, 5), 100, false));
        auxDepots.add(new Depot("INT-SE", new Position(22, 15), 80, true));
        auxDepots.add(new Depot("INT-SW", new Position(7, 15), 120, false));
        auxDepots.add(new Depot("INT-NW", new Position(7, 5), 90, true));

        System.out.println("\n=== CREATED DEPOTS ===");
        for (Depot depot : auxDepots) {
            System.out.println(depot);
        }
        System.out.println("=====================\n");

        return auxDepots;
    }
}
