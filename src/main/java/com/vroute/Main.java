package com.vroute;

import com.vroute.controllers.SimulationController;
import com.vroute.models.*;
import com.vroute.orchest.*;
import com.vroute.setup.EnvironmentSetup;
import com.vroute.ui.ControlPanel;
import com.vroute.ui.EventLogPanel;
import com.vroute.ui.MapView;
import com.vroute.ui.StatisticsPanel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;

/**
 * Clase principal de la aplicación V-Route Simulator
 */
public class Main extends Application {

    private static final int GRID_WIDTH = 70;
    private static final int GRID_HEIGHT = 50;
    private static final int CELL_SIZE = 20;
    
    private Environment environment;
    private Orchestrator orchestrator;
    private SimulationController simulationController;
    private MapView mapView;

    @Override
    public void start(Stage primaryStage) {
        // Inicializar el entorno y orquestrador
        initializeSimulation();
        
        // Crear el controlador de simulación
        simulationController = new SimulationController(orchestrator);
        
        // Crear la interfaz de usuario
        BorderPane root = new BorderPane();
        
        // Crear la vista del mapa
        mapView = new MapView(environment, GRID_WIDTH, GRID_HEIGHT, CELL_SIZE);
        root.setCenter(mapView);
        
        // Crear panel de control
        ControlPanel controlPanel = new ControlPanel(simulationController);
        root.setBottom(controlPanel);
        
        // Crear panel de eventos
        EventLogPanel eventLogPanel = new EventLogPanel();
        
        // Crear panel de estadísticas
        StatisticsPanel statsPanel = new StatisticsPanel(simulationController);
        
        // Panel lateral derecho con eventos y estadísticas
        VBox rightPanel = new VBox(10);
        rightPanel.getChildren().addAll(statsPanel, eventLogPanel);
        root.setRight(rightPanel);
        
        // Registrar listener de eventos
        simulationController.addEventListener(event -> {
            eventLogPanel.logEvent(event);
            mapView.updateMap(); // Actualizar el mapa cuando ocurre un evento
        });
        
        // Configurar la escena
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("V-Route Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Inicializa el entorno y orquestrador para la simulación
     */
    private void initializeSimulation() {
        LocalDateTime startDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        EnvironmentSetup setup = new EnvironmentSetup();
        environment = setup.createEnvironment(startDate);
        orchestrator = setup.createOrchestrator(environment);
    }
    
    /**
     * Método principal para iniciar la aplicación
     * 
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }
}
