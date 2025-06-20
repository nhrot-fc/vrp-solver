package com.vroute.ui;

import com.vroute.controllers.SimulationController;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Panel de control para la simulación con botones para avanzar
 * el tiempo y configurar la velocidad de simulación
 */
public class ControlPanel extends VBox {
    
    private final SimulationController simulationController;
    private final Label timeLabel;
    private Button playPauseButton;
    
    // Opciones de velocidad de simulación (minutos por tick)
    private final int[] timeStepOptions = {1, 5, 10, 15, 30, 60};
    
    /**
     * Constructor del panel de control
     * 
     * @param simulationController Controlador de la simulación
     */
    public ControlPanel(SimulationController simulationController) {
        this.simulationController = simulationController;
        
        setPadding(new Insets(10));
        setSpacing(10);
        
        // Panel superior con controles principales
        HBox mainControls = new HBox(10);
        
        // Etiqueta de tiempo
        timeLabel = new Label();
        timeLabel.textProperty().bind(simulationController.currentTimeProperty());
        timeLabel.setStyle("-fx-font-weight: bold;");
        
        // Botones de avance de tiempo manual
        Button advanceMinuteButton = new Button("Advance 1 Minute");
        Button advance10MinutesButton = new Button("Advance 10 Minutes");
        Button advanceHourButton = new Button("Advance 1 Hour");
        Button advanceDayButton = new Button("Advance 1 Day");
        
        // Configurar acciones de botones
        advanceMinuteButton.setOnAction(e -> simulationController.advanceTime(1));
        advance10MinutesButton.setOnAction(e -> simulationController.advanceTime(10));
        advanceHourButton.setOnAction(e -> simulationController.advanceTime(60));
        advanceDayButton.setOnAction(e -> simulationController.advanceTime(24 * 60));
        
        // Agregar botones al panel principal
        mainControls.getChildren().addAll(
            new Label("Current Time:"), 
            timeLabel,
            advanceMinuteButton,
            advance10MinutesButton,
            advanceHourButton,
            advanceDayButton
        );
        
        // Panel para controles de simulación automática
        HBox autoSimControls = createAutoSimulationControls();
        
        // Agregar paneles al contenedor principal
        getChildren().addAll(mainControls, autoSimControls);
    }
    
    /**
     * Crea el panel con controles para simulación automática
     * 
     * @return Panel HBox con los controles
     */
    private HBox createAutoSimulationControls() {
        HBox panel = new HBox(10);
        
        // Botón de reproducción/pausa
        playPauseButton = new Button("▶ Play");
        playPauseButton.setPrefWidth(100);
        
        // Selector de paso de tiempo
        Label timeStepLabel = new Label("Step Size:");
        ComboBox<Integer> timeStepCombo = new ComboBox<>();
        
        for (int step : timeStepOptions) {
            timeStepCombo.getItems().add(step);
        }
        
        timeStepCombo.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                if (object == 1) return "1 minute";
                if (object == 60) return "1 hour";
                return object + " minutes";
            }
            
            @Override
            public Integer fromString(String string) {
                return 0; // No se usa
            }
        });
        
        timeStepCombo.setValue(simulationController.getTimeStepMinutes());
        
        // Control deslizante para velocidad de simulación
        Label speedLabel = new Label("Speed:");
        Slider speedSlider = new Slider(100, 2000, 1000);
        speedSlider.setBlockIncrement(100);
        speedSlider.setMajorTickUnit(500);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setPrefWidth(200);
        
        // Invertir el valor para que a la derecha sea más rápido
        speedSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                double inverted = 2100 - object;
                if (inverted <= 500) return "Slower";
                if (inverted >= 1500) return "Faster";
                return "";
            }
            
            @Override
            public Double fromString(String string) {
                return 0.0; // No se usa
            }
        });
        
        // Configurar acciones
        playPauseButton.setOnAction(e -> toggleSimulation());
        
        timeStepCombo.setOnAction(e -> {
            int selectedStep = timeStepCombo.getValue();
            int intervalMs = (int)speedSlider.getValue();
            simulationController.setSimulationSpeed(selectedStep, intervalMs);
        });
        
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int selectedStep = timeStepCombo.getValue();
            int intervalMs = newVal.intValue();
            simulationController.setSimulationSpeed(selectedStep, intervalMs);
        });
        
        // Actualizar botón cuando cambia el estado de simulación
        simulationController.autoRunningProperty().addListener((obs, oldVal, newVal) -> 
            updatePlayPauseButton(newVal)
        );
        
        // Agregar controles al panel
        panel.getChildren().addAll(
            playPauseButton, 
            timeStepLabel, timeStepCombo, 
            speedLabel, speedSlider
        );
        
        return panel;
    }
    
    /**
     * Alterna entre iniciar y detener la simulación automática
     */
    private void toggleSimulation() {
        if (simulationController.isAutoRunning()) {
            simulationController.stopAutoAdvance();
        } else {
            simulationController.startAutoAdvance();
        }
    }
    
    /**
     * Actualiza el texto del botón de reproducción/pausa
     * 
     * @param isRunning Si la simulación está en marcha
     */
    private void updatePlayPauseButton(boolean isRunning) {
        if (isRunning) {
            playPauseButton.setText("⏸ Pause");
        } else {
            playPauseButton.setText("▶ Play");
        }
    }
}
