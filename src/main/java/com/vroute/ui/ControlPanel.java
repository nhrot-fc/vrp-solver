package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

import com.vroute.models.Environment;

public class ControlPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private Environment environment;
    private JLabel timeLabel;
    private JLabel statusLabel;
    private JSpinner timeStepSpinner;
    private JButton advanceTimeButton;
    private JButton refreshButton;
    private JSlider zoomSlider;
    private JSlider speedSlider;
    
    private ActionListener advanceTimeListener;
    private EnvironmentRenderer renderer;
    
    public ControlPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // North panel with title and time
        JPanel northPanel = createNorthPanel();
        add(northPanel, BorderLayout.NORTH);
        
        // Center panel with controls
        JPanel controlsPanel = createControlsPanel();
        add(controlsPanel, BorderLayout.CENTER);
        
        // South panel with status
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    public void setRenderer(EnvironmentRenderer renderer) {
        this.renderer = renderer;
        if (zoomSlider != null) {
            zoomSlider.setValue(50); // Default zoom level
            updateZoom();
        }
    }
    
    private JPanel createNorthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("V-Route Simulation");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        timeLabel = new JLabel("Current Time: Not initialized");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(timeLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Time step control
        JPanel timeStepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeStepPanel.add(new JLabel("Time Step (minutes): "));
        
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(15, 1, 1440, 5);
        timeStepSpinner = new JSpinner(spinnerModel);
        timeStepPanel.add(timeStepSpinner);
        panel.add(timeStepPanel);
        
        // Speed slider
        JPanel speedPanel = new JPanel(new BorderLayout());
        speedPanel.setBorder(BorderFactory.createTitledBorder("Simulation Speed"));
        
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 50);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        
        Hashtable<Integer, JLabel> speedLabels = new Hashtable<>();
        speedLabels.put(1, new JLabel("Slow"));
        speedLabels.put(50, new JLabel("Normal"));
        speedLabels.put(100, new JLabel("Fast"));
        speedSlider.setLabelTable(speedLabels);
        speedSlider.setPaintLabels(true);
        
        speedSlider.addChangeListener(e -> {
            if (!speedSlider.getValueIsAdjusting()) {
                updateTimeStep();
            }
        });
        
        speedPanel.add(speedSlider, BorderLayout.CENTER);
        panel.add(speedPanel);
        
        // Zoom slider
        JPanel zoomPanel = new JPanel(new BorderLayout());
        zoomPanel.setBorder(BorderFactory.createTitledBorder("Map Zoom"));
        
        zoomSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 50);
        zoomSlider.setMajorTickSpacing(25);
        zoomSlider.setMinorTickSpacing(5);
        zoomSlider.setPaintTicks(true);
        
        Hashtable<Integer, JLabel> zoomLabels = new Hashtable<>();
        zoomLabels.put(1, new JLabel("1x"));
        zoomLabels.put(50, new JLabel("2x"));
        zoomLabels.put(100, new JLabel("3x"));
        zoomSlider.setLabelTable(zoomLabels);
        zoomSlider.setPaintLabels(true);
        
        zoomSlider.addChangeListener(e -> {
            if (!zoomSlider.getValueIsAdjusting()) {
                updateZoom();
            }
        });
        
        zoomPanel.add(zoomSlider, BorderLayout.CENTER);
        panel.add(zoomPanel);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        advanceTimeButton = new JButton("Advance Time");
        advanceTimeButton.setEnabled(false);
        advanceTimeButton.addActionListener(e -> {
            if (advanceTimeListener != null) {
                advanceTimeListener.actionPerformed(e);
            }
        });
        buttonsPanel.add(advanceTimeButton);
        
        refreshButton = new JButton("Refresh View");
        refreshButton.setEnabled(false);
        refreshButton.addActionListener(e -> updateDisplay());
        buttonsPanel.add(refreshButton);
        
        panel.add(buttonsPanel);
        
        return panel;
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Status: Not initialized");
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }
    
    private void updateZoom() {
        if (renderer != null) {
            renderer.setZoom(zoomSlider.getValue());
        }
    }
    
    private void updateTimeStep() {
        int value = speedSlider.getValue();
        // Map slider value (1-100) to time steps:
        // - 1 = 1 minute
        // - 50 = 15 minutes
        // - 100 = 60 minutes
        int timeStep;
        if (value <= 50) {
            // 1 to 15 minutes (linear mapping from 1-50)
            timeStep = 1 + (int)((value - 1) * (14.0 / 49.0));
        } else {
            // 15 to 60 minutes (linear mapping from 51-100)
            timeStep = 15 + (int)((value - 50) * (45.0 / 50.0));
        }
        timeStepSpinner.setValue(timeStep);
    }
    
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        updateDisplay();
        advanceTimeButton.setEnabled(true);
        refreshButton.setEnabled(true);
    }
    
    public void setAdvanceTimeListener(ActionListener listener) {
        this.advanceTimeListener = listener;
    }
    
    public int getTimeStep() {
        return (Integer) timeStepSpinner.getValue();
    }
    
    public void updateDisplay() {
        if (environment == null) {
            timeLabel.setText("Current Time: Not initialized");
            statusLabel.setText("Status: Environment not loaded");
            return;
        }
        
        LocalDateTime currentTime = environment.getCurrentTime();
        timeLabel.setText("Current Time: " + currentTime.format(TIME_FORMATTER));
        
        // Update status with summary information
        StringBuilder statusText = new StringBuilder("Status: ");
        statusText.append(environment.getAvailableVehicles().size())
                 .append("/").append(environment.getVehicles().size())
                 .append(" vehicles available | ");
        
        statusText.append(environment.getPendingOrders().size())
                 .append(" pending orders | ");
        
        statusText.append(environment.getOverdueOrders().size())
                 .append(" overdue orders");
        
        statusLabel.setText(statusText.toString());
    }
} 