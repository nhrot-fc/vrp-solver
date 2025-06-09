package com.vroute.ui;

import com.vroute.models.Blockage;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.pathfinding.PathFinder;
import com.vroute.operation.ActionType;
import com.vroute.operation.VehicleAction;
import com.vroute.operation.VehiclePlan;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import java.util.Map;
import java.util.HashMap;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

public class MapRenderer {

    // Actualizo los colores para usar una paleta más acorde con Material Design
    private static final Color[] VEHICLE_COLORS = {
            Color.web("#2196F3"), // Azul
            Color.web("#4CAF50"), // Verde
            Color.web("#9C27B0"), // Púrpura
            Color.web("#FF9800"), // Naranja
            Color.web("#F44336"), // Rojo
            Color.web("#00BCD4"), // Cian
            Color.web("#FFEB3B"), // Amarillo
            Color.web("#E91E63"), // Rosa
            Color.web("#009688"), // Verde Azulado
            Color.web("#673AB7") // Morado
    };

    private static final Map<String, Color> vehicleColorMap = new HashMap<>();

    private static Color getVehicleColor(String vehicleId) {
        if (!vehicleColorMap.containsKey(vehicleId)) {
            int colorIndex = vehicleColorMap.size() % VEHICLE_COLORS.length;
            vehicleColorMap.put(vehicleId, VEHICLE_COLORS[colorIndex]);
        }
        return vehicleColorMap.get(vehicleId);
    }

    public static void drawGrid(Pane root, int gridWidth, int gridHeight, int cellSize) {
        Rectangle background = new Rectangle(0, 0, gridWidth * cellSize, gridHeight * cellSize);
        background.setFill(Color.web("#FAFAFA")); // Fondo ligeramente gris claro
        root.getChildren().add(background);

        // Dibujamos líneas horizontales
        for (int y = 0; y <= gridHeight; y++) {
            Line horizontalLine = new Line(
                    0, y * cellSize,
                    gridWidth * cellSize, y * cellSize);
            horizontalLine.setStroke(Color.web("#E0E0E0")); // Gris claro sutil
            horizontalLine.setStrokeWidth(1);
            root.getChildren().add(horizontalLine);
        }

        // Dibujamos líneas verticales
        for (int x = 0; x <= gridWidth; x++) {
            Line verticalLine = new Line(
                    x * cellSize, 0,
                    x * cellSize, gridHeight * cellSize);
            verticalLine.setStroke(Color.web("#E0E0E0")); // Gris claro sutil
            verticalLine.setStrokeWidth(1);
            root.getChildren().add(verticalLine);
        }

        // Dibujamos puntos en las intersecciones para los cruces
        for (int y = 0; y <= gridHeight; y++) {
            for (int x = 0; x <= gridWidth; x++) {
                Circle intersection = new Circle(
                        x * cellSize,
                        y * cellSize,
                        2 // Radio pequeño para los puntos de cruce
                );
                intersection.setFill(Color.web("#BDBDBD")); // Gris medio
                root.getChildren().add(intersection);
            }
        }
    }

    public static void drawPath(Pane root, PathFinder pathFinder, Position startPos, Position endPos,
            LocalDateTime timeContext, int cellSize) {
        List<Position> path = pathFinder.findPath(startPos, endPos, timeContext);
        if (path != null && !path.isEmpty()) {
            // Dibujamos las líneas entre posiciones consecutivas
            for (int i = 0; i < path.size() - 1; i++) {
                Position current = path.get(i);
                Position next = path.get(i + 1);

                // Crear una línea que conecta las dos posiciones
                Line pathLine = new Line(
                        current.getX() * cellSize,
                        current.getY() * cellSize,
                        next.getX() * cellSize,
                        next.getY() * cellSize);

                // Estilo simple tipo Material Design
                pathLine.setStroke(Color.web("#2196F3")); // Azul material
                pathLine.setStrokeWidth(3);
                pathLine.setOpacity(0.7);

                root.getChildren().add(pathLine);
            }
        }

        // Marcador para punto inicial
        Circle startNode = new Circle(
                startPos.getX() * cellSize,
                startPos.getY() * cellSize,
                6);
        startNode.setFill(Color.web("#4CAF50")); // Verde material
        startNode.setStroke(Color.WHITE);
        startNode.setStrokeWidth(1);

        // Marcador para punto final
        Circle endNode = new Circle(
                endPos.getX() * cellSize,
                endPos.getY() * cellSize,
                6);
        endNode.setFill(Color.web("#F44336")); // Rojo material
        endNode.setStroke(Color.WHITE);
        endNode.setStrokeWidth(1);

        root.getChildren().addAll(startNode, endNode);
    }

