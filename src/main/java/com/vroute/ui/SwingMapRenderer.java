package com.vroute.ui;

import java.awt.*;
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

public class SwingMapRenderer {

    private static final Color[] VEHICLE_COLORS = {
            new Color(33, 150, 243), // Blue
            new Color(76, 175, 80), // Green
            new Color(156, 39, 176), // Purple
            new Color(255, 152, 0), // Orange
            new Color(244, 67, 54), // Red
            new Color(0, 188, 212), // Cyan
            new Color(255, 235, 59), // Yellow
            new Color(233, 30, 99), // Pink
            new Color(0, 150, 136), // Teal
            new Color(103, 58, 183) // Deep Purple
    };

    private static final Color MAIN_DEPOT_COLOR = new Color(0, 0, 139); // Dark blue
    private static final Color AUX_DEPOT_COLOR = Color.BLUE;
    private static final Color NO_GAS_GLP_COLOR = Color.GRAY;
    private static final Color ORDER_COLOR = Color.ORANGE;
    private static final Color LATE_ORDER_COLOR = Color.RED;
    private static final Color BLOCKAGE_COLOR = Color.BLACK;
    private static final Color GRID_COLOR = Color.LIGHT_GRAY;
    private static final Color VEHICLE_LABEL_COLOR = Color.BLACK;

    public static void drawGrid(Graphics2D g2d, int width, int height, int cellSize) {
        // Set background color
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width * cellSize, height * cellSize);
        
