package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.vroute.models.Environment;
import com.vroute.orchest.Event;
import com.vroute.orchest.Orchestrator;

public class SimulationApp extends JFrame {
    private EnvironmentRenderer environmentRenderer;
    private ControlPanel controlPanel;
    private Environment environment;
    private Orchestrator orchestrator;
    private JScrollPane mapScrollPane;
    
    public SimulationApp() {
        setTitle("V-Route Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        initComponents();
        setupLayout();
        setupListeners();
        
        pack();
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }
    
    private void initComponents() {
        environmentRenderer = new EnvironmentRenderer();
        controlPanel = new ControlPanel();
        controlPanel.setRenderer(environmentRenderer);
        
        // Create a scroll pane for the environment renderer with always visible scrollbars
        mapScrollPane = new JScrollPane(environmentRenderer);
        mapScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        mapScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mapScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        mapScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
    }
    
    private void setupLayout() {
        // Set map scroll pane to a bigger size
        mapScrollPane.setPreferredSize(new Dimension(1200, 800));
        
        // Use a BorderLayout instead of SplitPane for more control over sizing
        setLayout(new BorderLayout());
        
        // Add the map component to the center (will get all extra space)
        add(mapScrollPane, BorderLayout.CENTER);
        
        // Create a wrapper panel with a border layout and padding
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createEmptyBorder(5, 25, 5, 25));
        
        // Add the control panel to fill the center of the wrapper
        wrapperPanel.add(controlPanel, BorderLayout.CENTER);
        
        // Add the wrapper to the south of the main layout
        add(wrapperPanel, BorderLayout.SOUTH);
        
        // Set minimum size for the window
        setMinimumSize(new Dimension(800, 600));
    }
    
    private void setupListeners() {
        // Setup listener for the advance time button
        controlPanel.setAdvanceTimeListener(e -> {
            if (environment != null) {
                if (orchestrator != null) {
                    // Use orchestrator's advanceTime method
                    orchestrator.advanceTime(1);
                } else {
                    // Fallback to direct environment advancement
                    environment.advanceTime(1);
                }
                updateUI();
            }
        });
        
        // Mouse listeners for map drag functionality
        MouseAdapter mapDragListener = new MouseAdapter() {
            private Point lastPoint = null;
            
            @Override
            public void mousePressed(MouseEvent e) {
                // When user clicks on the map, disable auto-centering
                environmentRenderer.disableAutoCenter();
                lastPoint = e.getPoint();
                // Change cursor to indicate dragging is possible
                environmentRenderer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
                // Reset cursor
                environmentRenderer.setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastPoint != null) {
                    // Calculate the drag distance
                    int dx = lastPoint.x - e.getX();
                    int dy = lastPoint.y - e.getY();
                    
                    // Get the viewport and scroll it
                    JViewport viewport = mapScrollPane.getViewport();
                    Point viewPos = viewport.getViewPosition();
                    
                    // Calculate new view position
                    int newX = Math.max(0, Math.min(viewPos.x + dx, 
                            environmentRenderer.getWidth() - viewport.getWidth()));
                    int newY = Math.max(0, Math.min(viewPos.y + dy, 
                            environmentRenderer.getHeight() - viewport.getHeight()));
                    
                    viewport.setViewPosition(new Point(newX, newY));
                    
                    // Update last point
                    lastPoint = e.getPoint();
                }
            }
        };
        
        // Register mouse listeners on the environment renderer
        environmentRenderer.addMouseListener(mapDragListener);
        environmentRenderer.addMouseMotionListener(mapDragListener);
        
        // Setup window closing listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Cleanup code if needed
                System.exit(0);
            }
        });
    }
    
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        environmentRenderer.setEnvironment(environment);
        controlPanel.setEnvironment(environment);
        
        // Center the view initially
        SwingUtilities.invokeLater(() -> {
            // Ensure scrollbars update and center on first load
            environmentRenderer.resetView();
        });
    }
    
    /**
     * Sets the orchestrator and configures event handling
     * 
     * @param orchestrator The orchestrator to use
     */
    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        
        if (orchestrator != null) {
            // Subscribe to orchestrator events
            orchestrator.addEventListener(this::handleEvent);
            
            // Configure control panel actions
            controlPanel.setStartAction(e -> startSimulation());
            controlPanel.setPauseAction(e -> stopSimulation());
            controlPanel.setResetAction(e -> resetSimulation());
        }
    }
    
    /**
     * Starts the simulation
     */
    private void startSimulation() {
        if (orchestrator == null) return;
        
        // Create a timer to advance the simulation
        Timer simulationTimer = new Timer(controlPanel.getSimulationSpeed(), e -> {
            orchestrator.advanceTime(1);
            updateUI();
        });
        
        simulationTimer.start();
        controlPanel.setSimulationTimer(simulationTimer);
    }
    
    /**
     * Stops the simulation
     */
    private void stopSimulation() {
        controlPanel.stopSimulation();
    }
    
    /**
     * Resets the simulation (placeholder - actual implementation would recreate the environment)
     */
    private void resetSimulation() {
        // Stop the simulation first
        stopSimulation();
        
        // For a real reset, we would need to recreate the environment
        JOptionPane.showMessageDialog(
                this,
                "Reset functionality would recreate the environment from initial data.",
                "Reset Simulation",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Handle events from the orchestrator
     * @param event The event to handle
     */
    private void handleEvent(Event event) {
        // Update the UI when events occur
        SwingUtilities.invokeLater(() -> {
            updateUI();
            // Could also display event notifications or highlight related entities
        });
    }
    
    /**
     * Updates the UI components with the current state of the environment
     */
    public void updateUI() {
        if (environment != null) {
            environmentRenderer.repaint();
            controlPanel.updateDisplay();
        }
    }
    
    public static void main(String[] args) {
        // Use Swing's Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new SimulationApp();
        });
    }
} 