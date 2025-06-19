package com.vroute.ui;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Blockage;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleStatus;
import com.vroute.operation.Action;
import com.vroute.operation.ActionType;
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
            Color.web("#2196F3"), // Blue
            Color.web("#4CAF50"), // Green
            Color.web("#9C27B0"), // Purple
            Color.web("#FF9800"), // Orange
            Color.web("#F44336"), // Red
            Color.web("#00BCD4"), // Cyan
            Color.web("#FFEB3B"), // Yellow
            Color.web("#E91E63"), // Pink
            Color.web("#009688"), // Teal
            Color.web("#673AB7") // Deep Purple
    };

    private static final Color MAIN_DEPOT_COLOR = Color.DARKBLUE; // Main deep blue
    private static final Color AUX_DEPOT_COLOR = Color.BLUE; // Aux blue
    private static final Color NO_GAS_GLP_COLOR = Color.GRAY; // No gas GLP gray
    private static final Color ORDER_COLOR = Color.ORANGE; // Orders orange
    private static final Color LATE_ORDER_COLOR = Color.RED; // Late orders red
    private static final Color BLOCKAGE_COLOR = Color.BLACK; // Blockages black
    private static final Color GRID_COLOR = Color.GRAY; // Grid gray
    private static final Color VEHICLE_LABEL_COLOR = Color.BLACK; // Vehicle label color

    public static void drawGrid(Pane pane, int width, int height, int cellSize) {
        // Set background color
        pane.setStyle("-fx-background-color: white;");
        
        // Draw horizontal lines
        for (int y = 0; y <= height; y++) {
            Line line = new Line(0, y * cellSize, width * cellSize, y * cellSize);
            line.setStroke(GRID_COLOR);
            line.setStrokeWidth(0.5);
            pane.getChildren().add(line);
        }

        // Draw vertical lines
        for (int x = 0; x <= width; x++) {
            Line line = new Line(x * cellSize, 0, x * cellSize, height * cellSize);
            line.setStroke(GRID_COLOR);
            line.setStrokeWidth(0.5);
            pane.getChildren().add(line);
        }
    }

    public static void drawDepots(Pane pane, List<Depot> auxDepots, Depot mainDepot, int cellSize) {
        // Draw main depot
        drawDepot(pane, mainDepot, MAIN_DEPOT_COLOR, cellSize, true);

        // Draw auxiliary depots
        for (Depot depot : auxDepots) {
            drawDepot(pane, depot, AUX_DEPOT_COLOR, cellSize, false);
        }
    }

    private static void drawDepot(Pane pane, Depot depot, Color color, int cellSize, boolean isMain) {
        Position pos = depot.getPosition();
        double size = cellSize * 0.8;

        // Check if the depot can provide fuel and has GLP
        Color fillColor = color;
        if (!depot.canRefuel() && depot.getCurrentGlpM3() == 0) {
            fillColor = NO_GAS_GLP_COLOR;
        }
        
        Rectangle rect = new Rectangle(
                pos.getX() * cellSize + (cellSize - size) / 2,
                pos.getY() * cellSize + (cellSize - size) / 2,
                size, size);

        rect.setFill(fillColor);
        
        pane.getChildren().add(rect);

        // Add depot label
        Text text = new Text(pos.getX() * cellSize + cellSize / 2,
                pos.getY() * cellSize + cellSize / 2 + 4,
                isMain ? "M" : "A");
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Arial", FontWeight.BOLD, cellSize * 0.5));
        text.setTranslateX(-text.getLayoutBounds().getWidth() / 2);
        pane.getChildren().add(text);

        // Add depot ID as tooltip
        Text idText = new Text(pos.getX() * cellSize + cellSize / 2,
                (pos.getY() + 1) * cellSize + 4,
                depot.getId());
        idText.setFill(color.darker());
        idText.setFont(Font.font("Arial", FontWeight.NORMAL, cellSize * 0.3));
        idText.setTranslateX(-idText.getLayoutBounds().getWidth() / 2);
        pane.getChildren().add(idText);
        
        // Add GLP level label
        Text glpText = new Text(pos.getX() * cellSize + cellSize / 2,
                (pos.getY() + 1) * cellSize + cellSize * 0.5,
                String.format("GLP: %d/%d", depot.getCurrentGlpM3(), depot.getGlpCapacityM3()));
        glpText.setFill(Color.BLACK);
        glpText.setFont(Font.font("Arial", FontWeight.NORMAL, cellSize * 0.25));
        glpText.setTranslateX(-glpText.getLayoutBounds().getWidth() / 2);
        pane.getChildren().add(glpText);
        
        // Add refuel capability indicator if applicable
        if (depot.canRefuel()) {
            Text fuelText = new Text(pos.getX() * cellSize + cellSize / 2,
                    (pos.getY() + 1) * cellSize + cellSize * 0.8,
                    "⛽ Fuel");
            fuelText.setFill(Color.DARKGREEN);
            fuelText.setFont(Font.font("Arial", FontWeight.NORMAL, cellSize * 0.25));
            fuelText.setTranslateX(-fuelText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(fuelText);
        }
    }

    public static void drawOrders(Pane pane, List<Order> orders, Environment environment, int cellSize) {
        LocalDateTime currentTime = environment.getCurrentTime();
        
        for (Order order : orders) {
            if (order.isDelivered()) {
                continue; // Skip delivered orders
            }

            Position pos = order.getPosition();
            
            boolean isLate = order.isOverdue(currentTime);
            
            // Use different shapes for regular and late orders
            if (!isLate) {
                // Regular orders as orange dots
                double radius = cellSize * 0.25;
                Circle circle = new Circle(
                        pos.getX() * cellSize + cellSize / 2,
                        pos.getY() * cellSize + cellSize / 2,
                        radius);
                circle.setFill(ORDER_COLOR);
                pane.getChildren().add(circle);
            } else {
                // Late orders as red squares (fists)
                double size = cellSize * 0.4;
                Rectangle rect = new Rectangle(
                        pos.getX() * cellSize + (cellSize - size) / 2,
                        pos.getY() * cellSize + (cellSize - size) / 2,
                        size, size);
                rect.setFill(LATE_ORDER_COLOR);
                pane.getChildren().add(rect);
            }

            // Add order quantity text
            Text text = new Text(pos.getX() * cellSize + cellSize / 2,
                    pos.getY() * cellSize + cellSize / 2 + 4,
                    String.valueOf(order.getRemainingGlpM3()));
            text.setFill(Color.WHITE);
            text.setFont(Font.font("Arial", FontWeight.BOLD, cellSize * 0.3));
            text.setTranslateX(-text.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(text);

            // Add order ID as tooltip
            Text idText = new Text(pos.getX() * cellSize + cellSize / 2,
                    (pos.getY() + 1) * cellSize + 4,
                    order.getId().substring(0, Math.min(order.getId().length(), 8)));
            idText.setFill(Color.BLACK);
            idText.setFont(Font.font("Arial", FontWeight.NORMAL, cellSize * 0.25));
            idText.setTranslateX(-idText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(idText);
            
            // Add GLP requirement label
            Text glpText = new Text(pos.getX() * cellSize + cellSize / 2,
                    (pos.getY() + 1) * cellSize + cellSize * 0.4,
                    String.format("GLP: %d/%d", order.getRemainingGlpM3(), order.getGlpRequestM3()));
            glpText.setFill(Color.BLACK);
            glpText.setFont(Font.font("Arial", FontWeight.NORMAL, cellSize * 0.25));
            glpText.setTranslateX(-glpText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(glpText);
            
            // Add due time label with color coding
            String timeLabel = order.getDueTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            Text timeText = new Text(pos.getX() * cellSize + cellSize / 2,
                    (pos.getY() + 1) * cellSize + cellSize * 0.7,
                    "Due: " + timeLabel);
            timeText.setFill(isLate ? LATE_ORDER_COLOR : Color.BLACK);
            timeText.setFont(Font.font("Arial", isLate ? FontWeight.BOLD : FontWeight.NORMAL, cellSize * 0.25));
            timeText.setTranslateX(-timeText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(timeText);
        }
    }

    public static void drawBlockages(Pane pane, Environment environment, LocalDateTime currentTime, int cellSize) {
        List<Blockage> activeBlockages = environment.getActiveBlockagesAt(currentTime);

        for (Blockage blockage : activeBlockages) {
            List<Position> blockagePoints = blockage.getLines();

            // Only draw lines connecting blockage points (black lines)
            for (int i = 0; i < blockagePoints.size() - 1; i++) {
                Position p1 = blockagePoints.get(i);
                Position p2 = blockagePoints.get(i + 1);

                Line line = new Line(
                        p1.getX() * cellSize + cellSize / 2,
                        p1.getY() * cellSize + cellSize / 2,
                        p2.getX() * cellSize + cellSize / 2,
                        p2.getY() * cellSize + cellSize / 2);

                line.setStroke(BLOCKAGE_COLOR);
                line.setStrokeWidth(2);
                pane.getChildren().add(line);
            }
            
            // Mark each blockage point with a small dot
            for (Position pos : blockagePoints) {
                Circle dot = new Circle(
                        pos.getX() * cellSize + cellSize / 2,
                        pos.getY() * cellSize + cellSize / 2,
                        cellSize * 0.15);
                
                dot.setFill(BLOCKAGE_COLOR);
                pane.getChildren().add(dot);
            }
        }
    }

    public static void drawVehicles(Pane pane, List<Vehicle> vehicles, int cellSize) {
        Map<String, Integer> vehicleColorMap = new HashMap<>();
        int colorIndex = 0;

        for (Vehicle vehicle : vehicles) {
            Position pos = vehicle.getCurrentPosition();

            // Get consistent color for each vehicle based on ID
            if (!vehicleColorMap.containsKey(vehicle.getId())) {
                vehicleColorMap.put(vehicle.getId(), colorIndex % VEHICLE_COLORS.length);
                colorIndex++;
            }

            Color color = VEHICLE_COLORS[vehicleColorMap.get(vehicle.getId())];

            // Adjust color based on vehicle status
            if (vehicle.getStatus() != VehicleStatus.AVAILABLE && 
                vehicle.getStatus() != VehicleStatus.DRIVING) {
                color = color.desaturate();
            }

            // Draw vehicle as an arrow to show direction
            // For simplicity, we'll use a triangle pointing right by default
            double centerX = pos.getX() * cellSize + cellSize / 2;
            double centerY = pos.getY() * cellSize + cellSize / 2;
            double arrowSize = cellSize * 0.4;
            
            // Triangle points - default pointing right
            double[] xPoints = {
                centerX - arrowSize, // left point
                centerX + arrowSize, // right point (tip)
                centerX - arrowSize  // bottom left
            };
            
            double[] yPoints = {
                centerY - arrowSize, // top 
                centerY,             // middle (tip)
                centerY + arrowSize  // bottom
            };
            
            javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon();
            for (int i = 0; i < 3; i++) {
                arrow.getPoints().addAll(xPoints[i], yPoints[i]);
            }
            
            arrow.setFill(color);
            pane.getChildren().add(arrow);

            // Add vehicle ID label
            Text text = new Text(centerX, centerY + 4, vehicle.getId());
            text.setFill(VEHICLE_LABEL_COLOR);
            text.setFont(Font.font("Arial", FontWeight.BOLD, cellSize * 0.25));
            text.setTranslateX(-text.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(text);
            
            // Add detailed fuel and GLP indicators
            double fuelPercentage = vehicle.getCurrentFuelGal() / vehicle.getFuelCapacityGal();
            fuelPercentage = Math.min(Math.max(fuelPercentage, 0.0), 1.0); // Clamp between 0 and 1
            Color fuelColor;
            
            if (fuelPercentage < 0.2) {
                fuelColor = Color.RED;
            } else if (fuelPercentage < 0.5) {
                fuelColor = Color.ORANGE;
            } else {
                fuelColor = Color.GREEN;
            }
            
            // Background for fuel indicator
            Rectangle fuelBg = new Rectangle(
                centerX - cellSize * 0.3, 
                centerY + cellSize * 0.4, 
                cellSize * 0.6, cellSize * 0.15);
            fuelBg.setFill(Color.LIGHTGRAY);
            fuelBg.setStroke(Color.BLACK);
            fuelBg.setStrokeWidth(0.5);
            pane.getChildren().add(fuelBg);
            
            // Actual fuel level indicator
            Rectangle fuelLevel = new Rectangle(
                centerX - cellSize * 0.3, 
                centerY + cellSize * 0.4, 
                cellSize * 0.6 * fuelPercentage, cellSize * 0.15);
            fuelLevel.setFill(fuelColor);
            pane.getChildren().add(fuelLevel);
            
            // Fuel text
            Text fuelText = new Text(
                centerX, 
                centerY + cellSize * 0.4 + cellSize * 0.12,
                String.format("%.1f/%.1f", vehicle.getCurrentFuelGal(), vehicle.getFuelCapacityGal()));
            fuelText.setFill(Color.BLACK);
            fuelText.setFont(Font.font("Arial", FontWeight.BOLD, cellSize * 0.2));
            fuelText.setTranslateX(-fuelText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(fuelText);
            
            // GLP indicator
            double glpPercentage = (double)vehicle.getCurrentGlpM3() / vehicle.getGlpCapacityM3();
            glpPercentage = Math.min(Math.max(glpPercentage, 0.0), 1.0); // Clamp between 0 and 1
            Color glpColor;
            
            if (glpPercentage < 0.2) {
                glpColor = Color.LIGHTBLUE;
            } else if (glpPercentage < 0.5) {
                glpColor = Color.SKYBLUE;
            } else {
                glpColor = Color.DARKBLUE;
            }
            
            // Background for GLP indicator
            Rectangle glpBg = new Rectangle(
                centerX - cellSize * 0.3, 
                centerY + cellSize * 0.6, 
                cellSize * 0.6, cellSize * 0.15);
            glpBg.setFill(Color.LIGHTGRAY);
            glpBg.setStroke(Color.BLACK);
            glpBg.setStrokeWidth(0.5);
            pane.getChildren().add(glpBg);
            
            // Actual GLP level indicator
            Rectangle glpLevel = new Rectangle(
                centerX - cellSize * 0.3, 
                centerY + cellSize * 0.6, 
                cellSize * 0.6 * glpPercentage, cellSize * 0.15);
            glpLevel.setFill(glpColor);
            pane.getChildren().add(glpLevel);
            
            // GLP text
            Text glpText = new Text(
                centerX, 
                centerY + cellSize * 0.6 + cellSize * 0.12,
                String.format("%d/%d", vehicle.getCurrentGlpM3(), vehicle.getGlpCapacityM3()));
            glpText.setFill(Color.BLACK);
            glpText.setFont(Font.font("Arial", FontWeight.BOLD, cellSize * 0.2));
            glpText.setTranslateX(-glpText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(glpText);
            
            // Vehicle status indicator
            Text statusText = new Text(
                centerX,
                centerY + cellSize * 0.8,
                vehicle.getStatus().name());
            statusText.setFill(Color.BLACK);
            statusText.setFont(Font.font("Arial", FontWeight.NORMAL, cellSize * 0.2));
            statusText.setTranslateX(-statusText.getLayoutBounds().getWidth() / 2);
            pane.getChildren().add(statusText);
        }
    }
    
    public static void drawVehiclePlan(Pane pane, VehiclePlan plan, int cellSize) {
        Vehicle vehicle = plan.getVehicle();
        List<Action> actions = plan.getActions();

        // Get color for the vehicle
        int colorIndex = Math.abs(vehicle.getId().hashCode()) % VEHICLE_COLORS.length;
        Color color = VEHICLE_COLORS[colorIndex];

        Position currentPos = vehicle.getCurrentPosition();

        // Draw each action
        for (Action action : actions) {
            ActionType type = action.getType();

            if (type == ActionType.DRIVE) {
                List<Position> path = action.getPath();

                if (path != null && !path.isEmpty()) {
                    // Draw simple solid line for the path
                    Position from = currentPos;

                    for (Position to : path) {
                        Line line = new Line(
                                from.getX() * cellSize + cellSize / 2,
                                from.getY() * cellSize + cellSize / 2,
                                to.getX() * cellSize + cellSize / 2,
                                to.getY() * cellSize + cellSize / 2);

                        line.setStroke(color);
                        line.setStrokeWidth(2);
                        pane.getChildren().add(line);

                        from = to;
                    }

                    currentPos = path.get(path.size() - 1);
                }
            } else if (type == ActionType.SERVE) {
                // Mark serving locations with a simple X
                Order order = action.getOrder();
                if (order != null) {
                    Position orderPos = order.getPosition();
                    double x = orderPos.getX() * cellSize + cellSize / 2;
                    double y = orderPos.getY() * cellSize + cellSize / 2;
                    double size = cellSize * 0.2;
                    
                    Line line1 = new Line(x - size, y - size, x + size, y + size);
                    Line line2 = new Line(x + size, y - size, x - size, y + size);
                    line1.setStroke(color);
                    line2.setStroke(color);
                    line1.setStrokeWidth(2);
                    line2.setStrokeWidth(2);
                    
                    pane.getChildren().addAll(line1, line2);
                    currentPos = orderPos;
                }
            } else if (type == ActionType.REFUEL || type == ActionType.RELOAD) {
                // Mark depot operations with a simple circle
                Position depotPos = action.getDestination();
                Circle circle = new Circle(
                        depotPos.getX() * cellSize + cellSize / 2,
                        depotPos.getY() * cellSize + cellSize / 2,
                        cellSize * 0.2);

                circle.setFill(Color.TRANSPARENT);
                circle.setStroke(color);
                circle.setStrokeWidth(1.5);
                pane.getChildren().add(circle);

                currentPos = depotPos;
            }
        }
    }
    
    /**
     * Draws only the current path a vehicle is following based on its current action
     * instead of drawing the entire vehicle plan.
     * 
     * @param pane  The pane to draw on
     * @param plan  The vehicle plan containing the actions
     * @param currentTime The current time to determine which action is active
     * @param cellSize The size of each cell on the grid
     */
    public static void drawCurrentVehiclePath(Pane pane, VehiclePlan plan, LocalDateTime currentTime, int cellSize) {
        if (plan == null) {
            return;
        }
        
        Vehicle vehicle = plan.getVehicle();
        Action currentAction = plan.getActionAt(currentTime);
        
        if (currentAction == null || currentAction.getType() != ActionType.DRIVE) {
            return; // No current driving action to draw
        }
        
        // Get color for the vehicle
        int colorIndex = Math.abs(vehicle.getId().hashCode()) % VEHICLE_COLORS.length;
        Color color = VEHICLE_COLORS[colorIndex];
        
        // Draw only the current driving path
        List<Position> path = currentAction.getPath();
        if (path == null || path.isEmpty()) {
            return;
        }
        
        // Calculate progress along the current path
        double progressRatio = 0.0;
        
        // Calculate the start time of the current action
        LocalDateTime actionStart = currentAction.getExpectedStartTime();
        LocalDateTime actionEnd = currentAction.getExpectedEndTime();
        
        if (actionStart != null && actionEnd != null) {
            try {
                double totalDurationSeconds = java.time.Duration.between(actionStart, actionEnd).getSeconds();
                if (totalDurationSeconds > 0) {
                    double elapsedSeconds = java.time.Duration.between(actionStart, currentTime).getSeconds();
                    progressRatio = Math.min(1.0, Math.max(0.0, elapsedSeconds / totalDurationSeconds));
                }
            } catch (Exception e) {
                // If there's any exception calculating the ratio, default to drawing full path
                progressRatio = 0.0;
            }
        }
        
        // If we couldn't calculate the progress or it's invalid, just draw the full path
        if (Double.isNaN(progressRatio) || progressRatio < 0) {
            progressRatio = 0.0;
        }
        
        int pathProgress = Math.min(path.size() - 1, (int) Math.floor(progressRatio * (path.size() - 1)));
        
        // Draw the path up to the current position
        Position from = path.get(0);
        for (int i = 1; i <= pathProgress; i++) {
            Position to = path.get(i);
            
            Line line = new Line(
                    from.getX() * cellSize + cellSize / 2,
                    from.getY() * cellSize + cellSize / 2,
                    to.getX() * cellSize + cellSize / 2,
                    to.getY() * cellSize + cellSize / 2);
            
            line.setStroke(color);
            line.setStrokeWidth(2);
            pane.getChildren().add(line);
            
            from = to;
        }
        
        // Draw a dashed line for the remaining path
        for (int i = pathProgress + 1; i < path.size(); i++) {
            Position to = path.get(i);
            
            Line line = new Line(
                    from.getX() * cellSize + cellSize / 2,
                    from.getY() * cellSize + cellSize / 2,
                    to.getX() * cellSize + cellSize / 2,
                    to.getY() * cellSize + cellSize / 2);
            
            line.setStroke(color);
            line.setStrokeWidth(1.5);
            line.getStrokeDashArray().addAll(5.0, 5.0); // Make it dashed
            line.setOpacity(0.6); // Make it semi-transparent
            pane.getChildren().add(line);
            
            from = to;
        }
    }
}
