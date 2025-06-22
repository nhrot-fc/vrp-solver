package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.vroute.models.Environment;

public class ControlPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private Environment environment;
    private JLabel timeLabel;
    private JLabel statusLabel;
    private JSpinner timeStepSpinner;
    private JButton advanceTimeButton;
    private JButton refreshButton;
    
    private ActionListener advanceTimeListener;
    
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