package com.vroute.ui;

import javax.swing.*;
import java.awt.*;

import com.vroute.models.Environment;
import com.vroute.orchest.Orchestrator;

public class SimulationApp extends JFrame {
    private EnvironmentRenderer environmentRenderer;
    private ControlPanel controlPanel;
    private Orchestrator orchestrator;

    public SimulationApp(Environment initialEnvironment) {
        this.orchestrator = new Orchestrator(initialEnvironment);
        initializeUI();
    }

    private void initializeUI() {
        // Set window properties
        setTitle("V-Route Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        
        // Create main components
        environmentRenderer = new EnvironmentRenderer();
        environmentRenderer.setEnvironment(orchestrator.getEnvironment());
        environmentRenderer.setOrchestrator(orchestrator);
        
        controlPanel = new ControlPanel(orchestrator);
        
        // Add components to frame
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        
        // Wrap environment renderer in a scroll pane
        JScrollPane scrollPane = new JScrollPane(environmentRenderer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Add wheel mouse listener for zooming
        scrollPane.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                // Control + mouse wheel = zoom
                int currentZoom = environmentRenderer.getZoom();
                int notches = e.getWheelRotation();
                environmentRenderer.setZoom(Math.max(0, Math.min(100, currentZoom - notches * 5)));
                e.consume();
            }
        });
        
        // Add to frame
        add(scrollPane, BorderLayout.CENTER);
        
        // Add status bar at the bottom that shows environment info
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        JLabel statusLabel = new JLabel(orchestrator.getEnvironment().toString());
        
        // Update status periodically
        Timer statusTimer = new Timer(1000, e -> {
            statusLabel.setText(orchestrator.getEnvironment().toString());
        });
        statusTimer.start();
        
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
        
        // Set window to be centered on screen
        setLocationRelativeTo(null);
    }
}