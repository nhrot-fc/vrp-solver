package com.vroute.ui;

import com.vroute.models.*;
import com.vroute.operation.VehiclePlan;
import com.vroute.orchest.DataReader;
import com.vroute.orchest.Event;
import com.vroute.orchest.EventType;
import com.vroute.orchest.Orchestrator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SimulationApp extends JFrame {
    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    private static final int CELL_SIZE = 20;
    private static final Logger logger = Logger.getLogger(SimulationApp.class.getName());

    // Simulation components
    private Environment environment;
    private Orchestrator orchestrator;
    private DataReader dataReader;

    // UI components
    private JPanel mapPanel;
    private JPanel statusPanel;
    private JPanel planDetailsPanel;
    private JLabel simulationTimeLabel;
    private JLabel statusLabel;
    private JProgressBar simulationProgress;
    private Timer simulationTimer;
    private String currentTimeText = "";

    // Simulation speed (milliseconds between steps)
    private int simulationSpeed = 500;

    // event list
    private List<Event> eventList = new ArrayList<>();

    public SimulationApp() {
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
        
        // Create UI
        createUI();

        // Update time display
        updateTimeDisplay();

        // Initial draw of the environment
        drawEnvironment();
    }

    private void createUI() {
        setTitle("V-Route Delivery Planning Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        // Create main layout
        setLayout(new BorderLayout());

        // Map area (center)
        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE));
        JScrollPane mapScrollPane = new JScrollPane(mapPanel);
        mapScrollPane.setPreferredSize(new Dimension(1080, 720));
        add(mapScrollPane, BorderLayout.CENTER);

        // Status and controls (top)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));

        simulationTimeLabel = new JLabel();
        simulationTimeLabel.setText(currentTimeText);
        simulationTimeLabel.setFont(simulationTimeLabel.getFont().deriveFont(Font.BOLD));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(Color.BLUE);

        simulationProgress = new JProgressBar(0, 100);
        simulationProgress.setPreferredSize(new Dimension(200, 20));
        simulationProgress.setStringPainted(true);

        topPanel.add(new JLabel("Simulation Time:"));
        topPanel.add(simulationTimeLabel);
        topPanel.add(new JLabel("Status:"));
        topPanel.add(statusLabel);
        topPanel.add(simulationProgress);
        add(topPanel, BorderLayout.NORTH);

        // Sidebar for plan details and stats (right)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(300, 0));

        // Status box
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Simulation Status"));
        
        // Plan details box
        planDetailsPanel = new JPanel();
        planDetailsPanel.setLayout(new BoxLayout(planDetailsPanel, BoxLayout.Y_AXIS));
        planDetailsPanel.setBorder(BorderFactory.createTitledBorder("Vehicle Plans"));

        JScrollPane planDetailsScrollPane = new JScrollPane(planDetailsPanel);
        planDetailsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        rightPanel.add(statusPanel, BorderLayout.NORTH);
        rightPanel.add(planDetailsScrollPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Controls (bottom)
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton startButton = new JButton("Start Simulation");
        startButton.addActionListener(e -> startSimulation());

        JButton pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> pauseSimulation());

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> stopSimulation());

        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 1);
        speedSlider.setMajorTickSpacing(5);
        speedSlider.setMinorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.addChangeListener(e -> {
            simulationSpeed = (int) (500 / speedSlider.getValue());
            if (simulationTimer != null && simulationTimer.isRunning()) {
                pauseSimulation();
                startSimulation();
            }
        });

        controlsPanel.add(new JLabel("Speed:"));
        controlsPanel.add(speedSlider);
        controlsPanel.add(startButton);
        controlsPanel.add(pauseButton);
        controlsPanel.add(stopButton);
        add(controlsPanel, BorderLayout.SOUTH);
    }

    private void initializeEnvironment(LocalDateTime startTime) {
        Depot mainDepot = new Depot(Constants.MAIN_PLANT_ID, Constants.CENTRAL_STORAGE_LOCATION, 1000000, true);
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("NORTH", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, false));
        auxDepots.add(new Depot("EAST", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, false));
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

        for (int i = 1; i <= 3; i++) {
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

    private void startSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            return; // Simulation already running
        }

        // Initialize the orchestrator if needed
        if (orchestrator.getVehiclePlans().isEmpty()) {
            updateStatus("Running initial planning...");
            try {
                // Just initialize the orchestrator without starting the simulation
                // The simulation will be advanced step by step in the timeline
                orchestrator.initialize();
                orchestrator.prepareSimulation();
            } catch (Exception e) {
                updateStatus("Error during planning: " + e.getMessage());
                logger.severe("Planning error: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        updateStatus("Simulation running");

        // Create a timer for simulation steps
        simulationTimer = new Timer(simulationSpeed, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    updateSimulationStep();
                } catch (Exception ex) {
                    updateStatus("Error during simulation: " + ex.getMessage());
                    logger.severe("Simulation error: " + ex.getMessage());
                    ex.printStackTrace();
                    pauseSimulation();
                }
            }
        });
        simulationTimer.start();
    }

    private void pauseSimulation() {
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
            updateStatus("Simulation paused");
        }
    }

    private void stopSimulation() {
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        updateStatus("Simulation stopped");
    }

    private void updateSimulationStep() {
        // Run a single step of the simulation using the refactored orchestrator's advanceTick method
        boolean simulationContinues = orchestrator.advanceTick();

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
        currentTimeText = simulationTime.format(formatter);
        if (simulationTimeLabel != null) {
            simulationTimeLabel.setText(currentTimeText);
        }
    }

    private void updateStatus(String status) {
        // Check if UI is already initialized
        if (statusLabel != null) {
            statusLabel.setText(status);

            // Add to status history if the panel is initialized
            if (statusPanel != null) {
                JLabel statusHistoryLabel = new JLabel(
                        environment.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + status);
                if (statusPanel.getComponentCount() > 20) {
                    statusPanel.remove(1); // Keep the header
                }
                statusPanel.add(statusHistoryLabel);
                statusPanel.revalidate();
                statusPanel.repaint();
            }
        }

        // Always log the status
        logger.info(status);
    }

    private void updateStats() {
        // Update simulation progress - assume we're showing a day's worth of progress
        double progressValue = (environment.getCurrentTime().getHour() * 60 + environment.getCurrentTime().getMinute())
                / (24.0 * 60);
        simulationProgress.setValue((int) (Math.min(1.0, progressValue) * 100));
        
        // Update plan details panel with current order and blockage statistics
        if (planDetailsPanel != null) {
            planDetailsPanel.removeAll();
            
            JLabel plansHeader = new JLabel("Simulation Statistics");
            plansHeader.setFont(plansHeader.getFont().deriveFont(Font.BOLD));
            planDetailsPanel.add(plansHeader);
            
            // Order stats
            int totalOrders = environment.getOrderQueue().size();
            int pendingOrders = environment.getPendingOrders().size();
            int deliveredOrders = totalOrders - pendingOrders;
            int overdueOrders = environment.getOverdueOrders().size();
            
            planDetailsPanel.add(new JLabel(String.format("Orders: %d total, %d delivered, %d pending, %d overdue",
                    totalOrders, deliveredOrders, pendingOrders, overdueOrders)));
            
            // Blockage stats
            int activeBlockages = environment.getActiveBlockagesAt(environment.getCurrentTime()).size();
            planDetailsPanel.add(new JLabel(String.format("Active blockages: %d", activeBlockages)));
            
            // Vehicle stats
            int availableVehicles = environment.getAvailableVehicles().size();
            int totalVehicles = environment.getVehicles().size();
            planDetailsPanel.add(new JLabel(String.format("Vehicles: %d/%d available", 
                    availableVehicles, totalVehicles)));
            
            // Add each vehicle plan summary
            planDetailsPanel.add(new JLabel("Vehicle Plans:"));
            for (Map.Entry<Vehicle, VehiclePlan> entry : orchestrator.getVehiclePlans().entrySet()) {
                Vehicle vehicle = entry.getKey();
                VehiclePlan plan = entry.getValue();
                
                String status = vehicle.getStatus().toString();
                String planSummary = plan != null ? 
                        String.format("%d actions, %d orders", plan.getActions().size(), plan.getOrderCount()) : "No plan";
                
                planDetailsPanel.add(new JLabel(String.format("  %s (%s): %s", 
                        vehicle.getId(), status, planSummary)));
            }
            
            planDetailsPanel.revalidate();
            planDetailsPanel.repaint();
        }
    }

    private void drawEnvironment() {
        if (mapPanel != null) {
            mapPanel.repaint();
        }
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

            // Draw orders
            SwingMapRenderer.drawOrders(g2d, environment.getPendingOrders(), environment, CELL_SIZE);

            // Draw blockages
            SwingMapRenderer.drawBlockages(g2d, environment, currentTime, CELL_SIZE);

            // Draw vehicles
            SwingMapRenderer.drawVehicles(g2d, environment.getAvailableVehicles(), CELL_SIZE);

            // Draw vehicle plans if available
            for (VehiclePlan plan : orchestrator.getVehiclePlans().values()) {
                SwingMapRenderer.drawCurrentVehiclePath(g2d, plan, currentTime, CELL_SIZE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            
            SimulationApp app = new SimulationApp();
            app.setVisible(true);
        });
    }
}
