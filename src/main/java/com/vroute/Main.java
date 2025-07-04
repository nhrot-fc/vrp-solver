package com.vroute;

import com.vroute.assignation.MetaheuristicAssignator;
import com.vroute.assignation.Solution;
import com.vroute.models.*;
import com.vroute.operation.VehiclePlan;
import com.vroute.operation.VehiclePlanCreator;
import com.vroute.ui.SwingMapRenderer;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends JFrame {

    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    private static final int CELL_SIZE = 20;

    private Environment environment;
    private Map<Vehicle, VehiclePlan> vehiclePlans;
    private JPanel mapPanel;
    private JPanel planDetailsPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            
            Main app = new Main();
            app.setVisible(true);
        });
    }

    public Main() {
        // set up the time to 01-01-2025 00:00:00
        LocalDateTime simulationStartTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        initializeSimulationEnvironment(simulationStartTime);

        // Create UI components
        createUI();

        // Initial draw of environment
        drawEnvironment();
    }

    private void createUI() {
        setTitle("V-Route Delivery Planning System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Map area
        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE));
        JScrollPane mapScrollPane = new JScrollPane(mapPanel);
        mapScrollPane.setPreferredSize(new Dimension(1080, 720));
        add(mapScrollPane, BorderLayout.CENTER);

        // Sidebar for plan details
        planDetailsPanel = new JPanel();
        planDetailsPanel.setLayout(new BoxLayout(planDetailsPanel, BoxLayout.Y_AXIS));
        planDetailsPanel.setPreferredSize(new Dimension(300, 0));
        JScrollPane detailsScrollPane = new JScrollPane(planDetailsPanel);
        add(detailsScrollPane, BorderLayout.EAST);

        // Button controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton runButton = new JButton("Run Assignation");
        runButton.addActionListener(e -> runAssignation());
        controlsPanel.add(runButton);
        add(controlsPanel, BorderLayout.SOUTH);
    }

    private void runAssignation() {
        // Clear previous plans
        vehiclePlans = new HashMap<>();
        planDetailsPanel.removeAll();

        MetaheuristicAssignator assignator = new MetaheuristicAssignator(environment);

        // Get solution
        Solution solution = assignator.solve(environment);
        planDetailsPanel.add(new JLabel("Solution Summary:"));
        planDetailsPanel.add(new JLabel(solution.toString()));

        solution.getVehicleOrderAssignments().forEach((vehicle, instructions) -> {
            if (!instructions.isEmpty()) {
                VehiclePlan plan = VehiclePlanCreator.createPlan(environment, vehicle, instructions);
                if (plan != null) {
                    vehiclePlans.put(vehicle, plan);
                    System.out.println(plan);
                    // Add plan details to sidebar
                    JLabel vehicleLabel = new JLabel("Plan for " + vehicle.getId() + ":");
                    planDetailsPanel.add(vehicleLabel);
                    planDetailsPanel.add(new JLabel(String.format("  Distance: %.2f km", plan.getTotalDistanceKm())));
                    planDetailsPanel.add(new JLabel(String.format("  GLP Delivered: %.2f m³", plan.getTotalGlpDeliveredM3())));
                    planDetailsPanel.add(new JLabel(String.format("  Fuel Used: %.2f gal", plan.getTotalFuelConsumedGal())));
                    planDetailsPanel.add(new JLabel("  Orders: " + plan.getServedOrders().size()));
                }
            }
        });

        // Redraw the map with the plans
        drawEnvironmentWithPlans();
        planDetailsPanel.revalidate();
        planDetailsPanel.repaint();
    }

    private void drawEnvironment() {
        if (mapPanel != null) {
            mapPanel.repaint();
        }
    }

    private void drawEnvironmentWithPlans() {
        drawEnvironment();
    }

    // Custom JPanel for rendering the map
    private class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Get the current simulation time
            LocalDateTime currentTime = environment.getCurrentTime();

            // Draw the grid
            SwingMapRenderer.drawGrid(g2d, GRID_WIDTH, GRID_HEIGHT, CELL_SIZE);

            // Draw depots
            SwingMapRenderer.drawDepots(g2d, environment.getAuxDepots(), environment.getMainDepot(), CELL_SIZE);

            // Draw vehicles
            SwingMapRenderer.drawVehicles(g2d, environment.getAvailableVehicles(), CELL_SIZE);

            // Draw orders
            SwingMapRenderer.drawOrders(g2d, environment.getPendingOrders(), environment, CELL_SIZE);

            // Draw blockages
            SwingMapRenderer.drawBlockages(g2d, environment, currentTime, CELL_SIZE);

            // Draw vehicle plans if available
            if (vehiclePlans != null) {
                for (VehiclePlan plan : vehiclePlans.values()) {
                    SwingMapRenderer.drawVehiclePlan(g2d, plan, CELL_SIZE);
                }
            }
        }
    }

    private void initializeSimulationEnvironment(LocalDateTime simulationStartTime) {
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 10000, true);
        List<Depot> auxDepots = createAuxiliaryDepots();
        List<Vehicle> vehicles = createSampleVehicles(mainDepot.getPosition());

        this.environment = new Environment(vehicles, mainDepot, auxDepots, simulationStartTime);
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

        for (int i = 1; i <= 100; i++) {
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

        orders.sort((o1, o2) -> o1.getDueTime().compareTo(o2.getDueTime()));

        for (Order order : orders) {
            environment.addOrder(order);
        }

        System.out.println("\n=== CREATED ORDERS ===");
        System.out.println("Total orders: " + orders.size());
        System.out.println("Earliest due date: " + orders.get(0).getDueTime());
        System.out.println("Latest due date: " + orders.get(orders.size() - 1).getDueTime());
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
