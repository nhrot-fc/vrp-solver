package com.vroute.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Blockage;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.operation.Action;
import com.vroute.operation.VehiclePlan;
import com.vroute.pathfinding.PathFinder;

import javafx.scene.effect.DropShadow;
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

    public static void drawGrid(Pane root, int gridWidth, int gridHeight, int cellSize) {
        Rectangle background = new Rectangle(0, 0, gridWidth * cellSize, gridHeight * cellSize);
        background.setFill(Color.web("#FAFAFA"));
        root.getChildren().add(background);

        for (int y = 0; y <= gridHeight; y++) {
            Line horizontalLine = new Line(
                    0, y * cellSize,
                    gridWidth * cellSize, y * cellSize);
            horizontalLine.setStroke(Color.web("#E0E0E0"));
            horizontalLine.setStrokeWidth(1);
            root.getChildren().add(horizontalLine);
        }

        for (int x = 0; x <= gridWidth; x++) {
            Line verticalLine = new Line(
                    x * cellSize, 0,
                    x * cellSize, gridHeight * cellSize);
            verticalLine.setStroke(Color.web("#E0E0E0"));
            verticalLine.setStrokeWidth(1);
            root.getChildren().add(verticalLine);
        }

        for (int y = 0; y <= gridHeight; y++) {
            for (int x = 0; x <= gridWidth; x++) {
                Circle intersection = new Circle(
                        x * cellSize,
                        y * cellSize,
                        2);
                intersection.setFill(Color.web("#BDBDBD"));
                root.getChildren().add(intersection);
            }
        }
    }

    public static void drawPath(Pane root, Environment environment, Position startPos, Position endPos,
            LocalDateTime timeContext, int cellSize) {
        List<Position> path = PathFinder.findPath(environment, startPos, endPos, timeContext);
        if (path != null && !path.isEmpty()) {
            for (int i = 0; i < path.size() - 1; i++) {
                Position current = path.get(i);
                Position next = path.get(i + 1);

                Line pathLine = new Line(
                        current.getX() * cellSize,
                        current.getY() * cellSize,
                        next.getX() * cellSize,
                        next.getY() * cellSize);

                pathLine.setStroke(Color.web("#2196F3"));
                pathLine.setStrokeWidth(3);
                pathLine.setOpacity(0.7);

                root.getChildren().add(pathLine);
            }
        }

        Circle startNode = new Circle(
                startPos.getX() * cellSize,
                startPos.getY() * cellSize,
                6);
        startNode.setFill(Color.web("#4CAF50"));
        startNode.setStroke(Color.WHITE);
        startNode.setStrokeWidth(1);

        Circle endNode = new Circle(
                endPos.getX() * cellSize,
                endPos.getY() * cellSize,
                6);
        endNode.setFill(Color.web("#F44336"));
        endNode.setStroke(Color.WHITE);
        endNode.setStrokeWidth(1);

        root.getChildren().addAll(startNode, endNode);
    }

    public static void drawBlockages(Pane root, Environment environment, LocalDateTime timeContext, int cellSize) {
        for (Blockage blockage : environment.getActiveBlockagesAt(timeContext)) {
            List<Position> blockagePoints = blockage.getBlockagePoints();

            for (Position blockedPos : blockagePoints) {
                if (environment.getGrid().isValidPosition(blockedPos)) {
                    Circle blockNode = new Circle(
                            blockedPos.getX() * cellSize,
                            blockedPos.getY() * cellSize,
                            5);
                    blockNode.setFill(Color.web("#F44336"));
                    root.getChildren().add(blockNode);

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

                        blockageLine.setStroke(Color.web("#F44336"));
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

            Color vehicleColor = vehicleColorMap.containsKey(vehicle.getId()) ? getVehicleColor(vehicle.getId())
                    : Color.web("#FF9800");

            Circle vehicleCircle = new Circle(
                    pos.getX() * cellSize,
                    pos.getY() * cellSize,
                    cellSize / 2.5);

            vehicleCircle.setFill(vehicleColor);
            vehicleCircle.setStroke(Color.BLACK);
            vehicleCircle.setStrokeWidth(1.5);

            Text idText = new Text(
                    pos.getX() * cellSize - 8,
                    pos.getY() * cellSize + 4,
                    vehicle.getId());
            idText.setFill(Color.BLACK);
            idText.setFont(Font.font("Arial", FontWeight.BOLD, 10));

            DropShadow textShadow = new DropShadow();
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
            if (!order.isDelivered()) {
                Position pos = order.getPosition();

                Color orderColor;

                if (order.isOverdue(now)) {
                    orderColor = Color.web("#F44336");
                } else {
                    long totalMinutes = Duration.between(
                            order.getArriveDate(),
                            order.getDueDate()).toMinutes();

                    long remainingMinutes = Duration.between(
                            now,
                            order.getDueDate()).toMinutes();

                    double percentRemaining = Math.max(0, Math.min(1, (double) remainingMinutes / totalMinutes));

                    if (percentRemaining > 0.66) {
                        orderColor = Color.web("#4CAF50");
                    } else if (percentRemaining > 0.33) {
                        orderColor = Color.web("#FF9800");
                    } else {
                        orderColor = Color.web("#F44336");
                    }
                }

                javafx.scene.shape.Polygon orderShape = new javafx.scene.shape.Polygon(
                        pos.getX() * cellSize, pos.getY() * cellSize - 10,
                        pos.getX() * cellSize + 10, pos.getY() * cellSize,
                        pos.getX() * cellSize, pos.getY() * cellSize + 10,
                        pos.getX() * cellSize - 10, pos.getY() * cellSize);

                orderShape.setFill(orderColor);
                orderShape.setStroke(Color.BLACK);
                orderShape.setStrokeWidth(1.5);

                Text volumeText = new Text(
                        pos.getX() * cellSize - 8,
                        pos.getY() * cellSize + 4,
                        String.format("%d", order.getRemainingGlpM3()));
                volumeText.setFill(Color.BLACK);
                volumeText.setFont(Font.font("Arial", FontWeight.BOLD, 9));

                DropShadow orderTextShadow = new DropShadow();
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

            Rectangle mainDepotMarker = new Rectangle(
                    pos.getX() * cellSize - 12,
                    pos.getY() * cellSize - 12,
                    24,
                    24);

            mainDepotMarker.setFill(Color.web("#795548"));
            mainDepotMarker.setStroke(Color.BLACK);
            mainDepotMarker.setStrokeWidth(1.5);
            mainDepotMarker.setRotate(45);

            Text depotLabel = new Text(
                    pos.getX() * cellSize - 15,
                    pos.getY() * cellSize - 12,
                    "MAIN");
            depotLabel.setFill(Color.web("#212121"));
            depotLabel.setFont(Font.font("Arial", FontWeight.BOLD, 9));

            root.getChildren().addAll(mainDepotMarker, depotLabel);
        }

        for (Depot depot : depots) {
            if (depot == mainDepot)
                continue;

            Position pos = depot.getPosition();

            Rectangle depotRect = new Rectangle(
                    pos.getX() * cellSize - 10,
                    pos.getY() * cellSize - 10,
                    20,
                    20);

            depotRect.setFill(Color.web("#9E9E9E"));
            depotRect.setStroke(Color.BLACK);
            depotRect.setStrokeWidth(1.5);
            depotRect.setRotate(45);

            String shortId = depot.getId().length() > 4 ? depot.getId().substring(0, 4) : depot.getId();
            Text depotLabel = new Text(
                    pos.getX() * cellSize - shortId.length() * 3,
                    pos.getY() * cellSize - 10,
                    shortId);
            depotLabel.setFill(Color.web("#212121"));
            depotLabel.setFont(Font.font("Arial", FontWeight.BOLD, 9));

            root.getChildren().addAll(depotRect, depotLabel);
        }
    }

    public static void drawVehiclePlan(Pane root, VehiclePlan plan, int cellSize) {
        if (plan == null || plan.getActions().isEmpty())
            return;

        Color vehicleColor = getVehicleColor(plan.getVehicle().getId());
        Position currentPos = plan.getVehicle().getCurrentPosition();
        
        // To connect all actions visually
        List<Position> allWaypoints = new ArrayList<>();
        allWaypoints.add(currentPos);
        
        // Process all actions and draw route segments for each action type
        for (Action action : plan.getActions()) {
            Position actionEndPos = action.getDestination();
            
            switch (action.getType()) {
                case DRIVE:
                    List<Position> pathSegments = action.getPath();
                    if (pathSegments != null && pathSegments.size() > 1) {
                        // Add all waypoints to our complete path
                        allWaypoints.addAll(pathSegments.subList(1, pathSegments.size()));
                        
                        // Draw the actual path with dashed lines
                        for (int i = 0; i < pathSegments.size() - 1; i++) {
                            Position startPos = pathSegments.get(i);
                            Position endPos = pathSegments.get(i + 1);

                            if (startPos == null || endPos == null) {
                                System.err.println("Warning: Null position found in driving path for action: " + action);
                                continue;
                            }

                            Line line = new Line(
                                    startPos.getX() * cellSize,
                                    startPos.getY() * cellSize,
                                    endPos.getX() * cellSize,
                                    endPos.getY() * cellSize);
                            line.setStroke(vehicleColor);
                            line.setStrokeWidth(2);
                            line.setOpacity(0.7);
                            line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

                            if (i % 2 == 0) {
                                line.getStrokeDashArray().addAll(5.0, 5.0);
                            }

                            root.getChildren().add(line);
                        }
                    }
                    break;
                
                default:
                    // For non-driving actions, connect to the previous position if needed
                    if (actionEndPos != null && !currentPos.equals(actionEndPos)) {
                        allWaypoints.add(actionEndPos);
                    }
                    break;
            }
            
            // Add appropriate icon for the action type at the end position
            if (actionEndPos != null) {
                Circle actionMarker = new Circle(
                        actionEndPos.getX() * cellSize,
                        actionEndPos.getY() * cellSize,
                        6);
                
                // Different colors/styles for different action types
                switch (action.getType()) {
                    case REFUEL:
                        actionMarker.setFill(Color.YELLOW);
                        actionMarker.setStroke(vehicleColor);
                        break;
                    case RELOAD:
                        actionMarker.setFill(Color.LIGHTGREEN);
                        actionMarker.setStroke(vehicleColor);
                        break;
                    case SERVE:
                        actionMarker.setFill(Color.ORANGE);
                        actionMarker.setStroke(vehicleColor);
                        break;
                    case MAINTENANCE:
                        actionMarker.setFill(Color.LIGHTBLUE);
                        actionMarker.setStroke(vehicleColor);
                        break;
                    case WAIT:
                        actionMarker.setFill(Color.LIGHTGRAY);
                        actionMarker.setStroke(vehicleColor);
                        break;
                    default:
                        actionMarker.setFill(vehicleColor);
                        actionMarker.setStroke(Color.WHITE);
                        break;
                }
                
                actionMarker.setStrokeWidth(1.5);
                root.getChildren().add(actionMarker);
                
                // Add a small text indicator for the action type
                String actionSymbol = "";
                switch (action.getType()) {
                    case REFUEL: actionSymbol = "⛽"; break;
                    case RELOAD: actionSymbol = "🛢️"; break;
                    case SERVE: actionSymbol = "🛒"; break;
                    case MAINTENANCE: actionSymbol = "🔧"; break;
                    case WAIT: actionSymbol = "⏸️"; break;
                    default: break;
                }
                
                if (!actionSymbol.isEmpty()) {
                    Text actionText = new Text(
                            actionEndPos.getX() * cellSize - 4,
                            actionEndPos.getY() * cellSize + 4,
                            actionSymbol);
                    actionText.setFill(Color.BLACK);
                    actionText.setFont(Font.font("Arial", FontWeight.BOLD, 9));
                    root.getChildren().add(actionText);
                }
                
                // Update current position for the next action
                currentPos = actionEndPos;
            }
        }
        
        // Draw connected path for the entire route with a thinner line
        if (allWaypoints.size() > 1) {
            for (int i = 0; i < allWaypoints.size() - 1; i++) {
                Position startPos = allWaypoints.get(i);
                Position endPos = allWaypoints.get(i + 1);
                
                Line connectionLine = new Line(
                        startPos.getX() * cellSize,
                        startPos.getY() * cellSize,
                        endPos.getX() * cellSize,
                        endPos.getY() * cellSize);
                connectionLine.setStroke(vehicleColor.deriveColor(0, 1, 1, 0.3));
                connectionLine.setStrokeWidth(1);
                
                // Set this as path underneath the other elements
                connectionLine.setViewOrder(100);
                root.getChildren().add(connectionLine);
            }
        }
    }
}