    public static void drawBlockages(Pane root, Environment environment, LocalDateTime timeContext, int cellSize) {
        for (Blockage blockage : environment.getActiveBlockagesAt(timeContext)) {
            List<Position> blockagePoints = blockage.getBlockagePoints();

            // Dibujamos los puntos de bloqueo
            for (Position blockedPos : blockagePoints) {
                if (environment.getGrid().isValidPosition(blockedPos)) {
                    // Marcador simple para puntos bloqueados
                    Circle blockNode = new Circle(
                            blockedPos.getX() * cellSize,
                            blockedPos.getY() * cellSize,
                            5);
                    blockNode.setFill(Color.web("#F44336")); // Rojo material
                    root.getChildren().add(blockNode);

                    // X para marcar el bloqueo de manera más sutil
                    double offset = 5;

                    Line line1 = new Line(
                            blockedPos.getX() * cellSize - offset,
                            blockedPos.getY() * cellSize - offset,
                            blockedPos.getX() * cellSize + offset,
                            blockedPos.getY() * cellSize + offset);

                    Line line2 = new Line(
                            blockedPos.getX() * cellSize + offset,
                            blockedPos.getY() * cellSize - offset,
                            blockedPos.getX() * cellSize - offset,
                            blockedPos.getY() * cellSize + offset);

                    line1.setStroke(Color.WHITE);
                    line1.setStrokeWidth(1.5);
                    line2.setStroke(Color.WHITE);
                    line2.setStrokeWidth(1.5);

                    root.getChildren().addAll(line1, line2);
                }
            }

            // Dibujamos líneas entre los puntos de bloqueo
            if (blockagePoints.size() > 1) {
                for (int i = 0; i < blockagePoints.size() - 1; i++) {
                    Position p1 = blockagePoints.get(i);
                    Position p2 = blockagePoints.get(i + 1);

                    if (environment.getGrid().isValidPosition(p1) && environment.getGrid().isValidPosition(p2)) {
                        Line blockageLine = new Line(
                                p1.getX() * cellSize,
                                p1.getY() * cellSize,
                                p2.getX() * cellSize,
                                p2.getY() * cellSize);

                        blockageLine.setStroke(Color.web("#F44336")); // Rojo material
                        blockageLine.setStrokeWidth(2);
                        blockageLine.getStrokeDashArray().addAll(5.0, 5.0);
                        blockageLine.setOpacity(0.8);
                        root.getChildren().add(blockageLine);
                    }
                }
            }
        }
    }

    public static void drawVehicles(Pane root, List<Vehicle> vehicles, int cellSize) {
        for (Vehicle vehicle : vehicles) {
            Position pos = vehicle.getCurrentPosition();

            // Usar el color asignado al vehículo
            Color vehicleColor = vehicleColorMap.containsKey(vehicle.getId()) ? getVehicleColor(vehicle.getId())
                    : Color.web("#FF9800");

            // Crear un círculo más visible para representar el vehículo
            Circle vehicleCircle = new Circle(
                    pos.getX() * cellSize,
                    pos.getY() * cellSize,
                    cellSize / 2.5); // Ligeramente más grande

            // Estilo limpio y visible
            vehicleCircle.setFill(vehicleColor);
            vehicleCircle.setStroke(Color.BLACK); // Borde negro para contraste
            vehicleCircle.setStrokeWidth(1.5);    // Borde más grueso

            // Añadir etiqueta con el ID del vehículo
            javafx.scene.text.Text idText = new javafx.scene.text.Text(
                    pos.getX() * cellSize - 8,
                    pos.getY() * cellSize + 4,
                    vehicle.getId());
            idText.setFill(Color.BLACK); // Texto negro para mejor legibilidad sobre colores claros
            idText.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 10));
            
