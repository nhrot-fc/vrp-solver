package com.vroute.ui;

import com.vroute.controllers.SimulationController;
import com.vroute.models.*;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Panel para generar eventos personalizados en tiempo de ejecución
 */
public class CustomEventsPanel extends TitledPane {
    
    private final SimulationController simulationController;
    private final Environment environment;
    
    /**
     * Constructor del panel de eventos personalizados
     * 
     * @param simulationController Controlador de simulación
     */
    public CustomEventsPanel(SimulationController simulationController) {
        this.simulationController = simulationController;
        this.environment = simulationController.getEnvironment();
        
        // Configurar el TitledPane
        setText("Generador de Eventos");
        setCollapsible(true);
        setExpanded(false);
        
        // Panel principal
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));
        
        // Botones para diferentes tipos de eventos
        Button createOrderButton = new Button("Crear Pedido Aleatorio");
        Button createBlockageButton = new Button("Crear Bloqueo");
        Button createBreakdownButton = new Button("Simular Avería");
        Button refillDepotButton = new Button("Reabastecer Depósito");
        
        // Configurar acciones de los botones
        createOrderButton.setOnAction(e -> createRandomOrder());
        createBlockageButton.setOnAction(e -> createRandomBlockage());
        createBreakdownButton.setOnAction(e -> createVehicleBreakdown());
        refillDepotButton.setOnAction(e -> refillRandomDepot());
        
        // Agregar botones al panel
        contentBox.getChildren().addAll(
            createOrderButton,
            createBlockageButton,
            createBreakdownButton,
            refillDepotButton
        );
        
        // Establecer el contenido del TitledPane
        setContent(contentBox);
    }
    
    /**
     * Crea un pedido aleatorio
     */
    private void createRandomOrder() {
        // Generar posición aleatoria
        Random random = new Random();
        int x = random.nextInt(environment.getMainDepot().getPosition().getX() + 30) + 5;
        int y = random.nextInt(environment.getMainDepot().getPosition().getY() + 30) + 5;
        Position position = new Position(x, y);
        
        // Configurar diálogo para crear pedido
        Dialog<Order> dialog = new Dialog<>();
        dialog.setTitle("Crear Pedido");
        dialog.setHeaderText("Ingrese los detalles del pedido");
        
        // Botones
        ButtonType createButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField idField = new TextField("ORD-" + System.currentTimeMillis() % 10000);
        Spinner<Integer> glpSpinner = new Spinner<>(1, 100, 10);
        glpSpinner.setEditable(true);
        Spinner<Integer> hoursSpinner = new Spinner<>(1, 48, 12); // Hasta 48 horas (2 días)
        hoursSpinner.setEditable(true);
        
        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("GLP (m³):"), 0, 1);
        grid.add(glpSpinner, 1, 1);
        grid.add(new Label("Plazo (horas):"), 0, 2);
        grid.add(hoursSpinner, 1, 2);
        grid.add(new Label("Posición:"), 0, 3);
        grid.add(new Label(position.toString()), 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Configurar conversion del resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                String id = idField.getText();
                int glp = glpSpinner.getValue();
                int hours = hoursSpinner.getValue();
                
                LocalDateTime now = environment.getCurrentTime();
                LocalDateTime dueTime = now.plusHours(hours);
                
                return new Order(id, now, dueTime, glp, position);
            }
            return null;
        });
        
        Optional<Order> result = dialog.showAndWait();
        
        result.ifPresent(order -> {
            // Agregar orden al entorno
            environment.addOrder(order);
            
            // Crear evento de llegada al log y registrarlo
            simulationController.addEventListener(e -> {
                // Este listener se activará cuando el evento sea procesado
                System.out.println("Evento procesado: " + e.getType() + " - " + e.getEntityId());
            });
            // No se necesita almacenar la referencia del evento
            
            // Actualizar interfaz
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Pedido Creado");
            alert.setHeaderText(null);
            alert.setContentText("Se ha creado el pedido " + order.getId() + " con " + order.getGlpRequestM3() + " m³ de GLP.");
            alert.showAndWait();
        });
    }
    
    /**
     * Crea un bloqueo en una posición aleatoria
     */
    private void createRandomBlockage() {
        // Generar posición aleatoria
        Random random = new Random();
        int x = random.nextInt(environment.getMainDepot().getPosition().getX() + 50) + 5;
        int y = random.nextInt(environment.getMainDepot().getPosition().getY() + 50) + 5;
        
        // Configurar diálogo para crear bloqueo
        Dialog<Blockage> dialog = new Dialog<>();
        dialog.setTitle("Crear Bloqueo");
        dialog.setHeaderText("Ingrese los detalles del bloqueo");
        
        // Botones
        ButtonType createButtonType = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        
        // Campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
                
        // Spinners para duración
        Spinner<Integer> hoursSpinner = new Spinner<>(1, 72, 6); // Hasta 72 horas (3 días)
        hoursSpinner.setEditable(true);
        
        // Selector de posición para 4 puntos para definir un área rectangular de bloqueo
        Spinner<Integer> x1Spinner = new Spinner<>(0, 500, x);
        Spinner<Integer> y1Spinner = new Spinner<>(0, 500, y);
        Spinner<Integer> x2Spinner = new Spinner<>(0, 500, x + 10);
        Spinner<Integer> y2Spinner = new Spinner<>(0, 500, y);
        Spinner<Integer> x3Spinner = new Spinner<>(0, 500, x + 10);
        Spinner<Integer> y3Spinner = new Spinner<>(0, 500, y + 10);
        Spinner<Integer> x4Spinner = new Spinner<>(0, 500, x);
        Spinner<Integer> y4Spinner = new Spinner<>(0, 500, y + 10);
        
        x1Spinner.setEditable(true);
        y1Spinner.setEditable(true);
        x2Spinner.setEditable(true);
        y2Spinner.setEditable(true);
        x3Spinner.setEditable(true);
        y3Spinner.setEditable(true);
        x4Spinner.setEditable(true);
        y4Spinner.setEditable(true);
        
        grid.add(new Label("Duración (horas):"), 0, 0);
        grid.add(hoursSpinner, 1, 0);
        grid.add(new Label("Punto 1 (X,Y):"), 0, 1);
        grid.add(new HBox(5, x1Spinner, y1Spinner), 1, 1);
        grid.add(new Label("Punto 2 (X,Y):"), 0, 2);
        grid.add(new HBox(5, x2Spinner, y2Spinner), 1, 2);
        grid.add(new Label("Punto 3 (X,Y):"), 0, 3);
        grid.add(new HBox(5, x3Spinner, y3Spinner), 1, 3);
        grid.add(new Label("Punto 4 (X,Y):"), 0, 4);
        grid.add(new HBox(5, x4Spinner, y4Spinner), 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Configurar conversion del resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                int hours = hoursSpinner.getValue();
                
                // Crear una lista de puntos para el bloqueo
                List<Position> points = new ArrayList<>();
                points.add(new Position(x1Spinner.getValue(), y1Spinner.getValue()));
                points.add(new Position(x2Spinner.getValue(), y2Spinner.getValue()));
                points.add(new Position(x3Spinner.getValue(), y3Spinner.getValue()));
                points.add(new Position(x4Spinner.getValue(), y4Spinner.getValue()));
                // Cerrar el polígono
                points.add(new Position(x1Spinner.getValue(), y1Spinner.getValue()));
                
                LocalDateTime startTime = environment.getCurrentTime();
                LocalDateTime endTime = startTime.plusHours(hours);
                
                return new Blockage(startTime, endTime, points);
            }
            return null;
        });
        
        Optional<Blockage> result = dialog.showAndWait();
        
        result.ifPresent(blockage -> {
            // Agregar bloqueo al entorno
            environment.addBlockage(blockage);
            
            // Registrar listener para eventos
            simulationController.addEventListener(e -> {
                System.out.println("Evento de bloqueo procesado: " + e.getType());
            });
            
            // Crear eventos de inicio y fin de bloqueo (no necesitamos guardar referencias)
            
            // Notificar al usuario
            int hours = (int) java.time.Duration.between(blockage.getStartTime(), blockage.getEndTime()).toHours();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Bloqueo Creado");
            alert.setHeaderText(null);
            alert.setContentText("Se ha creado un bloqueo con duración de " + hours + " horas.");
            alert.showAndWait();
        });
    }
    
    /**
     * Simula una avería en un vehículo aleatorio
     */
    private void createVehicleBreakdown() {
        // Obtener vehículos disponibles
        ChoiceDialog<Vehicle> dialog = new ChoiceDialog<>();
        dialog.setTitle("Simular Avería");
        dialog.setHeaderText("Seleccione el vehículo que sufrirá una avería");
        dialog.setContentText("Vehículo:");
        
        // Filtrar solo vehículos disponibles
        environment.getVehicles().stream()
            .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
            .forEach(dialog.getItems()::add);
        
        if (!dialog.getItems().isEmpty()) {
            dialog.setSelectedItem(dialog.getItems().get(0));
            
            Optional<Vehicle> result = dialog.showAndWait();
            result.ifPresent(vehicle -> {
                // Obtener turno actual basado en la hora
                LocalDateTime currentTime = environment.getCurrentTime();
                Shift shift = Shift.getShiftForTime(currentTime.toLocalTime());
                
                // Crear incidente
                Incident incident = new Incident(vehicle.getId(), IncidentType.TI2, shift);
                incident.setOccurrenceTime(currentTime);
                incident.setLocation(vehicle.getCurrentPosition());
                
                // Marcar vehículo como no disponible
                vehicle.setStatus(VehicleStatus.UNAVAILABLE);
                
                // Registrar incidente en el entorno
                environment.addIncident(incident);
                
                // Crear evento y registrarlo
                simulationController.addEventListener(e -> {
                    // Este listener se activará cuando el evento sea procesado
                    System.out.println("Evento de avería procesado: " + e.getType() + " - " + e.getEntityId());
                });
                // Crear el evento pero no necesitamos almacenar la referencia
                
                // Notificar al usuario
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Avería Simulada");
                alert.setHeaderText(null);
                alert.setContentText("Se ha simulado una avería en el vehículo " + vehicle.getId() + ".");
                alert.showAndWait();
            });
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No hay vehículos disponibles");
            alert.setHeaderText(null);
            alert.setContentText("No hay vehículos disponibles para simular una avería.");
            alert.showAndWait();
        }
    }
    
    /**
     * Reabastece un depósito aleatorio
     */
    private void refillRandomDepot() {
        // Mostrar diálogo para seleccionar depósito
        ChoiceDialog<Depot> dialog = new ChoiceDialog<>();
        dialog.setTitle("Reabastecer Depósito");
        dialog.setHeaderText("Seleccione el depósito a reabastecer");
        dialog.setContentText("Depósito:");
        
        // Agregar todos los depósitos
        dialog.getItems().add(environment.getMainDepot());
        dialog.getItems().addAll(environment.getAuxDepots());
        dialog.setSelectedItem(environment.getMainDepot());
        
        Optional<Depot> result = dialog.showAndWait();
        result.ifPresent(depot -> {
            // Reabastecer el depósito
            depot.refillGLP();
            
            // Notificar al usuario
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Depósito Reabastecido");
            alert.setHeaderText(null);
            alert.setContentText("Se ha reabastecido el depósito " + depot.getId() + " con GLP.");
            alert.showAndWait();
        });
    }
}
