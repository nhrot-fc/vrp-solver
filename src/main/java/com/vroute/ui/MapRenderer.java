package com.vroute.ui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.assignation.Solution;
import com.vroute.models.Blockage;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.operation.Action;
import com.vroute.operation.VehiclePlan;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class MapRenderer {

    private static final Color[] VEHICLE_COLORS = {
            Color.web("#2196F3"),
            Color.web("#4CAF50"),
            Color.web("#9C27B0"),
            Color.web("#FF9800"),
            Color.web("#F44336"),
            Color.web("#00BCD4"),
            Color.web("#FFEB3B"),
            Color.web("#E91E63"),
            Color.web("#009688"),
            Color.web("#673AB7")
    };

    private static final Map<String, Color> vehicleColorMap = new HashMap<>();

    private static Color getVehicleColor(String vehicleId) {
        if (!vehicleColorMap.containsKey(vehicleId)) {
            int colorIndex = vehicleColorMap.size() % VEHICLE_COLORS.length;
            vehicleColorMap.put(vehicleId, VEHICLE_COLORS[colorIndex]);
        }
        return vehicleColorMap.get(vehicleId);
    }

    public void renderEnvironment(Pane mapPane, Environment environment, int gridScale) {
        mapPane.getChildren().clear();

        // Dibujar fondo del mapa
        int gridWidth = environment.getGrid().getWidth();
        int gridHeight = environment.getGrid().getHeight();

        Rectangle background = new Rectangle(0, 0, gridWidth * gridScale, gridHeight * gridScale);
        background.setFill(Color.WHITE);
        mapPane.getChildren().add(background);

        // Dibujar grid
        for (int i = 0; i <= gridWidth; i++) {
            Line verticalLine = new Line(i * gridScale, 0, i * gridScale, gridHeight * gridScale);
            verticalLine.setStroke(Color.LIGHTGRAY);
            verticalLine.setStrokeWidth(0.5);
            mapPane.getChildren().add(verticalLine);
        }

        for (int j = 0; j <= gridHeight; j++) {
            Line horizontalLine = new Line(0, j * gridScale, gridWidth * gridScale, j * gridScale);
            horizontalLine.setStroke(Color.LIGHTGRAY);
            horizontalLine.setStrokeWidth(0.5);
            mapPane.getChildren().add(horizontalLine);
        }

        // Dibujar depósitos
        renderDepot(mapPane, environment.getMainDepot(), gridScale, true);

        for (Depot depot : environment.getAuxDepots()) {
            renderDepot(mapPane, depot, gridScale, false);
        }

        // Dibujar órdenes pendientes
        for (Order order : environment.getPendingOrders()) {
            renderOrder(mapPane, order, gridScale);
        }

        // Dibujar vehículos
        for (Vehicle vehicle : environment.getVehicles()) {
            renderVehicle(mapPane, vehicle, gridScale);
        }

        // Dibujar bloqueos activos
        for (Blockage blockage : environment.getActiveBlockagesAt(environment.getCurrentTime())) {
            renderBlockage(mapPane, blockage, gridScale);
        }
    }

    private void renderDepot(Pane mapPane, Depot depot, int gridScale, boolean isMain) {
        Position pos = depot.getPosition();
        int x = pos.getX() * gridScale;
        int y = pos.getY() * gridScale;

        // Dibujar depósito como cuadrado morado
        Rectangle depotRect = new Rectangle(x, y, gridScale, gridScale);
        depotRect.setFill(isMain ? Color.DARKMAGENTA : Color.PURPLE);
        depotRect.setStroke(Color.BLACK);
        depotRect.setStrokeWidth(1);

        mapPane.getChildren().add(depotRect);

        // Etiquetar depósito
        Text depotText = new Text(x, y - 5, depot.getId());
        depotText.setFill(Color.BLACK);
        depotText.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        mapPane.getChildren().add(depotText);
    }

    private void renderOrder(Pane mapPane, Order order, int gridScale) {
        Position pos = order.getPosition();
        int x = pos.getX() * gridScale;
        int y = pos.getY() * gridScale;

        // Dibujar punto de orden
        Circle orderCircle = new Circle(x + gridScale / 2, y + gridScale / 2, gridScale / 3);

        // Color basado en urgencia
        Color orderColor;
        if (order.isDelivered()) {
            orderColor = Color.LIGHTGREEN; // Orden entregada se mantiene verde claro
        } else if (order.isOverdue(LocalDateTime.now())) {
            orderColor = Color.RED; // Orden atrasada - rojo
        } else {
            orderColor = Color.YELLOW; // Orden pendiente - amarillo
        }

        orderCircle.setFill(orderColor);
        orderCircle.setStroke(Color.BLACK);
        orderCircle.setStrokeWidth(1);
        mapPane.getChildren().add(orderCircle);

        // Etiquetar orden
        Text orderText = new Text(x, y - 2, order.getId());
        orderText.setFill(Color.BLACK);
        orderText.setFont(Font.font("Arial", FontWeight.NORMAL, 8));
        mapPane.getChildren().add(orderText);
    }

    private void renderVehicle(Pane mapPane, Vehicle vehicle, int gridScale) {
        Position pos = vehicle.getCurrentPosition();
        int x = pos.getX() * gridScale;
        int y = pos.getY() * gridScale;

        // Obtener color específico para este vehículo
        Color vehicleColor = getVehicleColor(vehicle.getId());

        // Crear flecha para representar el vehículo
        double arrowSize = gridScale * 0.7;

        // Puntos para la flecha (triángulo)
        double[] pointsX = {
                x + gridScale / 2,
                x + gridScale / 2 - arrowSize / 2,
                x + gridScale / 2 + arrowSize / 2
        };

        double[] pointsY = {
                y + gridScale / 2 - arrowSize / 2,
                y + gridScale / 2 + arrowSize / 2,
                y + gridScale / 2 + arrowSize / 2
        };

        javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon();
        for (int i = 0; i < pointsX.length; i++) {
            arrow.getPoints().addAll(pointsX[i], pointsY[i]);
        }

        arrow.setFill(vehicleColor);
        arrow.setStroke(Color.BLACK);
        arrow.setStrokeWidth(1);
        mapPane.getChildren().add(arrow);

        // Etiquetar vehículo
        Text vehicleText = new Text(x, y, vehicle.getId());
        vehicleText.setFill(Color.BLACK);
        vehicleText.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        mapPane.getChildren().add(vehicleText);
    }

    private void renderBlockage(Pane mapPane, Blockage blockage, int gridScale) {
        List<Position> points = blockage.getBlockagePoints();

        // Dibujar líneas de bloqueo como líneas negras
        for (int i = 0; i < points.size() - 1; i++) {
            Position current = points.get(i);
            Position next = points.get(i + 1);

            Line blockageLine = new Line(
                    current.getX() * gridScale + gridScale / 2,
                    current.getY() * gridScale + gridScale / 2,
                    next.getX() * gridScale + gridScale / 2,
                    next.getY() * gridScale + gridScale / 2);

            blockageLine.setStroke(Color.BLACK);
            blockageLine.setStrokeWidth(2);
            mapPane.getChildren().add(blockageLine);
        }
    }

    public void renderSolution(Pane mapPane, Solution solution, LocalDateTime currentTime, int gridScale) {
        // Dibujar rutas para cada vehículo
        for (Map.Entry<Vehicle, VehiclePlan> entry : solution.getVehiclePlans().entrySet()) {
            Vehicle vehicle = entry.getKey();
            VehiclePlan plan = entry.getValue();

            Color vehicleColor = getVehicleColor(vehicle.getId());

            // Dibujar acciones del plan
            List<Action> actions = plan.getActions();
            if (!actions.isEmpty()) {
                renderVehiclePlan(mapPane, vehicle, plan, vehicleColor, gridScale);
            }
        }

        // Dibujar órdenes no asignadas con un indicador especial (círculo con borde
        // rojo)
        for (Order order : solution.getUnassignedOrders()) {
            Position pos = order.getPosition();
            int x = pos.getX() * gridScale;
            int y = pos.getY() * gridScale;

            // Dibujar un círculo con borde rojo para órdenes no asignadas
            Circle unassignedMarker = new Circle(x + gridScale / 2, y + gridScale / 2, gridScale / 3);
            unassignedMarker.setFill(Color.YELLOW);
            unassignedMarker.setStroke(Color.RED);
            unassignedMarker.setStrokeWidth(2);
            mapPane.getChildren().add(unassignedMarker);

            // Añadir texto para indicar que no está asignada
            Text unassignedText = new Text(x, y - 5, "N/A");
            unassignedText.setFill(Color.RED);
            unassignedText.setFont(Font.font("Arial", FontWeight.BOLD, 8));
            mapPane.getChildren().add(unassignedText);
        }
    }

    private void renderVehiclePlan(Pane mapPane, Vehicle vehicle, VehiclePlan plan, Color color, int gridScale) {
        List<Position> path = new ArrayList<>();

        // Extraer todos los puntos de posición de las acciones
        for (Action action : plan.getActions()) {
            Position actionPos = action.getDestination().getPosition();
            path.add(actionPos);
        }

        // Dibujar líneas de ruta
        for (int i = 0; i < path.size(); i++) {
            Position currentPos;
            Position nextPos;

            if (i == 0) {
                currentPos = vehicle.getCurrentPosition();
                nextPos = path.get(i);
            } else {
                currentPos = path.get(i - 1);
                nextPos = path.get(i);
            }

            Line routeLine = new Line(
                    currentPos.getX() * gridScale + gridScale / 2,
                    currentPos.getY() * gridScale + gridScale / 2,
                    nextPos.getX() * gridScale + gridScale / 2,
                    nextPos.getY() * gridScale + gridScale / 2);

            routeLine.setStroke(color);
            routeLine.setStrokeWidth(2);
            routeLine.getStrokeDashArray().addAll(5.0, 5.0);
            mapPane.getChildren().add(routeLine);

            // Dibujar flechas de dirección en cada segmento
            drawDirectionArrow(mapPane, currentPos, nextPos, color, gridScale);
        }
    }

    private void drawDirectionArrow(Pane mapPane, Position start, Position end, Color color, int gridScale) {
        // Calcular posiciones
        double startX = start.getX() * gridScale + gridScale / 2;
        double startY = start.getY() * gridScale + gridScale / 2;
        double endX = end.getX() * gridScale + gridScale / 2;
        double endY = end.getY() * gridScale + gridScale / 2;

        // Calcular el punto medio del segmento
        double midX = (startX + endX) / 2;
        double midY = (startY + endY) / 2;

        // Calcular la dirección
        double dirX = endX - startX;
        double dirY = endY - startY;
        double length = Math.sqrt(dirX * dirX + dirY * dirY);

        if (length < 1e-6)
            return; // Evitar división por cero

        dirX /= length;
        dirY /= length;

        // Puntas de flecha
        double arrowSize = gridScale / 3.0;

        // Calcular vector perpendicular
        double perpX = -dirY;
        double perpY = dirX;

        // Puntos para la punta de la flecha
        double arrowX1 = midX - dirX * arrowSize + perpX * arrowSize / 2;
        double arrowY1 = midY - dirY * arrowSize + perpY * arrowSize / 2;
        double arrowX2 = midX - dirX * arrowSize - perpX * arrowSize / 2;
        double arrowY2 = midY - dirY * arrowSize - perpY * arrowSize / 2;

        // Dibujar las líneas de la flecha
        Line line1 = new Line(midX, midY, arrowX1, arrowY1);
        Line line2 = new Line(midX, midY, arrowX2, arrowY2);

        line1.setStroke(color);
        line2.setStroke(color);
        line1.setStrokeWidth(1.5);
        line2.setStrokeWidth(1.5);

        mapPane.getChildren().addAll(line1, line2);
    }
}
