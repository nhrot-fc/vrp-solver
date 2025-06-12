package com.vroute.api;

import com.vroute.models.*;
import com.vroute.orchest.Orchestrator;
import com.vroute.operation.VehiclePlan;
import com.vroute.orchest.SimulationStats;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * REST controller for providing system status information via HTTP endpoints.
 * Provides access to current vehicle states, orders, and blockages.
 */
public class StatusController implements HttpHandler {
    
    private final Environment environment;
    private final Orchestrator orchestrator;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public StatusController(Environment environment, Orchestrator orchestrator) {
        this.environment = environment;
        this.orchestrator = orchestrator;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        // Set CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
            return;
        }
        
        if (!"GET".equals(method)) {
            sendErrorResponse(exchange, 405, "Method not allowed");
            return;
        }
        
        try {
            String response;
            switch (path) {
                case "/api/status":
                    response = getSystemStatus();
                    break;
                case "/api/vehicles":
                    response = getVehiclesStatus();
                    break;
                case "/api/orders":
                    response = getOrdersStatus();
                    break;
                case "/api/blockages":
                    response = getBlockagesStatus();
                    break;
                case "/api/stats":
                    response = getSimulationStats();
                    break;
                default:
                    sendErrorResponse(exchange, 404, "Endpoint not found");
                    return;
            }
            
            sendResponse(exchange, 200, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
    
    private String getSystemStatus() {
        LocalDateTime currentTime = environment.getCurrentTime();
        List<Vehicle> vehicles = environment.getVehicles();
        List<Order> orders = environment.getOrderQueue();
        List<Blockage> blockages = environment.getActiveBlockages();
        
        long availableVehicles = vehicles.stream()
            .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
            .count();
        
        long pendingOrders = orders.stream()
            .filter(o -> !o.isDelivered())
            .count();
        
        long overdueOrders = orders.stream()
            .filter(o -> !o.isDelivered() && o.isOverdue(currentTime))
            .count();
        
        long activeBlockages = blockages.stream()
            .filter(b -> b.isActiveAt(currentTime))
            .count();
        
        return String.format("""
            {
                "timestamp": "%s",
                "currentTime": "%s",
                "summary": {
                    "totalVehicles": %d,
                    "availableVehicles": %d,
                    "totalOrders": %d,
                    "pendingOrders": %d,
                    "overdueOrders": %d,
                    "totalBlockages": %d,
                    "activeBlockages": %d
                },
                "status": "running"
            }
            """,
            LocalDateTime.now().format(formatter),
            currentTime.format(formatter),
            vehicles.size(),
            availableVehicles,
            orders.size(),
            pendingOrders,
            overdueOrders,
            blockages.size(),
            activeBlockages
        );
    }
    
    private String getVehiclesStatus() {
        List<Vehicle> vehicles = environment.getVehicles();
        Map<Vehicle, VehiclePlan> vehiclePlans = orchestrator.getVehiclePlans();
        LocalDateTime currentTime = environment.getCurrentTime();
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(formatter)).append("\",\n");
        json.append("  \"vehicles\": [\n");
        
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            VehiclePlan plan = vehiclePlans.get(vehicle);
            
            json.append("    {\n");
            json.append("      \"id\": \"").append(vehicle.getId()).append("\",\n");
            json.append("      \"type\": \"").append(vehicle.getType().name()).append("\",\n");
            json.append("      \"status\": \"").append(vehicle.getStatus().name()).append("\",\n");
            json.append("      \"position\": {\n");
            json.append("        \"x\": ").append(vehicle.getCurrentPosition().getX()).append(",\n");
            json.append("        \"y\": ").append(vehicle.getCurrentPosition().getY()).append("\n");
            json.append("      },\n");
            json.append("      \"fuel\": {\n");
            json.append("        \"current\": ").append(String.format("%.2f", vehicle.getCurrentFuelGal())).append(",\n");
            json.append("        \"capacity\": ").append(String.format("%.2f", vehicle.getFuelCapacityGal())).append(",\n");
            json.append("        \"percentage\": ").append(String.format("%.1f", (vehicle.getCurrentFuelGal() / vehicle.getFuelCapacityGal()) * 100)).append("\n");
            json.append("      },\n");
            json.append("      \"glp\": {\n");
            json.append("        \"current\": ").append(vehicle.getCurrentGlpM3()).append(",\n");
            json.append("        \"capacity\": ").append(vehicle.getGlpCapacityM3()).append(",\n");
            json.append("        \"percentage\": ").append(String.format("%.1f", (vehicle.getCurrentGlpM3() / (double)vehicle.getGlpCapacityM3()) * 100)).append("\n");
            json.append("      }");
            
            if (plan != null) {
                json.append(",\n");
                json.append("      \"plan\": {\n");
                json.append("        \"totalDistance\": ").append(String.format("%.2f", plan.getTotalDistanceKm())).append(",\n");
                json.append("        \"totalGlpDelivery\": ").append(String.format("%.2f", plan.getTotalGlpDeliveredM3())).append(",\n");
                json.append("        \"totalFuelConsumption\": ").append(String.format("%.2f", plan.getTotalFuelConsumedGal())).append(",\n");
                json.append("        \"servedOrders\": ").append(plan.getServedOrders().size()).append(",\n");
                json.append("        \"currentStatus\": \"").append(plan.getStatusAt(currentTime).name()).append("\"\n");
                json.append("      }\n");
            } else {
                json.append("\n");
            }
            
            json.append("    }");
            if (i < vehicles.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    private String getOrdersStatus() {
        List<Order> orders = environment.getOrderQueue();
        LocalDateTime currentTime = environment.getCurrentTime();
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(formatter)).append("\",\n");
        json.append("  \"orders\": [\n");
        
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            
            json.append("    {\n");
            json.append("      \"id\": \"").append(order.getId()).append("\",\n");
            json.append("      \"arriveTime\": \"").append(order.getArriveTime().format(formatter)).append("\",\n");
            json.append("      \"dueTime\": \"").append(order.getDueTime().format(formatter)).append("\",\n");
            json.append("      \"position\": {\n");
            json.append("        \"x\": ").append(order.getPosition().getX()).append(",\n");
            json.append("        \"y\": ").append(order.getPosition().getY()).append("\n");
            json.append("      },\n");
            json.append("      \"glp\": {\n");
            json.append("        \"requested\": ").append(order.getGlpRequestM3()).append(",\n");
            json.append("        \"remaining\": ").append(order.getRemainingGlpM3()).append(",\n");
            json.append("        \"delivered\": ").append(order.getGlpRequestM3() - order.getRemainingGlpM3()).append("\n");
            json.append("      },\n");
            json.append("      \"status\": {\n");
            json.append("        \"isDelivered\": ").append(order.isDelivered()).append(",\n");
            json.append("        \"isOverdue\": ").append(order.isOverdue(currentTime)).append(",\n");
            json.append("        \"minutesUntilDue\": ").append(order.timeUntilDue(currentTime)).append(",\n");
            json.append("        \"priority\": ").append(String.format("%.2f", order.calculatePriority(currentTime))).append("\n");
            json.append("      },\n");
            json.append("      \"deliveryRecords\": ").append(order.getRecords().size()).append("\n");
            json.append("    }");
            if (i < orders.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    private String getBlockagesStatus() {
        List<Blockage> blockages = environment.getActiveBlockages();
        LocalDateTime currentTime = environment.getCurrentTime();
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(formatter)).append("\",\n");
        json.append("  \"blockages\": [\n");
        
        for (int i = 0; i < blockages.size(); i++) {
            Blockage blockage = blockages.get(i);
            
            json.append("    {\n");
            json.append("      \"startTime\": \"").append(blockage.getStartTime().format(formatter)).append("\",\n");
            json.append("      \"endTime\": \"").append(blockage.getEndTime().format(formatter)).append("\",\n");
            json.append("      \"isActive\": ").append(blockage.isActiveAt(currentTime)).append(",\n");
            json.append("      \"points\": [\n");
            
            List<Position> points = blockage.getBlockagePoints();
            for (int j = 0; j < points.size(); j++) {
                Position point = points.get(j);
                json.append("        {\n");
                json.append("          \"x\": ").append(point.getX()).append(",\n");
                json.append("          \"y\": ").append(point.getY()).append("\n");
                json.append("        }");
                if (j < points.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            
            json.append("      ]\n");
            json.append("    }");
            if (i < blockages.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    private String getSimulationStats() {
        // Note: This would require access to SimulationStats from the orchestrator
        // For now, returning basic statistics
        List<Vehicle> vehicles = environment.getVehicles();
        List<Order> orders = environment.getOrderQueue();
        LocalDateTime currentTime = environment.getCurrentTime();
        
        long deliveredOrders = orders.stream()
            .filter(Order::isDelivered)
            .count();
        
        long overdueOrders = orders.stream()
            .filter(o -> !o.isDelivered() && o.isOverdue(currentTime))
            .count();
        
        double totalGlpCapacity = vehicles.stream()
            .mapToDouble(Vehicle::getGlpCapacityM3)
            .sum();
        
        double currentGlpLoad = vehicles.stream()
            .mapToDouble(Vehicle::getCurrentGlpM3)
            .sum();
        
        return String.format("""
            {
                "timestamp": "%s",
                "currentTime": "%s",
                "orders": {
                    "total": %d,
                    "delivered": %d,
                    "pending": %d,
                    "overdue": %d,
                    "deliveryRate": %.2f
                },
                "fleet": {
                    "totalVehicles": %d,
                    "totalGlpCapacity": %.2f,
                    "currentGlpLoad": %.2f,
                    "capacityUtilization": %.2f
                }
            }
            """,
            LocalDateTime.now().format(formatter),
            currentTime.format(formatter),
            orders.size(),
            deliveredOrders,
            orders.size() - deliveredOrders,
            overdueOrders,
            orders.size() > 0 ? (deliveredOrders * 100.0 / orders.size()) : 0.0,
            vehicles.size(),
            totalGlpCapacity,
            currentGlpLoad,
            totalGlpCapacity > 0 ? (currentGlpLoad * 100.0 / totalGlpCapacity) : 0.0
        );
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
    
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorResponse = String.format("""
            {
                "error": "%s",
                "status": %d,
                "timestamp": "%s"
            }
            """, message, statusCode, LocalDateTime.now().format(formatter));
        sendResponse(exchange, statusCode, errorResponse);
    }
}
