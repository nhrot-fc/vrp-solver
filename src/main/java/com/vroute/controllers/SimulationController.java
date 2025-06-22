package com.vroute.controllers;

import com.vroute.models.Environment;
import com.vroute.orchest.Orchestrator;
import com.vroute.ui.SimulationApp;

import javax.swing.Timer;
import javax.swing.UIManager;

public class SimulationController {
    private final Orchestrator orchestrator;
    private final Environment environment;
    private Timer autoAdvanceTimer;
    private SimulationApp simulationApp;
    
    private boolean autoRunning = false;
    
    private int timeStepMinutes = 1;
    private int autoAdvanceIntervalMs = 1000; // 1 segundo por defecto
    
    public SimulationController(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.environment = orchestrator.getEnvironment();
    }
    
    public void initUI() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            simulationApp = new SimulationApp();
            simulationApp.setEnvironment(environment);
            simulationApp.setOrchestrator(orchestrator);
        });
    }
    
    public void advanceTime(int minutes) {
        orchestrator.advanceTime(minutes);
        if (simulationApp != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                simulationApp.updateUI();
            });
        }
    }
    
    public void startAutoAdvance() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
        }
        
        autoAdvanceTimer = new Timer(autoAdvanceIntervalMs, e -> advanceTime(timeStepMinutes));
        autoAdvanceTimer.start();
        autoRunning = true;
    }
    
    public void stopAutoAdvance() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
        }
        autoRunning = false;
    }
    
    public void setSimulationSpeed(int minutesPerTick, int intervalMillis) {
        this.timeStepMinutes = minutesPerTick;
        this.autoAdvanceIntervalMs = intervalMillis;
        
        if (isAutoRunning()) {
            stopAutoAdvance();
            startAutoAdvance();
        }
    }
    

    
    public Environment getEnvironment() {
        return environment;
    }
    
    public Orchestrator getOrchestrator() {
        return orchestrator;
    }
    
    public boolean isAutoRunning() {
        return autoRunning;
    }
    
    public int getTimeStepMinutes() {
        return timeStepMinutes;
    }
    
    public int getAutoAdvanceIntervalMs() {
        return autoAdvanceIntervalMs;
    }
    
    public void updateUI() {
        if (simulationApp != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                simulationApp.updateUI();
            });
        }
    }
}
