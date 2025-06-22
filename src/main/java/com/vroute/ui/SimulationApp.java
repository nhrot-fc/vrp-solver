package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.vroute.models.Environment;

public class SimulationApp extends JFrame {
    private EnvironmentRenderer environmentRenderer;
    private ControlPanel controlPanel;
    private Environment environment;
    
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
    }
    
    private void setupLayout() {
        // Create the main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.8); // 80% for the map, 20% for controls
        
        // Create a scroll pane for the environment renderer to allow scrolling for large maps
        JScrollPane mapScrollPane = new JScrollPane(environmentRenderer);
        mapScrollPane.setPreferredSize(new Dimension(800, 600));
        mapScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling
        mapScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        // Add components to the split pane
        splitPane.setLeftComponent(mapScrollPane);
        splitPane.setRightComponent(controlPanel);
        
        // Add the split pane to the frame
        getContentPane().add(splitPane);
    }
    
    private void setupListeners() {
        // Setup listener for the advance time button
        controlPanel.setAdvanceTimeListener(e -> {
            if (environment != null) {
                int timeStep = controlPanel.getTimeStep();
                environment.advanceTime(timeStep);
                environmentRenderer.repaint();
                controlPanel.updateDisplay();
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