package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.vroute.models.Environment;

public class SimulationApp extends JFrame {
    private EnvironmentRenderer environmentRenderer;
    private ControlPanel controlPanel;
    private Environment environment;
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
        // Create the main split pane with vertical split (map on top, controls at bottom)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.95); // 95% for the map, 5% for controls
        
        // Set map scroll pane to a bigger size
        mapScrollPane.setPreferredSize(new Dimension(1200, 800));
        
        // Add components to the split pane
        splitPane.setTopComponent(mapScrollPane);
        splitPane.setBottomComponent(controlPanel);
        splitPane.setDividerLocation(700); // Position the divider to favor the map
        
        // Add the split pane to the frame
        getContentPane().add(splitPane);
    }
    
    private void setupListeners() {
        // Setup listener for the advance time button
        controlPanel.setAdvanceTimeListener(e -> {
            if (environment != null) {
                environment.advanceTime(1);
                updateUI();
            }
        });
        
        // Add mouse listener to detect when user is manually scrolling/panning
        environmentRenderer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // When user clicks on the map, disable auto-centering
                environmentRenderer.disableAutoCenter();
            }
        });
        
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