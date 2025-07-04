package com.vroute.api;

import com.vroute.models.*;
import com.vroute.orchest.Orchestrator;
import com.vroute.operation.VehiclePlan;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    private final ApiServiceLauncher serviceLauncher;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatusController(Environment environment, Orchestrator orchestrator, ApiServiceLauncher serviceLauncher) {
        this.environment = environment;
        this.orchestrator = orchestrator;
        this.serviceLauncher = serviceLauncher;
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

        try {
            // Handle endpoints based on path
            switch (path) {
                case "/environment":
                    if ("GET".equals(method)) {
                        String response = getEnvironmentSnapshot();
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/vehicles":
                    if ("GET".equals(method)) {
                        String response = getVehiclesStatus();
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/orders":
                    if ("GET".equals(method)) {
                        String response = getOrdersStatus();
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/blockages":
                    if ("GET".equals(method)) {
                        String response = getBlockagesStatus();
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/simulation/status":
                    if ("GET".equals(method)) {
                        String response = getSimulationStatus();
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/simulation/start":
                    if ("POST".equals(method)) {
                        serviceLauncher.resumeSimulation();
                        String response = "{\"status\":\"success\",\"message\":\"Simulation started\"}";
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/simulation/pause":
                    if ("POST".equals(method)) {
                        serviceLauncher.pauseSimulation();
                        String response = "{\"status\":\"success\",\"message\":\"Simulation paused\"}";
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/vehicle/breakdown":
                    if ("POST".equals(method)) {
                        String response = handleVehicleBreakdown(exchange);
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/vehicle/repair":
                    if ("POST".equals(method)) {
                        String response = handleVehicleRepair(exchange);
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                case "/simulation/speed":
                    if ("GET".equals(method)) {
                        String response = getSimulationSpeed();
                        sendResponse(exchange, 200, response);
                    } else if ("POST".equals(method)) {
                        String response = setSimulationSpeed(exchange);
                        sendResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 405, "Method not allowed");
                    }
                    break;

                default:
                    sendErrorResponse(exchange, 404, "Endpoint not found");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
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
            json.append("        \"current\": ").append(String.format("%.2f", vehicle.getCurrentFuelGal()))
                    .append(",\n");
            json.append("        \"capacity\": ").append(String.format("%.2f", vehicle.getFuelCapacityGal()))
                    .append(",\n");
            json.append("        \"percentage\": ")
                    .append(String.format("%.1f", (vehicle.getCurrentFuelGal() / vehicle.getFuelCapacityGal()) * 100))
                    .append("\n");
            json.append("      },\n");
            json.append("      \"glp\": {\n");
            json.append("        \"current\": ").append(vehicle.getCurrentGlpM3()).append(",\n");
            json.append("        \"capacity\": ").append(vehicle.getGlpCapacityM3()).append(",\n");
            json.append("        \"percentage\": ").append(
                    String.format("%.1f", (vehicle.getCurrentGlpM3() / (double) vehicle.getGlpCapacityM3()) * 100))
                    .append("\n");
            json.append("      }");

            if (plan != null) {
                json.append(",\n");
                json.append("      \"plan\": {\n");
                json.append("        \"totalDistance\": ").append(String.format("%.2f", plan.getTotalDistanceKm()))
                        .append(",\n");
                json.append("        \"totalGlpDelivery\": ")
                        .append(String.format("%.2f", plan.getTotalGlpDeliveredM3())).append(",\n");
                json.append("        \"totalFuelConsumption\": ")
                        .append(String.format("%.2f", plan.getTotalFuelConsumedGal())).append(",\n");
                json.append("        \"servedOrders\": ").append(plan.getServedOrders().size()).append(",\n");
                json.append("        \"currentStatus\": \"").append(plan.getStatusAt(currentTime).name())
                        .append("\",\n");

                // Add path information
                json.append("        \"path\": [\n");
                List<Position> pathPoints = plan.getPathPoints();
                for (int j = 0; j < pathPoints.size(); j++) {
                    Position point = pathPoints.get(j);
                    json.append("          {\n");
                    json.append("            \"x\": ").append(point.getX()).append(",\n");
                    json.append("            \"y\": ").append(point.getY()).append("\n");
                    json.append("          }");
                    if (j < pathPoints.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("        ]\n");
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
            json.append("        \"delivered\": ").append(order.getGlpRequestM3() - order.getRemainingGlpM3())
                    .append("\n");
            json.append("      },\n");
            json.append("      \"status\": {\n");
            json.append("        \"isDelivered\": ").append(order.isDelivered()).append(",\n");
            json.append("        \"isOverdue\": ").append(order.isOverdue(currentTime)).append(",\n");
            json.append("        \"minutesUntilDue\": ").append(order.timeUntilDue(currentTime)).append(",\n");
            json.append("        \"priority\": ").append(String.format("%.2f", order.calculatePriority(currentTime)))
                    .append("\n");
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

            List<Position> points = blockage.getLines();
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

    /**
     * Returns a complete snapshot of the environment state in JSON format
     * Only includes current state and active paths, not full plans
     */
    private String getEnvironmentSnapshot() {
        LocalDateTime currentTime = environment.getCurrentTime();
        List<Vehicle> vehicles = environment.getVehicles();
        List<Order> orders = environment.getOrderQueue();
        List<Blockage> activeBlockages = environment.getActiveBlockagesAt(currentTime);
        Depot mainDepot = environment.getMainDepot();
        List<Depot> auxDepots = environment.getAuxDepots();
        Map<Vehicle, VehiclePlan> vehiclePlans = orchestrator.getVehiclePlans();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"timestamp\": \"").append(LocalDateTime.now().format(formatter)).append("\",\n");
        json.append("  \"simulationTime\": \"").append(currentTime.format(formatter)).append("\",\n");
        json.append("  \"simulationRunning\": ").append(serviceLauncher.isSimulationRunning()).append(",\n");

        // Add vehicles with current positions and only current active paths
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
            json.append("        \"percentage\": ").append(String.format("%.1f", (vehicle.getCurrentGlpM3() / (double) vehicle.getGlpCapacityM3()) * 100)).append("\n");
            json.append("      }");
            
            // Only include current active path, not full plan
            if (plan != null) {
                var currentAction = plan.getActionAt(currentTime);
                if (currentAction != null && currentAction.getType() == com.vroute.operation.ActionType.DRIVE) {
                    json.append(",\n      \"currentPath\": {\n");
                    json.append("        \"actionType\": \"").append(currentAction.getType().name()).append("\",\n");
                    json.append("        \"startTime\": \"").append(currentAction.getExpectedStartTime().format(formatter)).append("\",\n");
                    json.append("        \"endTime\": \"").append(currentAction.getExpectedEndTime().format(formatter)).append("\",\n");
                    json.append("        \"path\": [");
                    
                    var path = currentAction.getPath();
                    if (path != null && !path.isEmpty()) {
                        for (int j = 0; j < path.size(); j++) {
                            var pos = path.get(j);
                            json.append("{\"x\": ").append(pos.getX()).append(", \"y\": ").append(pos.getY()).append("}");
                            if (j < path.size() - 1) json.append(", ");
                        }
                    }
                    json.append("]\n");
                    json.append("      }");
                }
            }
            
            json.append("\n    }");
            if (i < vehicles.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Add only non-delivered orders
        json.append("  \"orders\": [\n");
        List<Order> activeOrders = orders.stream()
                .filter(order -> !order.isDelivered())
                .collect(java.util.stream.Collectors.toList());
        
        for (int i = 0; i < activeOrders.size(); i++) {
            Order order = activeOrders.get(i);
            
            json.append("    {\n");
            json.append("      \"id\": \"").append(order.getId()).append("\",\n");
            json.append("      \"position\": {\n");
            json.append("        \"x\": ").append(order.getPosition().getX()).append(",\n");
            json.append("        \"y\": ").append(order.getPosition().getY()).append("\n");
            json.append("      },\n");
            json.append("      \"arriveTime\": \"").append(order.getArriveTime().format(formatter)).append("\",\n");
            json.append("      \"dueTime\": \"").append(order.getDueTime().format(formatter)).append("\",\n");
            json.append("      \"isOverdue\": ").append(order.isOverdue(currentTime)).append(",\n");
            json.append("      \"glp\": {\n");
            json.append("        \"requested\": ").append(order.getGlpRequestM3()).append(",\n");
            json.append("        \"remaining\": ").append(order.getRemainingGlpM3()).append("\n");
            json.append("      }\n");
            json.append("    }");
            if (i < activeOrders.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Add only currently active blockages
        json.append("  \"blockages\": [\n");
        for (int i = 0; i < activeBlockages.size(); i++) {
            Blockage blockage = activeBlockages.get(i);
            
            json.append("    {\n");
            json.append("      \"id\": \"BLOCKAGE_").append(blockage.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("MMddHHmm"))).append("\",\n");
            json.append("      \"startTime\": \"").append(blockage.getStartTime().format(formatter)).append("\",\n");
            json.append("      \"endTime\": \"").append(blockage.getEndTime().format(formatter)).append("\",\n");
            json.append("      \"positions\": [");
            
            var positions = blockage.getLines();
            for (int j = 0; j < positions.size(); j++) {
                var pos = positions.get(j);
                json.append("{\"x\": ").append(pos.getX()).append(", \"y\": ").append(pos.getY()).append("}");
                if (j < positions.size() - 1) json.append(", ");
            }
            json.append("]\n");
            json.append("    }");
            if (i < activeBlockages.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Add depots with current state
        json.append("  \"depots\": [\n");
        // Main depot
        json.append("    {\n");
        json.append("      \"id\": \"").append(mainDepot.getId()).append("\",\n");
        json.append("      \"position\": {\"x\": ").append(mainDepot.getPosition().getX())
                .append(", \"y\": ").append(mainDepot.getPosition().getY()).append("},\n");
        json.append("      \"isMain\": true,\n");
        json.append("      \"canRefuel\": ").append(mainDepot.canRefuel()).append(",\n");
        json.append("      \"glp\": {\n");
        json.append("        \"current\": ").append(mainDepot.getCurrentGlpM3()).append(",\n");
        json.append("        \"capacity\": ").append(mainDepot.getGlpCapacityM3()).append("\n");
        json.append("      }\n");
        json.append("    }");

        // Auxiliary depots
        for (Depot depot : auxDepots) {
            json.append(",\n    {\n");
            json.append("      \"id\": \"").append(depot.getId()).append("\",\n");
            json.append("      \"position\": {\"x\": ").append(depot.getPosition().getX())
                    .append(", \"y\": ").append(depot.getPosition().getY()).append("},\n");
            json.append("      \"isMain\": false,\n");
            json.append("      \"canRefuel\": ").append(depot.canRefuel()).append(",\n");
            json.append("      \"glp\": {\n");
            json.append("        \"current\": ").append(depot.getCurrentGlpM3()).append(",\n");
            json.append("        \"capacity\": ").append(depot.getGlpCapacityM3()).append("\n");
            json.append("      }\n");
            json.append("    }");
        }
        json.append("\n  ]\n");

        json.append("}");
        return json.toString();
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

    /**
     * Returns the current simulation status with statistics
     */
    private String getSimulationStatus() {
        LocalDateTime currentTime = environment.getCurrentTime();
        List<Vehicle> vehicles = environment.getVehicles();
        List<Order> orders = environment.getOrderQueue();

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
                    "simulationTime": "%s",
                    "status": {
                        "running": %b,
                        "speed": %d
                    },
                    "orders": {
                        "total": %d,
                        "delivered": %d,
                        "pending": %d,
                        "overdue": %d,
                        "deliveryRate": %.2f
                    },
                    "fleet": {
                        "totalVehicles": %d,
                        "availableVehicles": %d,
                        "totalGlpCapacity": %.2f,
                        "currentGlpLoad": %.2f,
                        "capacityUtilization": %.2f
                    }
                }
                """,
                LocalDateTime.now().format(formatter),
                currentTime.format(formatter),
                serviceLauncher.isSimulationRunning(),
                serviceLauncher.getSimulationSpeed(),
                orders.size(),
                deliveredOrders,
                orders.size() - deliveredOrders,
                overdueOrders,
                orders.size() > 0 ? (deliveredOrders * 100.0 / orders.size()) : 0.0,
                vehicles.size(),
                environment.getAvailableVehicles().size(),
                totalGlpCapacity,
                currentGlpLoad,
                totalGlpCapacity > 0 ? (currentGlpLoad * 100.0 / totalGlpCapacity) : 0.0);
    }
    
    /**
     * Handles vehicle breakdown requests
     * Expected JSON: {"vehicleId": "TA01", "reason": "Engine failure", "estimatedRepairHours": 4}
     */
    private String handleVehicleBreakdown(HttpExchange exchange) throws IOException {
        try {
            // Read request body
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            reader.close();
            
            // Parse JSON manually (simple parsing)
            String json = requestBody.toString();
            String vehicleId = extractJsonValue(json, "vehicleId");
            String reason = extractJsonValue(json, "reason");
            String estimatedHoursStr = extractJsonValue(json, "estimatedRepairHours");
            
            if (vehicleId == null || vehicleId.trim().isEmpty()) {
                return "{\"status\":\"error\",\"message\":\"Vehicle ID is required\"}";
            }
            
            // Find vehicle
            Vehicle vehicle = environment.findVehicleById(vehicleId);
            if (vehicle == null) {
                return String.format("{\"status\":\"error\",\"message\":\"Vehicle %s not found\"}", vehicleId);
            }
            
            // Check if vehicle is already unavailable
            if (vehicle.getStatus() == VehicleStatus.UNAVAILABLE) {
                return String.format("{\"status\":\"error\",\"message\":\"Vehicle %s is already unavailable\"}", vehicleId);
            }
            
            // Parse estimated repair hours
            int estimatedHours = 2; // Default
            if (estimatedHoursStr != null && !estimatedHoursStr.trim().isEmpty()) {
                try {
                    estimatedHours = Integer.parseInt(estimatedHoursStr);
                } catch (NumberFormatException e) {
                    estimatedHours = 2; // Use default if parsing fails
                }
            }
            
            // Create incident
            LocalDateTime currentTime = environment.getCurrentTime();
            Shift currentShift = Shift.getShiftForTime(currentTime.toLocalTime());
            
            // Use TI2 as default (requires repair, 2 hours immobilization + 1 shift repair)
            IncidentType incidentType = estimatedHours <= 2 ? IncidentType.TI1 : 
                                       estimatedHours <= 24 ? IncidentType.TI2 : IncidentType.TI3;
            
            String incidentReason = reason != null && !reason.trim().isEmpty() ? reason : "Mechanical failure";
            Incident incident = new Incident(vehicleId, incidentType, currentShift);
            incident.setOccurrenceTime(currentTime);
            incident.setLocation(vehicle.getCurrentPosition());
            
            // Add incident to environment
            environment.addIncident(incident);
            
            // Set vehicle status to unavailable
            vehicle.setStatus(VehicleStatus.UNAVAILABLE);
            
            LocalDateTime repairTime = incident.calculateAvailabilityTime();
            
            return String.format("""
                {
                    "status": "success",
                    "message": "Vehicle %s has been marked as broken down",
                    "vehicleId": "%s",
                    "reason": "%s",
                    "incidentType": "%s",
                    "breakdownTime": "%s",
                    "estimatedRepairTime": "%s",
                    "vehicleStatus": "%s"
                }
                """, vehicleId, vehicleId, incidentReason, incidentType.name(),
                currentTime.format(formatter), 
                repairTime != null ? repairTime.format(formatter) : "Unknown",
                vehicle.getStatus().name());
                
        } catch (Exception e) {
            return String.format("{\"status\":\"error\",\"message\":\"Error processing breakdown: %s\"}", e.getMessage());
        }
    }
    
    /**
     * Handles vehicle repair requests
     * Expected JSON: {"vehicleId": "TA01"}
     */
    private String handleVehicleRepair(HttpExchange exchange) throws IOException {
        try {
            // Read request body
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            reader.close();
            
            // Parse JSON manually (simple parsing)
            String json = requestBody.toString();
            String vehicleId = extractJsonValue(json, "vehicleId");
            
            if (vehicleId == null || vehicleId.trim().isEmpty()) {
                return "{\"status\":\"error\",\"message\":\"Vehicle ID is required\"}";
            }
            
            // Find vehicle
            Vehicle vehicle = environment.findVehicleById(vehicleId);
            if (vehicle == null) {
                return String.format("{\"status\":\"error\",\"message\":\"Vehicle %s not found\"}", vehicleId);
            }
            
            // Check if vehicle is actually broken down
            if (vehicle.getStatus() != VehicleStatus.UNAVAILABLE) {
                return String.format("{\"status\":\"error\",\"message\":\"Vehicle %s is not broken down (status: %s)\"}", 
                    vehicleId, vehicle.getStatus().name());
            }
            
            // Find and resolve active incidents for this vehicle
            List<Incident> activeIncidents = environment.getActiveIncidentsForVehicle(vehicleId);
            int resolvedIncidents = 0;
            
            for (Incident incident : activeIncidents) {
                if (!incident.isResolved()) {
                    incident.setResolved();
                    resolvedIncidents++;
                }
            }
            
            // Set vehicle status back to available
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            
            return String.format("""
                {
                    "status": "success",
                    "message": "Vehicle %s has been repaired and is now available",
                    "vehicleId": "%s",
                    "repairTime": "%s",
                    "resolvedIncidents": %d,
                    "vehicleStatus": "%s"
                }
                """, vehicleId, vehicleId, 
                environment.getCurrentTime().format(formatter),
                resolvedIncidents,
                vehicle.getStatus().name());
                
        } catch (Exception e) {
            return String.format("{\"status\":\"error\",\"message\":\"Error processing repair: %s\"}", e.getMessage());
        }
    }
    
    /**
     * Gets the current simulation speed
     */
    private String getSimulationSpeed() {
        try {
            int currentSpeed = serviceLauncher.getSimulationSpeed();
            boolean isRunning = serviceLauncher.isSimulationRunning();
            
            return String.format("""
                {
                    "status": "success",
                    "currentSpeed": %d,
                    "unit": "milliseconds",
                    "description": "Time between simulation ticks",
                    "simulationRunning": %b,
                    "timestamp": "%s"
                }
                """, currentSpeed, isRunning, LocalDateTime.now().format(formatter));
                
        } catch (Exception e) {
            return String.format("{\"status\":\"error\",\"message\":\"Error getting simulation speed: %s\"}", e.getMessage());
        }
    }
    
    /**
     * Sets the simulation speed
     * Expected JSON: {"speed": 1000}
     */
    private String setSimulationSpeed(HttpExchange exchange) throws IOException {
        try {
            // Read request body
            BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            reader.close();
            
            // Parse JSON manually (simple parsing)
            String json = requestBody.toString();
            String speedStr = extractJsonValue(json, "speed");
            
            if (speedStr == null || speedStr.trim().isEmpty()) {
                return "{\"status\":\"error\",\"message\":\"Speed value is required\"}";
            }
            
            // Parse speed value
            int newSpeed;
            try {
                newSpeed = Integer.parseInt(speedStr);
            } catch (NumberFormatException e) {
                return "{\"status\":\"error\",\"message\":\"Speed must be a valid integer\"}";
            }
            
            // Validate speed range
            if (newSpeed < 50) {
                return "{\"status\":\"error\",\"message\":\"Speed must be at least 50 milliseconds\"}";
            }
            
            if (newSpeed > 10000) {
                return "{\"status\":\"error\",\"message\":\"Speed must not exceed 10000 milliseconds\"}";
            }
            
            // Set the new speed
            int oldSpeed = serviceLauncher.getSimulationSpeed();
            serviceLauncher.setSimulationSpeed(newSpeed);
            
            return String.format("""
                {
                    "status": "success",
                    "message": "Simulation speed updated successfully",
                    "oldSpeed": %d,
                    "newSpeed": %d,
                    "unit": "milliseconds",
                    "timestamp": "%s"
                }
                """, oldSpeed, newSpeed, LocalDateTime.now().format(formatter));
                
        } catch (Exception e) {
            return String.format("{\"status\":\"error\",\"message\":\"Error setting simulation speed: %s\"}", e.getMessage());
        }
    }
    
    /**
     * Simple JSON value extraction utility
     */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        
        // Try without quotes for numeric values
        pattern = "\"" + key + "\"\\s*:\\s*([^,}]+)";
        p = java.util.regex.Pattern.compile(pattern);
        m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        
        return null;
    }
}
