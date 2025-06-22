package com.vroute.controllers;

import com.vroute.models.Environment;
import com.vroute.orchest.Event;
import com.vroute.orchest.Orchestrator;
import com.vroute.ui.SimulationApp;

import java.util.function.Consumer;
import javax.swing.Timer;

/**
 * Controlador para la simulación del sistema V-Route.
 * Maneja el avance del tiempo y notifica eventos.
 */
public class SimulationController {
    private final Orchestrator orchestrator;
    private final Environment environment;
    private Timer autoAdvanceTimer;
    private SimulationApp simulationApp;
    
    // Estado de la simulación
    private boolean autoRunning = false;
    
    // Velocidad de simulación (minutos por tick)
    private int timeStepMinutes = 1;
    private int autoAdvanceIntervalMs = 1000; // 1 segundo por defecto
    
    /**
     * Constructor del controlador de simulación
     * 
     * @param orchestrator El orquestrador que maneja los eventos
     */
    public SimulationController(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.environment = orchestrator.getEnvironment();
    }
    
    /**
     * Inicia la interfaz gráfica
     */
    public void initUI() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            simulationApp = new SimulationApp();
            simulationApp.setEnvironment(environment);
        });
    }
    
    /**
     * Avanza el tiempo de la simulación
     * 
     * @param minutes Cantidad de minutos a avanzar
     */
    public void advanceTime(int minutes) {
        orchestrator.advanceTime(minutes);
        if (simulationApp != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                simulationApp.updateUI();
            });
        }
    }
    
    /**
     * Inicia el avance automático del tiempo
     */
    public void startAutoAdvance() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
        }
        
        autoAdvanceTimer = new Timer(autoAdvanceIntervalMs, e -> advanceTime(timeStepMinutes));
        autoAdvanceTimer.start();
        autoRunning = true;
    }
    
    /**
     * Detiene el avance automático del tiempo
     */
    public void stopAutoAdvance() {
        if (autoAdvanceTimer != null) {
            autoAdvanceTimer.stop();
        }
        autoRunning = false;
    }
    
    /**
     * Configura la velocidad de la simulación
     * 
     * @param minutesPerTick Minutos que avanzan por cada tick
     * @param intervalMillis Intervalo en milisegundos entre ticks
     */
    public void setSimulationSpeed(int minutesPerTick, int intervalMillis) {
        this.timeStepMinutes = minutesPerTick;
        this.autoAdvanceIntervalMs = intervalMillis;
        
        // Si está corriendo, reiniciamos con la nueva velocidad
        if (isAutoRunning()) {
            stopAutoAdvance();
            startAutoAdvance();
        }
    }
    
    /**
     * Agrega un listener para eventos del orquestrador
     * 
     * @param listener Función que procesa un evento
     */
    public void addEventListener(Consumer<Event> listener) {
        orchestrator.addEventListener(listener);
    }
    
    /**
     * Obtiene el entorno de simulación
     * 
     * @return El entorno actual
     */
    public Environment getEnvironment() {
        return environment;
    }
    
    /**
     * Obtiene el orquestrador de simulación
     * 
     * @return El orquestrador actual
     */
    public Orchestrator getOrchestrator() {
        return orchestrator;
    }
    
    /**
     * Verifica si el avance automático está activo
     * 
     * @return true si el avance automático está activo
     */
    public boolean isAutoRunning() {
        return autoRunning;
    }
    
    /**
     * Obtiene la velocidad actual de simulación en minutos por tick
     * 
     * @return Minutos por tick
     */
    public int getTimeStepMinutes() {
        return timeStepMinutes;
    }
    
    /**
     * Obtiene el intervalo entre ticks en milisegundos
     * 
     * @return Intervalo en ms
     */
    public int getAutoAdvanceIntervalMs() {
        return autoAdvanceIntervalMs;
    }
    
    /**
     * Actualiza el estado de la interfaz gráfica
     */
    public void updateUI() {
        if (simulationApp != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                simulationApp.updateUI();
            });
        }
    }
}
