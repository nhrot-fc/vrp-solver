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
    private JSlider speedSlider;

    // Simulation controls
    private JButton startButton;
    private JButton pauseButton;
    private JButton resetButton;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton resetViewButton;
    private Timer simulationTimer;
    private boolean simulationRunning = false;

    private ActionListener advanceTimeListener;
    private EnvironmentRenderer renderer;

    // Zoom tracking
    private int currentZoom = 50;
    private final int PADDING_HEIGHT = 40;
    private final int PADDING_WIDTH = 20;

    public ControlPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(PADDING_HEIGHT, PADDING_WIDTH, PADDING_HEIGHT, PADDING_WIDTH));

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));

        // Time display
        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        timePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        timeLabel = new JLabel("Current Time: Not initialized");
        timePanel.add(timeLabel);
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(timePanel, BorderLayout.WEST);
        controlsPanel.add(leftPanel);
        controlsPanel.add(Box.createHorizontalGlue());

        // Simulation control buttons in a compact panel (center)
        JPanel simulationControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        startButton = new JButton("Start");
        startButton.setEnabled(false);
        startButton.addActionListener(e -> startSimulation());
        simulationControlPanel.add(startButton);

        pauseButton = new JButton("Pause");
        pauseButton.setEnabled(false);
        pauseButton.addActionListener(e -> pauseSimulation());
        simulationControlPanel.add(pauseButton);

        resetButton = new JButton("Reset");
        resetButton.setEnabled(false);
        resetButton.addActionListener(e -> resetSimulation());
        simulationControlPanel.add(resetButton);

        controlsPanel.add(simulationControlPanel);
        controlsPanel.add(Box.createHorizontalGlue());

        // Zoom buttons in a compact panel
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        zoomInButton = new JButton("Zoom +");
        zoomInButton.addActionListener(e -> {
            // Increase zoom by 15 points to make zooming more noticeable
            currentZoom = Math.min(100, currentZoom + 15);
            if (renderer != null) {
                renderer.setZoom(currentZoom);
            }
        });
        zoomPanel.add(zoomInButton);

        zoomOutButton = new JButton("Zoom -");
        zoomOutButton.addActionListener(e -> {
            // Decrease zoom by 15 points to make zooming more noticeable
            currentZoom = Math.max(10, currentZoom - 15);
            if (renderer != null) {
                renderer.setZoom(currentZoom);
            }
        });
        zoomPanel.add(zoomOutButton);

        resetViewButton = new JButton("Reset View");
        resetViewButton.addActionListener(e -> {
            currentZoom = 50; // Reset to default zoom
            if (renderer != null) {
                renderer.setZoom(currentZoom);
                renderer.resetView(); // Reset to centered view
            }
        });
        zoomPanel.add(resetViewButton);

        controlsPanel.add(zoomPanel);

        // Add spring to push slider to the right
        controlsPanel.add(Box.createHorizontalGlue());

        // Speed slider with 5 discrete steps in a compact panel
        JPanel speedPanel = new JPanel(new BorderLayout());

        // Create a slider with 5 discrete steps and make it wider
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 5, 1);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setPreferredSize(new Dimension(250, speedSlider.getPreferredSize().height));

        Hashtable<Integer, JLabel> speedLabels = new Hashtable<>();
        speedLabels.put(1, new JLabel("x1"));
        speedLabels.put(2, new JLabel("x2"));
        speedLabels.put(3, new JLabel("x3"));
        speedLabels.put(4, new JLabel("x4"));
        speedLabels.put(5, new JLabel("x5"));
        speedSlider.setLabelTable(speedLabels);
        speedSlider.setPaintLabels(true);

        speedSlider.addChangeListener(e -> {
            if (!speedSlider.getValueIsAdjusting()) {
                updateTimerSpeed();
            }
        });

        speedPanel.add(speedSlider, BorderLayout.CENTER);
        controlsPanel.add(speedPanel);

        add(controlsPanel, BorderLayout.CENTER);

        simulationTimer = new Timer(500, e -> {
            if (environment != null && simulationRunning) {
                advanceSimulation();
            }
        });

        // Initialize timer speed based on slider position
        updateTimerSpeed();
    }

    public void setRenderer(EnvironmentRenderer renderer) {
        this.renderer = renderer;
        if (renderer != null) {
            renderer.setZoom(currentZoom);
        }
    }

    private void updateTimerSpeed() {
        int value = speedSlider.getValue();
        int delay;
        switch (value) {
            case 1:
                delay = 1000;
                break;
            case 2:
                delay = 500;
                break;
            case 3:
                delay = 250;
                break;
            case 4:
                delay = 125;
                break;
            case 5:
                delay = 50;
                break;
            default:
                delay = 1000;
        }

        if (simulationTimer != null) {
            simulationTimer.setDelay(delay);
            if (simulationRunning) {
                simulationTimer.restart();
            }
        }
    }
    
    /**
     * Gets the current simulation speed in milliseconds
     * @return Delay in milliseconds between simulation steps
     */
    public int getSimulationSpeed() {
        int value = speedSlider.getValue();
        switch (value) {
            case 1: return 1000;
            case 2: return 500;
            case 3: return 250;
            case 4: return 125;
            case 5: return 50;
            default: return 1000;
        }
    }
    
    /**
     * Sets the simulation timer from an external source
     * @param timer The timer to use for simulation
     */
    public void setSimulationTimer(Timer timer) {
        // Stop existing timer if present
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        
        this.simulationTimer = timer;
        simulationRunning = timer != null && timer.isRunning();
        
        // Update button states
        startButton.setEnabled(!simulationRunning);
        pauseButton.setEnabled(simulationRunning);
    }
    
    /**
     * Stops the simulation timer
     */
    public void stopSimulation() {
        simulationRunning = false;
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
        updateDisplay();
        startButton.setEnabled(true);
        resetButton.setEnabled(true);
    }

    public void setAdvanceTimeListener(ActionListener listener) {
        this.advanceTimeListener = listener;
    }
    
    /**
     * Sets a custom action listener for the start button
     * 
     * @param listener The action listener to set
     */
    public void setStartAction(ActionListener listener) {
        // Clear existing listeners
        for (ActionListener al : startButton.getActionListeners()) {
            startButton.removeActionListener(al);
        }
        // Add new listener
        startButton.addActionListener(listener);
    }
    
    /**
     * Sets a custom action listener for the pause button
     * 
     * @param listener The action listener to set
     */
    public void setPauseAction(ActionListener listener) {
        // Clear existing listeners
        for (ActionListener al : pauseButton.getActionListeners()) {
            pauseButton.removeActionListener(al);
        }
        // Add new listener
        pauseButton.addActionListener(listener);
    }
    
    /**
     * Sets a custom action listener for the reset button
     * 
     * @param listener The action listener to set
     */
    public void setResetAction(ActionListener listener) {
        // Clear existing listeners
        for (ActionListener al : resetButton.getActionListeners()) {
            resetButton.removeActionListener(al);
        }
        // Add new listener
        resetButton.addActionListener(listener);
    }

    /**
     * Avanza un paso en la simulación
     */
    private void advanceSimulation() {
        if (environment != null && advanceTimeListener != null) {
            advanceTimeListener.actionPerformed(null);
        }
    }

    /**
     * Inicia la simulación automática
     */
    private void startSimulation() {
        if (environment != null) {
            simulationRunning = true;
            updateTimerSpeed(); // Set correct speed
            simulationTimer.start();
            startButton.setEnabled(false);
            pauseButton.setEnabled(true);
        }
    }

    /**
     * Pausa la simulación automática
     */
    private void pauseSimulation() {
        simulationRunning = false;
        simulationTimer.stop();
        startButton.setEnabled(true);
        pauseButton.setEnabled(false);
    }

    /**
     * Reinicia la simulación al estado inicial
     */
    private void resetSimulation() {
        // Pause simulation if running
        if (simulationRunning) {
            pauseSimulation();
        }

        // Ask for confirmation before resetting
        int response = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to reset the simulation?\nThis will reset all vehicles, orders, and time to their initial state.",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (response == JOptionPane.YES_OPTION) {
            // Reset simulation by notifying the controller via listener
            if (environment != null && advanceTimeListener != null) {
                // Create a new listener for reset specifically
                if (renderer != null) {
                    renderer.resetView(); // Reset map view to default
                }

                // This would require a new event system or controller to handle resets
                // For now, display a message that it would be implemented
                JOptionPane.showMessageDialog(
                        this,
                        "Reset functionality will be implemented in the controller.\n" +
                                "In a full implementation, this would recreate the initial environment state.",
                        "Reset Simulation",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void updateDisplay() {
        if (environment == null) {
            timeLabel.setText("Current Time: Not initialized");
            return;
        }

        LocalDateTime currentTime = environment.getCurrentTime();
        timeLabel.setText("Current Time: " + currentTime.format(TIME_FORMATTER));
    }
}