package com.vroute.ui;

import com.vroute.orchest.Event;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Panel para mostrar el registro de eventos ocurridos durante la simulación
 */
public class EventLogPanel extends VBox {
    
    private final VBox logBox;
    private int logCounter = 0;
    private static final int MAX_LOG_ENTRIES = 100;
    
    /**
     * Constructor del panel de registro de eventos
     */
    public EventLogPanel() {
        setPadding(new Insets(10));
        setPrefWidth(350);
        
        // Crear título del panel
        Label title = new Label("Event Log");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Crear contenedor de mensajes
        logBox = new VBox(2);
        logBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #ddd;");
        
        // Crear scroll para el contenedor de mensajes
        ScrollPane logScroll = new ScrollPane(logBox);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(500);
        
        // Agregar componentes al panel
        getChildren().addAll(title, logScroll);
    }
    
    /**
     * Agrega un evento al registro
     * 
     * @param event Evento a registrar
     */
    public void logEvent(Event event) {
        String eventInfo = event.toString();
        addToLog(eventInfo);
    }
    
    /**
     * Agrega un mensaje al registro
     * 
     * @param message Mensaje a agregar
     */
    public void addToLog(String message) {
        // Crear etiqueta con ajuste de línea
        Label logEntry = new Label(message);
        logEntry.setWrapText(true);
        logEntry.setMaxWidth(330);
        logEntry.setPadding(new Insets(2, 5, 2, 5));
        
        // Alternar colores de fondo para mejor legibilidad
        if (logCounter % 2 == 0) {
            logEntry.setStyle("-fx-background-color: #f0f0f0;");
        } else {
            logEntry.setStyle("-fx-background-color: #e8e8e8;");
        }
        logCounter++;
        
        // Agregar al principio del contenedor
        logBox.getChildren().add(0, logEntry);
        
        // Limitar cantidad de entradas
        if (logBox.getChildren().size() > MAX_LOG_ENTRIES) {
            logBox.getChildren().remove(MAX_LOG_ENTRIES);
        }
    }
    
    /**
     * Limpia todos los eventos del registro
     */
    public void clearLog() {
        logBox.getChildren().clear();
        logCounter = 0;
    }
}
