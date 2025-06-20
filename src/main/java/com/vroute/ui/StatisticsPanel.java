package com.vroute.ui;

import com.vroute.controllers.SimulationController;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.VehicleStatus;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

/**
 * Panel para mostrar estadísticas en tiempo real de la simulación
 */
public class StatisticsPanel extends TitledPane {
    
    private final SimulationController simulationController;
    private final GridPane statsGrid;
    private final Label orderStatsLabel;
    private final Label vehicleStatsLabel;
    private final Label overdueOrdersLabel;
    private final Label simulationSpeedLabel;
    
    // Mapeo de etiquetas para datos actualizables
    private final Map<String, Label> dataLabels = new HashMap<>();
    
    /**
     * Constructor del panel de estadísticas
     * 
     * @param simulationController Controlador de simulación
     */
    public StatisticsPanel(SimulationController simulationController) {
        this.simulationController = simulationController;
        
        // Configurar el TitledPane
        setText("Estadísticas de Simulación");
        setCollapsible(true);
        setExpanded(true);
        
        // Panel principal de estadísticas
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));
        
        // Crear grid para organizar estadísticas
        statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(5);
        
        // Inicializar etiquetas
        orderStatsLabel = createTitleLabel("Pedidos");
        vehicleStatsLabel = createTitleLabel("Vehículos");
        overdueOrdersLabel = createTitleLabel("Pedidos Vencidos");
        simulationSpeedLabel = createTitleLabel("Velocidad de Simulación");
        
        // Agregar etiquetas al grid
        int row = 0;
        statsGrid.add(orderStatsLabel, 0, row++, 2, 1);
        addStatItem("totalOrders", "Total:", "0", row++);
        addStatItem("pendingOrders", "Pendientes:", "0", row++);
        addStatItem("deliveredOrders", "Entregados:", "0", row++);
        
        row++;
        statsGrid.add(overdueOrdersLabel, 0, row++, 2, 1);
        addStatItem("overdueOrders", "Vencidos:", "0", row++);
        addStatItem("overduePercentage", "% Vencidos:", "0%", row++);
        
        row++;
        statsGrid.add(vehicleStatsLabel, 0, row++, 2, 1);
        addStatItem("totalVehicles", "Total:", "0", row++);
        addStatItem("availableVehicles", "Disponibles:", "0", row++);
        addStatItem("busyVehicles", "Ocupados:", "0", row++);
        addStatItem("maintenanceVehicles", "En mantenimiento:", "0", row++);
        
        row++;
        statsGrid.add(simulationSpeedLabel, 0, row++, 2, 1);
        addStatItem("timeStep", "Min/Tick:", "1", row++);
        addStatItem("interval", "Intervalo:", "1000ms", row++);
        
        // Botón para actualizar estadísticas manualmente
        Label updateInfoLabel = new Label("* Las estadísticas se actualizan automáticamente");
        updateInfoLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        
        // Agregar componentes al contenedor principal
        contentBox.getChildren().addAll(statsGrid, updateInfoLabel);
        
        // Establecer el contenido del TitledPane
        setContent(contentBox);
        
        // Actualizar datos iniciales
        updateStatistics();
        
        // Configurar actualización automática cuando cambia el tiempo
        simulationController.currentTimeProperty().addListener((obs, old, newVal) -> updateStatistics());
    }
    
    /**
     * Actualiza todas las estadísticas
     */
    public void updateStatistics() {
        Environment environment = simulationController.getEnvironment();
        
        // Estadísticas de pedidos
        int totalOrders = environment.getOrderQueue().size();
        long deliveredOrders = environment.getOrderQueue().stream()
                .filter(Order::isDelivered)
                .count();
        int pendingOrders = totalOrders - (int)deliveredOrders;
        int overdueOrders = environment.getOverdueOrders().size();
        double overduePercentage = (pendingOrders > 0) ? 
                (double)overdueOrders / pendingOrders * 100 : 0;
        
        // Estadísticas de vehículos
        int totalVehicles = environment.getVehicles().size();
        int availableVehicles = (int)environment.getVehicles().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                .count();
        int maintenanceVehicles = (int)environment.getVehicles().stream()
                .filter(v -> v.getStatus() == VehicleStatus.MAINTENANCE)
                .count();
        int busyVehicles = totalVehicles - availableVehicles - maintenanceVehicles;
        
        // Estadísticas de simulación
        int timeStep = simulationController.getTimeStepMinutes();
        int intervalMs = simulationController.getAutoAdvanceIntervalMs();
        
        // Actualizar valores
        updateStatValue("totalOrders", String.valueOf(totalOrders));
        updateStatValue("pendingOrders", String.valueOf(pendingOrders));
        updateStatValue("deliveredOrders", String.valueOf(deliveredOrders));
        
        updateStatValue("overdueOrders", String.valueOf(overdueOrders));
        updateStatValue("overduePercentage", String.format("%.1f%%", overduePercentage));
        
        updateStatValue("totalVehicles", String.valueOf(totalVehicles));
        updateStatValue("availableVehicles", String.valueOf(availableVehicles));
        updateStatValue("busyVehicles", String.valueOf(busyVehicles));
        updateStatValue("maintenanceVehicles", String.valueOf(maintenanceVehicles));
        
        updateStatValue("timeStep", timeStep + " min");
        updateStatValue("interval", intervalMs + " ms");
    }
    
    /**
     * Crea una etiqueta de título
     * 
     * @param text Texto de la etiqueta
     * @return Etiqueta formateada
     */
    private Label createTitleLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setPadding(new Insets(5, 0, 0, 0));
        return label;
    }
    
    /**
     * Agrega un elemento de estadística al grid
     * 
     * @param id Identificador para actualización
     * @param name Nombre de la estadística
     * @param initialValue Valor inicial
     * @param row Fila donde colocar en el grid
     */
    private void addStatItem(String id, String name, String initialValue, int row) {
        Label nameLabel = new Label(name);
        Label valueLabel = new Label(initialValue);
        
        nameLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        statsGrid.add(nameLabel, 0, row);
        statsGrid.add(valueLabel, 1, row);
        
        // Guardar referencia para actualización
        dataLabels.put(id, valueLabel);
    }
    
    /**
     * Actualiza el valor de una estadística
     * 
     * @param id Identificador de la estadística
     * @param value Nuevo valor
     */
    private void updateStatValue(String id, String value) {
        Label label = dataLabels.get(id);
        if (label != null) {
            label.setText(value);
        }
    }
}
