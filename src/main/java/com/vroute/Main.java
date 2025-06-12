package com.vroute;

import com.vroute.assignation.TabuSearchOptimizer;
import com.vroute.assignation.Solution;
import com.vroute.models.*;
import com.vroute.operation.VehiclePlan;
import com.vroute.pathfinding.Grid;
import com.vroute.ui.MapRenderer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main extends Application {
    private Environment environment;
    private Solution solution;
    private MapRenderer mapRenderer;

    @Override
    public void start(Stage primaryStage) {
        // Crear el entorno y generar la solución
        createEnvironment();
        runTabuSearchOptimization();

        // Configurar la interfaz gráfica
        BorderPane root = new BorderPane();
        Pane mapPane = new Pane();
        mapPane.setMinSize(700, 500);
        mapPane.setPrefSize(700, 500);

        mapRenderer = new MapRenderer();
        mapRenderer.renderEnvironment(mapPane, environment, 10);

        // Renderizar los resultados de la solución
        if (solution != null) {
            mapRenderer.renderSolution(mapPane, solution, environment.getCurrentTime(), 10);
        }

        // Panel de información
        VBox infoPanel = createInfoPanel();
        ScrollPane scrollPane = new ScrollPane(infoPanel);
        scrollPane.setFitToWidth(true);

        // Configurar el layout principal
        root.setCenter(mapPane);
        root.setRight(scrollPane);

        // Configurar la escena y mostrarla
        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setTitle("V-Route TabuSearch Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createInfoPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setMinWidth(300);

        // Título
        Label titleLabel = new Label("Información de Optimización");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");
        panel.getChildren().add(titleLabel);

        // Información de la solución
        if (solution != null) {
            panel.getChildren().add(new Label("Costo total: " + String.format("%.2f", solution.getTotalCost())));
            panel.getChildren().add(new Label("Órdenes no asignadas: " + solution.getUnassignedOrders().size()));

            // Información de vehículos
            Label vehiclesTitle = new Label("Planes de Vehículos:");
            vehiclesTitle.setStyle("-fx-font-weight: bold;");
            panel.getChildren().add(vehiclesTitle);

            for (Map.Entry<Vehicle, VehiclePlan> entry : solution.getVehiclePlans().entrySet()) {
                VehiclePlan plan = entry.getValue();
                Vehicle vehicle = entry.getKey();

                VBox vehicleBox = new VBox(5);
                vehicleBox.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 5;");

                vehicleBox.getChildren().add(new Label(vehicle.toString()));
                vehicleBox.getChildren()
                        .add(new Label("Distancia: " + String.format("%.2f km", plan.getTotalDistanceKm())));
                vehicleBox.getChildren().add(new Label("Duración: " + formatDuration(plan.getTotalDuration())));
                vehicleBox.getChildren().add(new Label("Factible: " + (plan.isFeasible() ? "Sí" : "No")));
                vehicleBox.getChildren().add(new Label("Acciones: " + plan.getActions().size()));

                panel.getChildren().add(vehicleBox);
            }

            // Órdenes no asignadas
            if (!solution.getUnassignedOrders().isEmpty()) {
                Label unassignedTitle = new Label("Órdenes No Asignadas:");
                unassignedTitle.setStyle("-fx-font-weight: bold;");
                panel.getChildren().add(unassignedTitle);

                for (Order order : solution.getUnassignedOrders()) {
                    panel.getChildren().add(new Label(order.toString()));
                }
            }
        } else {
            panel.getChildren().add(new Label("No se ha generado una solución"));
        }

        return panel;
    }

    private String formatDuration(java.time.Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);
    }

    private void createEnvironment() {
        // Crear el grid para el pathfinding
        Grid grid = new Grid(Constants.CITY_LENGTH_X, Constants.CITY_WIDTH_Y);

        // Crear depósitos
        Depot mainDepot = new Depot("D000", Constants.CENTRAL_STORAGE_LOCATION, 1000, true);
        mainDepot.refillGLP();

        List<Depot> auxDepots = new ArrayList<>();
        Depot northDepot = new Depot("D001", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 500, true);
        northDepot.refillGLP();
        Depot eastDepot = new Depot("D002", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 500, false);
        eastDepot.refillGLP();
        auxDepots.add(northDepot);
        auxDepots.add(eastDepot);

        // Crear vehículos
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle vehicle1 = new Vehicle("TA01", VehicleType.TA, mainDepot.getPosition());
        Vehicle vehicle2 = new Vehicle("TB02", VehicleType.TB, mainDepot.getPosition());
        vehicles.add(vehicle1);
        vehicles.add(vehicle2);

        // Crear el entorno con fecha actual
        LocalDateTime currentTime = LocalDateTime.now();
        environment = new Environment(grid, vehicles, mainDepot, auxDepots, currentTime);

        // Generar órdenes aleatorias
        Random random = new Random(42); // Semilla fija para reproducibilidad

        for (int i = 1; i <= 10; i++) {
            int x = random.nextInt(Constants.CITY_LENGTH_X);
            int y = random.nextInt(Constants.CITY_WIDTH_Y);
            Position position = new Position(x, y);

            int glpRequest = 5 + random.nextInt(10); // Entre 5 y 14 m³

            // Fecha de llegada entre ahora y 2 horas después
            LocalDateTime arriveDate = currentTime.plusMinutes(random.nextInt(120));
            // Fecha límite entre 4 y 10 horas después de la llegada
            LocalDateTime dueDate = arriveDate.plusHours(4 + random.nextInt(7));

            Order order = new Order(
                    String.format("O%03d", i),
                    arriveDate,
                    dueDate,
                    glpRequest,
                    position);

            environment.addOrder(order);
        }
    }

    private void runTabuSearchOptimization() {
        System.out.println("⏳ Ejecutando optimización con TabuSearch...");

        try {
            // Crear el optimizador con parámetros reducidos para prueba
            TabuSearchOptimizer optimizer = new TabuSearchOptimizer(500, 20, 50);

            // Ejecutar la optimización
            solution = optimizer.solve(environment);

            System.out.println("✅ Optimización completada");
            System.out.println("Costo total: " + solution.getTotalCost());
            System.out.println("Órdenes no asignadas: " + solution.getUnassignedOrders().size());

            // Mostrar detalles de los planes por vehículo
            for (Map.Entry<Vehicle, VehiclePlan> entry : solution.getVehiclePlans().entrySet()) {
                VehiclePlan plan = entry.getValue();
                Vehicle vehicle = entry.getKey();

                System.out.println("\nVehículo: " + vehicle.getId());
                System.out.println("Distancia: " + String.format("%.2f km", plan.getTotalDistanceKm()));
                System.out.println("Duración: " + formatDuration(plan.getTotalDuration()));
                System.out.println("Factible: " + (plan.isFeasible() ? "Sí" : "No"));
                System.out.println("Número de acciones: " + plan.getActions().size());
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error al ejecutar la optimización: " + e.getMessage());
            e.printStackTrace();

            // Crear una solución ficticia para visualización
            createDummySolution();
        }
    }

    private void createDummySolution() {
        System.out.println("⚙️ Creando solución ficticia para verificación visual...");

        Map<Vehicle, VehiclePlan> vehiclePlans = new HashMap<>();
        List<Order> unassignedOrders = new ArrayList<>();
        double totalCost = 0.0;

        // Asignar algunas órdenes a los vehículos
        List<Order> pendingOrders = environment.getPendingOrders();
        List<Vehicle> vehicles = environment.getVehicles();

        if (pendingOrders.size() >= 10 && vehicles.size() >= 2) {
            // Asignar 5 órdenes al primer vehículo
            List<Order> vehicle1Orders = pendingOrders.subList(0, 5);

            // Asignar 3 órdenes al segundo vehículo
            List<Order> vehicle2Orders = pendingOrders.subList(5, 8);

            // Dejar 2 órdenes sin asignar
            unassignedOrders.addAll(pendingOrders.subList(8, 10));

            // Crear planes de vehículos ficticios
            Vehicle vehicle1 = vehicles.get(0);
            Vehicle vehicle2 = vehicles.get(1);

            // Plan para vehículo 1
            VehiclePlan plan1 = createDummyVehiclePlan(vehicle1, vehicle1Orders);
            vehiclePlans.put(vehicle1, plan1);
            totalCost += 150.0; // Costo ficticio

            // Plan para vehículo 2
            VehiclePlan plan2 = createDummyVehiclePlan(vehicle2, vehicle2Orders);
            vehiclePlans.put(vehicle2, plan2);
            totalCost += 120.0; // Costo ficticio

            // Agregar penalización por órdenes no asignadas
            totalCost += unassignedOrders.size() * 50.0;
        } else {
            // Si no hay suficientes órdenes o vehículos, dejar todas sin asignar
            unassignedOrders.addAll(pendingOrders);
            totalCost = pendingOrders.size() * 50.0;
        }

        // Crear la solución ficticia
        solution = new Solution(vehiclePlans, unassignedOrders, totalCost);

        System.out.println("📊 Solución ficticia creada:");
        System.out.println("Costo total: " + totalCost);
        System.out.println("Órdenes asignadas: " + (pendingOrders.size() - unassignedOrders.size()));
        System.out.println("Órdenes no asignadas: " + unassignedOrders.size());
    }

    private VehiclePlan createDummyVehiclePlan(Vehicle vehicle, List<Order> orders) {
        // Este método crea un plan ficticio simplemente para visualización
        // En una implementación real, estos planes serían generados por el optimizador

        LocalDateTime startTime = environment.getCurrentTime();
        List<com.vroute.operation.Action> actions = new ArrayList<>();
        double totalDistance = 0.0;
        
        Position currentPosition = vehicle.getCurrentPosition();
        LocalDateTime currentTime = startTime;
        
        // Crear acciones ficticias para cada orden
        for (Order order : orders) {
            // Calcular distancia ficticia
            double distance = calculateDistance(currentPosition, order.getPosition());
            totalDistance += distance;
            
            // Estimar tiempo de viaje (asumiendo 60 km/h = 1 km/min)
            int travelMinutes = (int) Math.ceil(distance);
            LocalDateTime arriveTime = currentTime.plusMinutes(travelMinutes);
            
            // Crear acción de conducción ficticia
            com.vroute.operation.DriveAction driveAction = new com.vroute.operation.DriveAction(
                    currentTime, 
                    arriveTime, 
                    vehicle.clone(), 
                    new ArrayList<>(), // Ruta vacía
                    order, 
                    distance * 0.1); // Consumo ficticio de combustible
            
            actions.add(driveAction);
            
            // Crear acción de servicio ficticia
            LocalDateTime serveStartTime = arriveTime;
            LocalDateTime serveEndTime = serveStartTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
            
            com.vroute.operation.ServeAction serveAction = new com.vroute.operation.ServeAction(
                    serveStartTime,
                    serveEndTime,
                    vehicle.clone(),
                    order,
                    order.getRemainingGlpM3());
            
            actions.add(serveAction);
            
            // Actualizar posición y tiempo actual para la siguiente orden
            currentPosition = order.getPosition();
            currentTime = serveEndTime;
        }
        
        // Si tenemos más de 2 órdenes, agregar una visita a un depósito para recargar
        if (orders.size() > 2 && !actions.isEmpty()) {
            // Insertar una visita al depósito después de la segunda orden servida
            int insertIndex = 4; // Después de la 2a orden (2 acciones por orden)
            
            if (actions.size() >= insertIndex) {
                com.vroute.operation.Action prevAction = actions.get(insertIndex - 1);
                LocalDateTime refillStartTime = prevAction.getEndTime();
                Position lastPosition = prevAction.getDestination().getPosition();
                
                // Seleccionar un depósito (el principal por simplicidad)
                Depot depot = environment.getMainDepot();
                
                // Calcular distancia al depósito
                double distToDepot = calculateDistance(lastPosition, depot.getPosition());
                totalDistance += distToDepot;
                
                // Crear acción de conducción al depósito
                LocalDateTime arriveDepotTime = refillStartTime.plusMinutes((int) Math.ceil(distToDepot));
                
                com.vroute.operation.DriveAction driveToDepotAction = new com.vroute.operation.DriveAction(
                        refillStartTime, 
                        arriveDepotTime, 
                        vehicle.clone(), 
                        new ArrayList<>(), 
                        depot, 
                        distToDepot * 0.1);
                
                // Crear acción de recarga GLP
                LocalDateTime reloadStartTime = arriveDepotTime;
                LocalDateTime reloadEndTime = reloadStartTime.plusMinutes(Constants.VEHICLE_GLP_TRANSFER_DURATION_MINUTES);
                
                com.vroute.operation.ReloadAction reloadAction = new com.vroute.operation.ReloadAction(
                        reloadStartTime,
                        reloadEndTime,
                        vehicle.clone(),
                        depot,
                        15); // Cantidad ficticia GLP
                
                // Insertar acciones en la lista
                actions.add(insertIndex, driveToDepotAction);
                actions.add(insertIndex + 1, reloadAction);
                
                // Recalcular tiempos para las acciones restantes
                LocalDateTime newCurrentTime = reloadEndTime;
                for (int i = insertIndex + 2; i < actions.size(); i += 2) {
                    com.vroute.operation.DriveAction drive = (com.vroute.operation.DriveAction) actions.get(i);
                    Position fromPos = i == insertIndex + 2 ? depot.getPosition() : actions.get(i-2).getDestination().getPosition();
                    Position toPos = drive.getDestination().getPosition();
                    
                    double leg = calculateDistance(fromPos, toPos);
                    int legMinutes = (int) Math.ceil(leg);
                    
                    LocalDateTime newDriveStart = newCurrentTime;
                    LocalDateTime newDriveEnd = newDriveStart.plusMinutes(legMinutes);
                    
                    // Crear nueva acción de conducción con tiempos actualizados
                    com.vroute.operation.DriveAction updatedDrive = new com.vroute.operation.DriveAction(
                            newDriveStart, 
                            newDriveEnd, 
                            vehicle.clone(), 
                            new ArrayList<>(), 
                            drive.getDestination(), 
                            leg * 0.1);
                    
                    actions.set(i, updatedDrive);
                    
                    // Actualizar acción de servicio
                    com.vroute.operation.ServeAction serve = (com.vroute.operation.ServeAction) actions.get(i+1);
                    LocalDateTime newServeStart = newDriveEnd;
                    LocalDateTime newServeEnd = newServeStart.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
                    
                    com.vroute.operation.ServeAction updatedServe = new com.vroute.operation.ServeAction(
                            newServeStart,
                            newServeEnd,
                            vehicle.clone(),
                            (Order) serve.getDestination(),
                            serve.getGlpDischargedM3());
                    
                    actions.set(i+1, updatedServe);
                    
                    newCurrentTime = newServeEnd;
                }
            }
        }
        
        // Tiempo total estimado desde la primera hasta la última acción
        java.time.Duration totalDuration = java.time.Duration.ZERO;
        if (!actions.isEmpty()) {
            com.vroute.operation.Action firstAction = actions.get(0);
            com.vroute.operation.Action lastAction = actions.get(actions.size() - 1);
            totalDuration = java.time.Duration.between(firstAction.getStartTime(), lastAction.getEndTime());
        } else {
            totalDuration = java.time.Duration.ofMinutes((long) totalDistance);
        }

        return new VehiclePlan(
                vehicle,
                actions,
                startTime,
                true,
                totalDistance * 2.5, // Costo ficticio basado en distancia
                totalDistance,
                totalDuration);
    }

    private double calculateDistance(Position pos1, Position pos2) {
        int dx = pos1.getX() - pos2.getX();
        int dy = pos1.getY() - pos2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static void main(String[] args) {
        System.out.println("🚀 Starting VRoute TabuSearch Test...");
        launch(args);
    }
}
