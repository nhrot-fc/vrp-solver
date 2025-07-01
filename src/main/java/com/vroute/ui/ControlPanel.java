package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;

import com.vroute.orchest.Orchestrator;

public class ControlPanel extends JPanel {
    private final Orchestrator orchestrator;
    private Timer simulationTimer;
    private int tickFrequency = 1; // ticks per second
    private boolean isRunning = false;
    
    private JButton startButton;
    private JButton pauseButton;
    private JButton resetButton;
    private JSlider tickSlider;
    private JLabel statusLabel;
    
    public ControlPanel(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        initializeUI();
        setupTimer();
    }
    
    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Create buttons
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        resetButton = new JButton("Reset");
        
        // Disable pause button initially
        pauseButton.setEnabled(false);
        
        // Create slider for tick frequency
        tickSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 10);
        tickSlider.setMajorTickSpacing(5);
        tickSlider.setMinorTickSpacing(1);
        tickSlider.setPaintTicks(true);
        tickSlider.setPaintLabels(true);
        tickSlider.setPreferredSize(new Dimension(200, 40));
        
        // Create status label
        statusLabel = new JLabel("Simulation paused");
        
        // Add components
        add(startButton);
        add(pauseButton);
        add(resetButton);
        add(new JLabel("Tick Speed:"));
        add(tickSlider);
        add(statusLabel);
        
        // Add action listeners
        startButton.addActionListener(e -> startSimulation());
        pauseButton.addActionListener(e -> pauseSimulation());
        resetButton.addActionListener(e -> resetSimulation());
        tickSlider.addChangeListener(e -> updateTickFrequency());
        
        // Set initial tick frequency
        updateTickFrequency();
    }
    
    private void setupTimer() {
        simulationTimer = new Timer(1000 / tickFrequency, e -> {
            // Advance simulation by one minute when timer fires
            orchestrator.advanceTime(Duration.ofMinutes(1));
            // Repaint the parent component (which should contain the renderer)
            Container parent = getParent();
            if (parent != null) {
                parent.repaint();
            }
        });
        simulationTimer.setRepeats(true);
    }
    
    private void updateTickFrequency() {
        tickFrequency = tickSlider.getValue() / 10 + 1; // Scale 1-20 to 1.1-3.0 ticks per second
        if (simulationTimer != null) {
            simulationTimer.setDelay(1000 / tickFrequency);
            statusLabel.setText(isRunning ? 
                String.format("Running: %.1f ticks/sec", (float)tickFrequency) : 
                "Simulation paused");
        }
    }
    
    private void startSimulation() {
        isRunning = true;
        orchestrator.start();
        simulationTimer.start();
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        resetButton.setEnabled(false);
        updateTickFrequency(); // Update status label
    }
    
    private void pauseSimulation() {
        isRunning = false;
        orchestrator.stop();
        simulationTimer.stop();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
        resetButton.setEnabled(true);
        statusLabel.setText("Simulation paused");
    }
    
    private void resetSimulation() {
        // Reset simulation state - could involve reloading initial data
        // This would depend on how the application manages state
        pauseSimulation();
        // Additional reset logic would go here
    }
}