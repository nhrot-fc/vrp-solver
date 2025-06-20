package com.vroute.controllers;

import com.vroute.models.Environment;
import com.vroute.orchest.Event;
import com.vroute.orchest.Orchestrator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

/**
 * Controlador para la simulación del sistema V-Route.
 * Maneja el avance del tiempo y notifica eventos.
 */
public class SimulationController {
    private final Orchestrator orchestrator;
    private final Environment environment;
    private Timeline autoAdvanceTimeline;
    
    // Propiedades observables para la interfaz
    private final StringProperty currentTimeProperty = new SimpleStringProperty();
    private final BooleanProperty autoRunningProperty = new SimpleBooleanProperty(false);
    
    // Velocidad de simulación (minutos por tick)
    private int timeStepMinutes = 1;
    private int autoAdvanceIntervalMs = 1000; // 1 segundo por defecto
    
    // Formato de fecha/hora para mostrar
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * Constructor del controlador de simulación
     * 
     * @param orchestrator El orquestrador que maneja los eventos
     */
    public SimulationController(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.environment = orchestrator.getEnvironment();
        updateTimeProperty();
    }
    
    /**
     * Avanza el tiempo de la simulación
     * 
     * @param minutes Cantidad de minutos a avanzar
     */
    public void advanceTime(int minutes) {
        orchestrator.advanceTime(minutes);
        updateTimeProperty();
    }
    
    /**
     * Inicia el avance automático del tiempo
     */
    public void startAutoAdvance() {
        if (autoAdvanceTimeline != null) {
            autoAdvanceTimeline.stop();
        }
        
        autoAdvanceTimeline = new Timeline(
            new KeyFrame(Duration.millis(autoAdvanceIntervalMs), e -> advanceTime(timeStepMinutes))
        );
        autoAdvanceTimeline.setCycleCount(Timeline.INDEFINITE);
        autoAdvanceTimeline.play();
        autoRunningProperty.set(true);
    }
    
    /**
     * Detiene el avance automático del tiempo
     */
    public void stopAutoAdvance() {
        if (autoAdvanceTimeline != null) {
            autoAdvanceTimeline.stop();
        }
        autoRunningProperty.set(false);
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
     * Actualiza la propiedad del tiempo actual
     */
    private void updateTimeProperty() {
        LocalDateTime currentTime = environment.getCurrentTime();
        currentTimeProperty.set(currentTime.format(DATE_TIME_FORMATTER));
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
     * Obtiene la propiedad observable del tiempo actual
     * 
     * @return Propiedad de cadena con el tiempo formateado
     */
    public StringProperty currentTimeProperty() {
        return currentTimeProperty;
    }
    
    /**
     * Obtiene la propiedad que indica si el avance automático está activo
     * 
     * @return Propiedad booleana que indica si está en auto-avance
     */
    public BooleanProperty autoRunningProperty() {
        return autoRunningProperty;
    }
    
    /**
     * Verifica si el avance automático está activo
     * 
     * @return true si el avance automático está activo
     */
    public boolean isAutoRunning() {
        return autoRunningProperty.get();
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
}