        // Draw grid lines
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(0.5f));
        
        // Draw horizontal lines
        for (int y = 0; y <= height; y++) {
            g2d.drawLine(0, y * cellSize, width * cellSize, y * cellSize);
        }

        // Draw vertical lines
        for (int x = 0; x <= width; x++) {
            g2d.drawLine(x * cellSize, 0, x * cellSize, height * cellSize);
        }
    }

    public static void drawDepots(Graphics2D g2d, List<Depot> auxDepots, Depot mainDepot, int cellSize) {
        // Draw main depot
        drawDepot(g2d, mainDepot, MAIN_DEPOT_COLOR, cellSize, true);

        // Draw auxiliary depots
        for (Depot depot : auxDepots) {
            drawDepot(g2d, depot, AUX_DEPOT_COLOR, cellSize, false);
        }
    }

    private static void drawDepot(Graphics2D g2d, Depot depot, Color color, int cellSize, boolean isMain) {
        Position pos = depot.getPosition();
        int size = (int) (cellSize * 0.8);
        int x = pos.getX() * cellSize + (cellSize - size) / 2;
        int y = pos.getY() * cellSize + (cellSize - size) / 2;

        // Check if the depot can provide fuel and has GLP
        Color fillColor = color;
        if (!depot.canRefuel() && depot.getCurrentGlpM3() == 0) {
            fillColor = NO_GAS_GLP_COLOR;
        }
        
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, size, size);
        
        // Draw depot label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, cellSize / 2));
        FontMetrics fm = g2d.getFontMetrics();
        String label = isMain ? "M" : "A";
        int labelX = x + (size - fm.stringWidth(label)) / 2;
        int labelY = y + (size + fm.getAscent()) / 2;
        g2d.drawString(label, labelX, labelY);

        // Draw depot ID
        g2d.setColor(color.darker());
        g2d.setFont(new Font("Arial", Font.PLAIN, cellSize / 3));
        fm = g2d.getFontMetrics();
        String id = depot.getId();
        int idX = pos.getX() * cellSize + (cellSize - fm.stringWidth(id)) / 2;
        int idY = (pos.getY() + 1) * cellSize + fm.getAscent() + 4;
        g2d.drawString(id, idX, idY);
        
        // Draw GLP level
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, cellSize / 4));
        fm = g2d.getFontMetrics();
        String glpText = String.format("GLP: %d/%d", depot.getCurrentGlpM3(), depot.getGlpCapacityM3());
        int glpX = pos.getX() * cellSize + (cellSize - fm.stringWidth(glpText)) / 2;
        int glpY = (pos.getY() + 1) * cellSize + cellSize / 2;
        g2d.drawString(glpText, glpX, glpY);
        
        // Draw refuel capability
        if (depot.canRefuel()) {
            g2d.setColor(Color.GREEN.darker());
            g2d.setFont(new Font("Arial", Font.PLAIN, cellSize / 4));
            fm = g2d.getFontMetrics();
            String fuelText = "⛽ Fuel";
            int fuelX = pos.getX() * cellSize + (cellSize - fm.stringWidth(fuelText)) / 2;
            int fuelY = (pos.getY() + 1) * cellSize + (int) (cellSize * 0.8);
            g2d.drawString(fuelText, fuelX, fuelY);
        }
    }

    public static void drawOrders(Graphics2D g2d, List<Order> orders, Environment environment, int cellSize) {
        LocalDateTime currentTime = environment.getCurrentTime();
        
        for (Order order : orders) {
            if (order.isDelivered()) {
                continue;
            }

            Position pos = order.getPosition();
            boolean isLate = order.isOverdue(currentTime);
            
            int x = pos.getX() * cellSize + cellSize / 4;
            int y = pos.getY() * cellSize + cellSize / 4;
            int size = cellSize / 2;
            
            // Use different shapes for regular and late orders
            if (!isLate) {
                g2d.setColor(ORDER_COLOR);
                g2d.fillOval(x, y, size, size);
            } else {
                g2d.setColor(LATE_ORDER_COLOR);
                g2d.fillRect(x, y, size, size);
            }

            // Add order quantity text
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, cellSize / 3));
            FontMetrics fm = g2d.getFontMetrics();
            String qtyText = String.valueOf(order.getRemainingGlpM3());
            int qtyX = pos.getX() * cellSize + (cellSize - fm.stringWidth(qtyText)) / 2;
            int qtyY = pos.getY() * cellSize + (cellSize + fm.getAscent()) / 2;
            g2d.drawString(qtyText, qtyX, qtyY);

            // Add order ID
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, cellSize / 4));
            fm = g2d.getFontMetrics();
            String orderId = order.getId().substring(0, Math.min(order.getId().length(), 8));
            int idX = pos.getX() * cellSize + (cellSize - fm.stringWidth(orderId)) / 2;
            int idY = (pos.getY() + 1) * cellSize + fm.getAscent() + 4;
            g2d.drawString(orderId, idX, idY);
            
            // Add GLP requirement
            String glpText = String.format("GLP: %d/%d", order.getRemainingGlpM3(), order.getGlpRequestM3());
            int glpX = pos.getX() * cellSize + (cellSize - fm.stringWidth(glpText)) / 2;
            int glpY = (pos.getY() + 1) * cellSize + cellSize / 2;
            g2d.drawString(glpText, glpX, glpY);
            
            // Add due time
            g2d.setColor(isLate ? LATE_ORDER_COLOR : Color.BLACK);
            g2d.setFont(new Font("Arial", isLate ? Font.BOLD : Font.PLAIN, cellSize / 4));
            fm = g2d.getFontMetrics();
            String timeLabel = "Due: " + order.getDueTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            int timeX = pos.getX() * cellSize + (cellSize - fm.stringWidth(timeLabel)) / 2;
            int timeY = (pos.getY() + 1) * cellSize + (int) (cellSize * 0.7);
            g2d.drawString(timeLabel, timeX, timeY);
        }
    }

    public static void drawBlockages(Graphics2D g2d, Environment environment, LocalDateTime currentTime, int cellSize) {
        List<Blockage> activeBlockages = environment.getActiveBlockagesAt(currentTime);

        g2d.setColor(BLOCKAGE_COLOR);
        g2d.setStroke(new BasicStroke(3.0f));

        for (Blockage blockage : activeBlockages) {
            List<Position> blockagePoints = blockage.getLines();

            // Draw lines connecting blockage points
            for (int i = 0; i < blockagePoints.size() - 1; i++) {
                Position from = blockagePoints.get(i);
                Position to = blockagePoints.get(i + 1);
                
                int x1 = from.getX() * cellSize + cellSize / 2;
                int y1 = from.getY() * cellSize + cellSize / 2;
                int x2 = to.getX() * cellSize + cellSize / 2;
                int y2 = to.getY() * cellSize + cellSize / 2;
                
                g2d.drawLine(x1, y1, x2, y2);
            }
            
            // Mark each blockage point with a small dot
            for (Position pos : blockagePoints) {
                int x = pos.getX() * cellSize + cellSize / 2 - 2;
                int y = pos.getY() * cellSize + cellSize / 2 - 2;
                g2d.fillOval(x, y, 4, 4);
            }
        }
    }

    public static void drawVehicles(Graphics2D g2d, List<Vehicle> vehicles, int cellSize) {
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
                color = color.darker();
            }

            // Draw vehicle as a triangle (arrow)
            int centerX = pos.getX() * cellSize + cellSize / 2;
            int centerY = pos.getY() * cellSize + cellSize / 2;
            int arrowSize = (int) (cellSize * 0.4);
            
            // Triangle points - default pointing right
            int[] xPoints = {
                centerX - arrowSize,
                centerX + arrowSize,
                centerX - arrowSize
            };
            
            int[] yPoints = {
                centerY - arrowSize,
                centerY,
                centerY + arrowSize
            };
            
            g2d.setColor(color);
            g2d.fillPolygon(xPoints, yPoints, 3);

            // Add vehicle ID label
            g2d.setColor(VEHICLE_LABEL_COLOR);
            g2d.setFont(new Font("Arial", Font.BOLD, cellSize / 4));
            FontMetrics fm = g2d.getFontMetrics();
            String vehicleId = vehicle.getId();
            int idX = centerX - fm.stringWidth(vehicleId) / 2;
            int idY = centerY + fm.getAscent() / 2;
            g2d.drawString(vehicleId, idX, idY);
            
            // Draw fuel indicator
            double fuelPercentage = vehicle.getCurrentFuelGal() / vehicle.getFuelCapacityGal();
            fuelPercentage = Math.min(Math.max(fuelPercentage, 0.0), 1.0);
            
            Color fuelColor;
            if (fuelPercentage < 0.2) {
                fuelColor = Color.RED;
            } else if (fuelPercentage < 0.5) {
                fuelColor = Color.ORANGE;
            } else {
                fuelColor = Color.GREEN;
            }
            
            // Background for fuel indicator
            int fuelX = centerX - (int) (cellSize * 0.3);
            int fuelY = centerY + (int) (cellSize * 0.4);
            int fuelWidth = (int) (cellSize * 0.6);
            int fuelHeight = (int) (cellSize * 0.15);
            
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(fuelX, fuelY, fuelWidth, fuelHeight);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(fuelX, fuelY, fuelWidth, fuelHeight);
            
            // Actual fuel level
            g2d.setColor(fuelColor);
            g2d.fillRect(fuelX, fuelY, (int) (fuelWidth * fuelPercentage), fuelHeight);
        }
    }
    
    public static void drawVehiclePlan(Graphics2D g2d, VehiclePlan plan, int cellSize) {
        Vehicle vehicle = plan.getVehicle();
        List<Action> actions = plan.getActions();

        // Get color for the vehicle
        int colorIndex = Math.abs(vehicle.getId().hashCode()) % VEHICLE_COLORS.length;
        Color color = VEHICLE_COLORS[colorIndex];

        // Draw each action
        for (Action action : actions) {
            if (action.getType() == ActionType.DRIVE && action.getPath() != null) {
                List<Position> path = action.getPath();
                
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(2.0f));
                
                for (int i = 0; i < path.size() - 1; i++) {
                    Position from = path.get(i);
                    Position to = path.get(i + 1);
                    
                    int x1 = from.getX() * cellSize + cellSize / 2;
                    int y1 = from.getY() * cellSize + cellSize / 2;
                    int x2 = to.getX() * cellSize + cellSize / 2;
                    int y2 = to.getY() * cellSize + cellSize / 2;
                    
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }
        }
    }
    
    public static void drawCurrentVehiclePath(Graphics2D g2d, VehiclePlan plan, LocalDateTime currentTime, int cellSize) {
        if (plan == null) {
            return;
        }
        
        Vehicle vehicle = plan.getVehicle();
        Action currentAction = plan.getActionAt(currentTime);
        
        if (currentAction == null || currentAction.getType() != ActionType.DRIVE) {
            return;
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
            long totalDuration = java.time.Duration.between(actionStart, actionEnd).toMinutes();
            long elapsedDuration = java.time.Duration.between(actionStart, currentTime).toMinutes();
            
            if (totalDuration > 0) {
                progressRatio = (double) elapsedDuration / totalDuration;
            }
        }
        
        // If we couldn't calculate the progress or it's invalid, just draw the full path
        if (Double.isNaN(progressRatio) || progressRatio < 0) {
            progressRatio = 1.0;
        }
        
        int pathProgress = Math.min(path.size() - 1, (int) Math.floor(progressRatio * (path.size() - 1)));
        
        // Draw the path up to the current position
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3.0f));
        
        Position from = path.get(0);
        for (int i = 1; i <= pathProgress; i++) {
            Position to = path.get(i);
            
            int x1 = from.getX() * cellSize + cellSize / 2;
            int y1 = from.getY() * cellSize + cellSize / 2;
            int x2 = to.getX() * cellSize + cellSize / 2;
            int y2 = to.getY() * cellSize + cellSize / 2;
            
            g2d.drawLine(x1, y1, x2, y2);
            from = to;
        }
        
        // Draw a dashed line for the remaining path
        g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        g2d.setColor(color.brighter());
        
        for (int i = pathProgress + 1; i < path.size(); i++) {
            Position to = path.get(i);
            
            int x1 = from.getX() * cellSize + cellSize / 2;
            int y1 = from.getY() * cellSize + cellSize / 2;
            int x2 = to.getX() * cellSize + cellSize / 2;
            int y2 = to.getY() * cellSize + cellSize / 2;
            
            g2d.drawLine(x1, y1, x2, y2);
            from = to;
        }
    }
}