            // Efecto para que el texto resalte
            javafx.scene.effect.DropShadow textShadow = new javafx.scene.effect.DropShadow();
            textShadow.setColor(Color.WHITE);
            textShadow.setRadius(1);
            textShadow.setOffsetX(1);
            textShadow.setOffsetY(1);
            idText.setEffect(textShadow);


            root.getChildren().addAll(vehicleCircle, idText);
        }
    }

    public static void drawOrders(Pane root, List<Order> orders, int cellSize) {
        LocalDateTime now = LocalDateTime.now();

        for (Order order : orders) {
            if (!order.isDelivered()) { // Solo dibujar órdenes pendientes
                Position pos = order.getPosition();

                // Determinar el color basado en la urgencia pero con paleta Material Design
                Color orderColor;

                if (order.isOverdue(now)) {
                    // Orden vencida
                    orderColor = Color.web("#F44336"); // Rojo material
                } else {
                    // Calcular el tiempo restante como porcentaje del tiempo total
                    long totalMinutes = Duration.between(
                            order.getArriveDate(),
                            order.getDueDate()).toMinutes();

                    long remainingMinutes = Duration.between(
                            now,
                            order.getDueDate()).toMinutes();

                    double percentRemaining = Math.max(0, Math.min(1, (double) remainingMinutes / totalMinutes));

                    if (percentRemaining > 0.66) {
                        orderColor = Color.web("#4CAF50"); // Verde material
                    } else if (percentRemaining > 0.33) {
                        orderColor = Color.web("#FF9800"); // Naranja material
                    } else {
                        orderColor = Color.web("#F44336"); // Rojo material
                    }
                }

                // Usar un rombo (diamante) para representar órdenes, un poco más grande
                javafx.scene.shape.Polygon orderShape = new javafx.scene.shape.Polygon(
                        pos.getX() * cellSize, pos.getY() * cellSize - 10, // Aumentado de 8 a 10
                        pos.getX() * cellSize + 10, pos.getY() * cellSize,
                        pos.getX() * cellSize, pos.getY() * cellSize + 10,
                        pos.getX() * cellSize - 10, pos.getY() * cellSize);

                orderShape.setFill(orderColor);
                orderShape.setStroke(Color.BLACK); // Borde negro para contraste
                orderShape.setStrokeWidth(1.5);    // Borde más grueso

                // Agregar un pequeño texto con el volumen de GLP
                javafx.scene.text.Text volumeText = new javafx.scene.text.Text(
                        pos.getX() * cellSize - 8,
                        pos.getY() * cellSize + 4,
                        String.format("%d", order.getRemainingGlpM3()));
                volumeText.setFill(Color.BLACK); // Texto negro
                volumeText.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 9));
                
                javafx.scene.effect.DropShadow orderTextShadow = new javafx.scene.effect.DropShadow();
                orderTextShadow.setColor(Color.WHITE);
                orderTextShadow.setRadius(1);
                orderTextShadow.setOffsetX(1);
                orderTextShadow.setOffsetY(1);
                volumeText.setEffect(orderTextShadow);

                root.getChildren().addAll(orderShape, volumeText);
            }
        }
    }

    public static void drawDepots(Pane root, List<Depot> depots, Depot mainDepot, int cellSize) {
        if (mainDepot != null) {
            Position pos = mainDepot.getPosition();

            // Símbolo simple para el depósito principal (cuadrado), más grande
            Rectangle mainDepotMarker = new Rectangle(
                    pos.getX() * cellSize - 12, // Aumentado de 10 a 12
                    pos.getY() * cellSize - 12,
                    24, // Aumentado de 20 a 24
                    24);

            mainDepotMarker.setFill(Color.web("#795548")); // Marrón material
            mainDepotMarker.setStroke(Color.BLACK); // Borde negro para contraste
            mainDepotMarker.setStrokeWidth(1.5);    // Borde más grueso
            mainDepotMarker.setRotate(45); // Girar para tener un rombo

            // Añadir etiqueta
            javafx.scene.text.Text depotLabel = new javafx.scene.text.Text(
                    pos.getX() * cellSize - 15,
                    pos.getY() * cellSize - 12,
                    "MAIN");
            depotLabel.setFill(Color.web("#212121")); // Casi negro
            depotLabel.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 9));

            root.getChildren().addAll(mainDepotMarker, depotLabel);
        }

        for (Depot depot : depots) {
            if (depot == mainDepot)
                continue; 

            Position pos = depot.getPosition();

            // Símbolo simple para depósitos (cuadrado más pequeño), más grande
            Rectangle depotRect = new Rectangle(
                    pos.getX() * cellSize - 10, // Aumentado de 8 a 10
                    pos.getY() * cellSize - 10,
                    20, // Aumentado de 16 a 20
                    20);

            depotRect.setFill(Color.web("#9E9E9E")); // Gris material
            depotRect.setStroke(Color.BLACK); // Borde negro para contraste
            depotRect.setStrokeWidth(1.5);    // Borde más grueso
            depotRect.setRotate(45); // Girar para tener un rombo

            // Añadir una etiqueta con el id corto
            String shortId = depot.getId().length() > 4 ? depot.getId().substring(0, 4) : depot.getId();
            javafx.scene.text.Text depotLabel = new javafx.scene.text.Text(
                    pos.getX() * cellSize - shortId.length() * 3,
                    pos.getY() * cellSize - 10,
                    shortId);
            depotLabel.setFill(Color.web("#212121")); // Casi negro
            depotLabel.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 9));

            root.getChildren().addAll(depotRect, depotLabel);
        }
    }

    public static void drawVehiclePlan(Pane root, VehiclePlan plan, int cellSize) {
        if (plan == null || plan.getActions().isEmpty())
            return;

        // Obtener un color único para este vehículo
        Color vehicleColor = getVehicleColor(plan.getVehicle().getId());

        for (VehicleAction action : plan.getActions()) {
            if (action.getType() == ActionType.DRIVING) {
                List<Position> pathSegments = action.getPath();
                if (pathSegments != null && pathSegments.size() > 1) {
                    for (int i = 0; i < pathSegments.size() - 1; i++) {
                        Position startPos = pathSegments.get(i);
                        Position endPos = pathSegments.get(i + 1);

                        if (startPos == null || endPos == null) {
                            System.err.println("Warning: Null position found in driving path for action: " + action);
                            continue; // Skip this segment
                        }

                        // Dibujar la línea de ruta
                        Line line = new Line(
                                startPos.getX() * cellSize,
                                startPos.getY() * cellSize,
                                endPos.getX() * cellSize,
                                endPos.getY() * cellSize);
                        line.setStroke(vehicleColor);
                        line.setStrokeWidth(2);
                        line.setOpacity(0.7);
                        line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

                        // Línea punteada para segmentos alternos
                        if (i % 2 == 0) {
                            line.getStrokeDashArray().addAll(5.0, 5.0);
                        }

                        root.getChildren().add(line);
                    }

                    // Pequeño marcador para el final del trayecto
                    Position lastPos = pathSegments.get(pathSegments.size() - 1);
                    Circle endMarker = new Circle(
                            lastPos.getX() * cellSize,
                            lastPos.getY() * cellSize,
                            4);
                    endMarker.setFill(vehicleColor);
                    endMarker.setStroke(Color.WHITE);
                    endMarker.setStrokeWidth(1);
                    root.getChildren().add(endMarker);
                }
            }
        }
    }
}
