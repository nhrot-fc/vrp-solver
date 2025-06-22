package com.vroute.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vroute.models.Blockage;
import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

public class EnvironmentRenderer extends JPanel {
    private static final int DEFAULT_CELL_SIZE = 12;
    private static final int MIN_CELL_SIZE = 8;
    private static final int MAX_CELL_SIZE = 50;  // Increased max zoom level
    private static final int VEHICLE_SIZE = 8;
    private static final int DEPOT_SIZE = 12;
    private static final int ORDER_SIZE = 6;
    private static final int MAP_PADDING = 20;  // Padding around the map edges
    
    private int cellSize = DEFAULT_CELL_SIZE;
    private Environment environment;
    private Map<String, Color> vehicleColors;
    private boolean autoCenter = true;
    private Point viewCenter = null;
    
    public EnvironmentRenderer() {
        setBackground(Color.WHITE);
        vehicleColors = new HashMap<>();
    }
    
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        resetZoom();
        repaint();
    }
    
    public void resetZoom() {
        // Reset to default view (centered)
        autoCenter = true;
        viewCenter = null;
        revalidate();
        repaint();
    }
    
    public int getZoom() {
        // Convert cell size (MIN_CELL_SIZE-MAX_CELL_SIZE) back to slider value (1-100)
        return (int)(((cellSize - MIN_CELL_SIZE) * 100.0) / (MAX_CELL_SIZE - MIN_CELL_SIZE));
    }
    
    public void setZoom(int zoomLevel) {
        // Convert slider value (1-100) to cell size (MIN_CELL_SIZE-MAX_CELL_SIZE)
        int oldCellSize = this.cellSize;
        this.cellSize = MIN_CELL_SIZE + (int)((zoomLevel / 100.0) * (MAX_CELL_SIZE - MIN_CELL_SIZE));
        
        // If we're not auto-centering, adjust the view center to maintain the same visible area
        if (!autoCenter && viewCenter != null) {
            Container parent = getParent();
            if (parent instanceof JViewport) {
                JViewport viewport = (JViewport) parent;
                Rectangle viewRect = viewport.getViewRect();
                
                // Calculate the center point of the current view
                int centerX = viewRect.x + viewRect.width / 2;
                int centerY = viewRect.y + viewRect.height / 2;
                
                // Scale it to the new cell size
                double scale = (double) cellSize / oldCellSize;
                viewCenter.x = (int) (centerX * scale);
                viewCenter.y = (int) (centerY * scale);
            }
        }
        
        revalidate();
        repaint();
        
        // Ensure the scroll pane updates to center if needed
        SwingUtilities.invokeLater(this::scrollToCenter);
    }
    
    private void scrollToCenter() {
        if (!autoCenter || viewCenter == null) return;
        
        Container parent = getParent();
        if (parent instanceof JViewport) {
            JViewport viewport = (JViewport) parent;
            int viewportWidth = viewport.getWidth();
            int viewportHeight = viewport.getHeight();
            
            // Calculate x,y position to center the actual map content (not just the panel)
            int x = Math.max(0, MAP_PADDING + (Constants.CITY_LENGTH_X * cellSize - viewportWidth) / 2);
            int y = Math.max(0, MAP_PADDING + (Constants.CITY_WIDTH_Y * cellSize - viewportHeight) / 2);
            
            viewport.setViewPosition(new Point(x, y));
        }
    }
    
    public void disableAutoCenter() {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            JViewport viewport = (JViewport) parent;
            Rectangle viewRect = viewport.getViewRect();
            
            // Store current view center
            viewCenter = new Point(viewRect.x + viewRect.width / 2, viewRect.y + viewRect.height / 2);
            autoCenter = false;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (environment == null) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw grid lines
        drawGrid(g2d);
        
        // Draw blockages
        drawBlockages(g2d);
        
        // Draw depots
        drawDepots(g2d);
        
        // Draw orders
        drawOrders(g2d);
        
        // Draw vehicles
        drawVehicles(g2d);
    }
    
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(0.5f));
        
        // Draw horizontal lines
        for (int y = 0; y <= Constants.CITY_WIDTH_Y; y++) {
            g2d.drawLine(
                adjustX(0), adjustY(y * cellSize), 
                adjustX(Constants.CITY_LENGTH_X * cellSize), adjustY(y * cellSize)
            );
        }
        
        // Draw vertical lines
        for (int x = 0; x <= Constants.CITY_LENGTH_X; x++) {
            g2d.drawLine(
                adjustX(x * cellSize), adjustY(0), 
                adjustX(x * cellSize), adjustY(Constants.CITY_WIDTH_Y * cellSize)
            );
        }
    }
    
    private void drawBlockages(Graphics2D g2d) {
        if (environment == null) return;
        
        LocalDateTime currentTime = environment.getCurrentTime();
        List<Blockage> activeBlockages = environment.getActiveBlockagesAt(currentTime);
        
        g2d.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
        
        for (Blockage blockage : activeBlockages) {
            List<Position> lines = blockage.getLines();
            for (int i = 0; i < lines.size() - 1; i++) {
                Position p1 = lines.get(i);
                Position p2 = lines.get(i + 1);
                
                // Draw the blockage line
                g2d.setStroke(new BasicStroke(4.0f));
                g2d.drawLine(
                    adjustX(p1.getX() * cellSize), adjustY(p1.getY() * cellSize),
                    adjustX(p2.getX() * cellSize), adjustY(p2.getY() * cellSize)
                );
            }
        }
    }
    
    private void drawDepots(Graphics2D g2d) {
        if (environment == null) return;
        
        // Draw main depot
        Depot mainDepot = environment.getMainDepot();
        drawDepot(g2d, mainDepot, Color.BLUE);
        
        // Draw aux depots
        for (Depot depot : environment.getAuxDepots()) {
            drawDepot(g2d, depot, Color.CYAN);
        }
    }
    
    private void drawDepot(Graphics2D g2d, Depot depot, Color color) {
        Position pos = depot.getPosition();
        int depotSize = DEPOT_SIZE * cellSize / DEFAULT_CELL_SIZE;
        int x = adjustX(pos.getX() * cellSize - depotSize/2);
        int y = adjustY(pos.getY() * cellSize - depotSize/2);
        
        g2d.setColor(color);
        g2d.fillRect(x, y, depotSize, depotSize);
        
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, depotSize, depotSize);
        
        // Add depot ID as label
        g2d.setFont(new Font("Arial", Font.BOLD, 10 * cellSize / DEFAULT_CELL_SIZE));
        g2d.drawString(depot.getId(), x, y - 2);
    }
    
    private void drawOrders(Graphics2D g2d) {
        if (environment == null) return;
        
        List<Order> orders = environment.getOrderQueue();
        for (Order order : orders) {
            if (!order.isDelivered()) {
                Color orderColor = order.isOverdue(environment.getCurrentTime()) ? 
                                   Color.RED : Color.GREEN;
                drawOrder(g2d, order, orderColor);
            }
        }
    }
    
    private void drawOrder(Graphics2D g2d, Order order, Color color) {
        Position pos = order.getPosition();
        int orderSize = ORDER_SIZE * cellSize / DEFAULT_CELL_SIZE;
        int x = adjustX(pos.getX() * cellSize - orderSize/2);
        int y = adjustY(pos.getY() * cellSize - orderSize/2);
        
        g2d.setColor(color);
        g2d.fillOval(x, y, orderSize, orderSize);
        
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y, orderSize, orderSize);
    }
    
    private void drawVehicles(Graphics2D g2d) {
        if (environment == null) return;
        
        for (Vehicle vehicle : environment.getVehicles()) {
            Color vehicleColor = getVehicleColor(vehicle);
            drawVehicle(g2d, vehicle, vehicleColor);
        }
    }
    
    private Color getVehicleColor(Vehicle vehicle) {
        String id = vehicle.getId();
        if (!vehicleColors.containsKey(id)) {
            // Assign a color based on vehicle type
            switch (vehicle.getType()) {
                case TA:
                    vehicleColors.put(id, Color.RED);
                    break;
                case TB:
                    vehicleColors.put(id, Color.BLUE);
                    break;
                case TC:
                    vehicleColors.put(id, Color.GREEN);
                    break;
                case TD:
                    vehicleColors.put(id, Color.ORANGE);
                    break;
                default:
                    vehicleColors.put(id, Color.GRAY);
            }
        }
        return vehicleColors.get(id);
    }
    
    private void drawVehicle(Graphics2D g2d, Vehicle vehicle, Color color) {
        Position pos = vehicle.getCurrentPosition();
        int vehicleSize = VEHICLE_SIZE * cellSize / DEFAULT_CELL_SIZE;
        int x = adjustX(pos.getX() * cellSize - vehicleSize/2);
        int y = adjustY(pos.getY() * cellSize - vehicleSize/2);
        
        // Adjust appearance based on status
        Color fillColor;
        switch (vehicle.getStatus()) {
            case AVAILABLE:
                fillColor = color;
                break;
            case MAINTENANCE:
                fillColor = Color.YELLOW;
                break;
            case UNAVAILABLE:
                fillColor = Color.GRAY;
                break;
            default:
                fillColor = color;
        }
        
        // Draw the vehicle
        g2d.setColor(fillColor);
        g2d.fillRect(x, y, vehicleSize, vehicleSize);
        
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, vehicleSize, vehicleSize);
        
        // Add vehicle ID as label
        g2d.setFont(new Font("Arial", Font.PLAIN, 9 * cellSize / DEFAULT_CELL_SIZE));
        g2d.drawString(vehicle.getId(), x, y - 1);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(
            Constants.CITY_LENGTH_X * cellSize + 1 + (MAP_PADDING * 2),
            Constants.CITY_WIDTH_Y * cellSize + 1 + (MAP_PADDING * 2)
        );
    }
    
    // Draw elements with padding offset
    private int adjustX(int x) {
        return x + MAP_PADDING;
    }
    
    private int adjustY(int y) {
        return y + MAP_PADDING;
    }
}
