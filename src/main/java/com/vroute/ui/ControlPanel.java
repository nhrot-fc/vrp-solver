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
    private int currentZoom = 50; // Default zoom level (middle)

    public ControlPanel() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Time display
        timeLabel = new JLabel("Current Time: Not initialized");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        add(timeLabel);

        // Simulation control buttons
        JPanel simulationControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        simulationControlPanel.setBorder(BorderFactory.createTitledBorder("Simulation Control"));

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

        add(simulationControlPanel);

        // Zoom buttons
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        zoomPanel.setBorder(BorderFactory.createTitledBorder("Map Zoom"));

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

        add(zoomPanel);

        // Speed slider with 5 discrete steps
        JPanel speedPanel = new JPanel(new BorderLayout());
        speedPanel.setBorder(BorderFactory.createTitledBorder("Simulation Speed"));

        // Create a slider with 5 discrete steps
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 5, 1);
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.setSnapToTicks(true);

        Hashtable<Integer, JLabel> speedLabels = new Hashtable<>();
        speedLabels.put(1, new JLabel("X1"));
        speedLabels.put(2, new JLabel("X2"));
        speedLabels.put(3, new JLabel("X3"));
        speedLabels.put(4, new JLabel("X4"));
        speedLabels.put(5, new JLabel("X5"));
        speedSlider.setLabelTable(speedLabels);
        speedSlider.setPaintLabels(true);

        speedSlider.addChangeListener(e -> {
            if (!speedSlider.getValueIsAdjusting()) {
                updateTimerSpeed();
            }
        });

        speedPanel.add(speedSlider, BorderLayout.CENTER);
        add(speedPanel);

        // Create simulation timer (initially with medium speed - X3)
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
        // Map slider value (1-5) to timer delay in ms
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

        // Reset logic - depends on how you want to define "reset"
        // For now, we'll just notify that reset was requested via a message
        JOptionPane.showMessageDialog(
                this,
                "Reset functionality needs to be implemented based on application requirements.\n" +
                        "This could reload initial state, reset time, or restart simulation.",
                "Reset Requested",
                JOptionPane.INFORMATION_MESSAGE);
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