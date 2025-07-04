package com.vroute.api;

import com.vroute.models.Environment;
import com.vroute.orchest.Orchestrator;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * HTTP server that provides REST API endpoints for the V-Route system.
 * Exposes information about vehicles, orders, blockages, and system status.
 */
public class ApiServer {
    
    private static final Logger logger = Logger.getLogger(ApiServer.class.getName());
    
    private final HttpServer server;
    private final Environment environment;
    private final Orchestrator orchestrator;
    private final int port;
    private final ApiServiceLauncher serviceLauncher;
    
    public ApiServer(Environment environment, Orchestrator orchestrator, ApiServiceLauncher serviceLauncher, int port) throws IOException {
        this.environment = environment;
        this.orchestrator = orchestrator;
        this.port = port;
        this.serviceLauncher = serviceLauncher;
        
        // Create HTTP server
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Set up endpoints
        setupEndpoints();
        
        // Use a thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(4));
    }
    
    private void setupEndpoints() {
        // Main status controller for all API endpoints
        StatusController statusController = new StatusController(environment, orchestrator, serviceLauncher);
        
        // API endpoints - simplified structure
        server.createContext("/environment", statusController);
        server.createContext("/vehicles", statusController);
        server.createContext("/orders", statusController);
        server.createContext("/blockages", statusController);
        server.createContext("/simulation/start", statusController);
        server.createContext("/simulation/pause", statusController);
        server.createContext("/simulation/status", statusController);
        server.createContext("/vehicle/breakdown", statusController);
        server.createContext("/vehicle/repair", statusController);
        
        // Root endpoint with API documentation
        server.createContext("/", new RootHandler());
        
        // Health check endpoint
        server.createContext("/health", new HealthHandler());
        
        logger.info("API endpoints configured:");
        logger.info("  GET /environment     - Complete environment snapshot");
        logger.info("  GET /vehicles        - Vehicle status and plans");
        logger.info("  GET /orders          - Orders status and delivery info");
        logger.info("  GET /blockages       - Active blockages and restrictions");
        logger.info("  GET /simulation/status - Get simulation status");
        logger.info("  POST /simulation/start - Start/resume simulation");
        logger.info("  POST /simulation/pause - Pause simulation");
        logger.info("  POST /vehicle/breakdown - Mark vehicle as broken down");
        logger.info("  POST /vehicle/repair    - Repair vehicle and make available");
        logger.info("  GET /health          - Health check");
    }
    
    public void start() {
        server.start();
        logger.info("V-Route API server started on port " + port);
        logger.info("Access the API at: http://localhost:" + port);
        logger.info("API documentation at: http://localhost:" + port + "/");
    }
    
    public void stop() {
        server.stop(0);
        logger.info("V-Route API server stopped");
    }
    
    public boolean isRunning() {
        return server != null;
    }
    
    /**
     * Root handler that provides API documentation
     */
    private static class RootHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, 0);
                exchange.getResponseBody().close();
                return;
            }
            
            String response = createApiDocumentation();
            
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private String createApiDocumentation() {
            return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>V-Route API Documentation</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 40px; background-color: #f5f5f5; }
                        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }
                        h2 { color: #34495e; margin-top: 30px; }
                        .endpoint { background: #ecf0f1; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #3498db; }
                        .method { background: #27ae60; color: white; padding: 4px 8px; border-radius: 3px; font-weight: bold; }
                        .url { font-family: 'Courier New', monospace; background: #34495e; color: white; padding: 4px 8px; border-radius: 3px; margin-left: 10px; }
                        .description { margin-top: 10px; color: #555; }
                        .example { background: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 5px; margin-top: 10px; font-family: 'Courier New', monospace; font-size: 12px; overflow-x: auto; }
                        .status { text-align: center; background: #27ae60; color: white; padding: 10px; border-radius: 5px; margin-bottom: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="status">
                            🚛 V-Route API Server - Activo desde %s
                        </div>
                        
                        <h1>📚 V-Route API Documentation</h1>
                        <p>API REST para obtener el estado actual del sistema de enrutamiento de vehículos V-Route.</p>
                        
                        <h2>🔗 Endpoints Disponibles</h2>
                        
                        <div class="endpoint">
                            <span class="method">GET</span><span class="url">/environment</span>
                            <div class="description">
                                <strong>Snapshot completo del entorno</strong><br>
                                Información completa sobre el estado actual del sistema, incluyendo vehículos, pedidos, bloqueos, y más.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">GET</span><span class="url">/vehicles</span>
                            <div class="description">
                                <strong>Estado detallado de todos los vehículos</strong><br>
                                Información completa de cada vehículo: posición, combustible, carga GLP, estado, plan de ruta.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">GET</span><span class="url">/orders</span>
                            <div class="description">
                                <strong>Estado de todos los pedidos</strong><br>
                                Lista de pedidos con información de entrega, fechas límite, posiciones, prioridad.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">GET</span><span class="url">/blockages</span>
                            <div class="description">
                                <strong>Bloqueos actuales en el sistema</strong><br>
                                Información sobre bloqueos de rutas activos e inactivos con sus períodos de tiempo.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">GET</span><span class="url">/simulation/status</span>
                            <div class="description">
                                <strong>Get simulation status</strong><br>
                                Endpoint for getting the status of the simulation.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">POST</span><span class="url">/simulation/start</span>
                            <div class="description">
                                <strong>Start/resume simulation</strong><br>
                                Endpoint for starting or resuming the simulation.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">POST</span><span class="url">/simulation/pause</span>
                            <div class="description">
                                <strong>Pause simulation</strong><br>
                                Endpoint for pausing the simulation.
                            </div>
                        </div>
                        
                        <div class="endpoint">
                            <span class="method">GET</span><span class="url">/health</span>
                            <div class="description">
                                <strong>Verificación de salud del servidor</strong><br>
                                Endpoint simple para verificar si el servidor API está funcionando.
                            </div>
                        </div>
                        
                        <h2>📋 Ejemplo de Respuesta</h2>
                        <div class="example">
{
  "timestamp": "2025-06-12 10:30:45",
  "currentTime": "2025-01-15 08:00:00",
  "summary": {
    "totalVehicles": 20,
    "availableVehicles": 15,
    "totalOrders": 45,
    "pendingOrders": 12,
    "overdueOrders": 2,
    "totalBlockages": 8,
    "activeBlockages": 3
  },
  "status": "running"
}
                        </div>
                        
                        <h2>ℹ️ Información Adicional</h2>
                        <ul>
                            <li><strong>Formato:</strong> Todas las respuestas están en formato JSON</li>
                            <li><strong>Codificación:</strong> UTF-8</li>
                            <li><strong>CORS:</strong> Habilitado para todas las origins</li>
                            <li><strong>Tiempo:</strong> Los timestamps usan el formato 'yyyy-MM-dd HH:mm:ss'</li>
                            <li><strong>Estado HTTP:</strong> 200 para éxito, 404 para endpoint no encontrado, 500 para errores del servidor</li>
                        </ul>
                        
                        <h2>🔧 Uso</h2>
                        <p>Puedes acceder a los endpoints directamente desde tu navegador o usar herramientas como curl:</p>
                        <div class="example">
curl -X GET http://localhost:8080/api/status
curl -X GET http://localhost:8080/api/vehicles
curl -X GET http://localhost:8080/api/orders
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        }
    }
    
    /**
     * Simple health check handler
     */
    private static class HealthHandler implements HttpHandler {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            
            String response = String.format("""
                {
                    "status": "healthy",
                    "timestamp": "%s",
                    "service": "V-Route API Server"
                }
                """, LocalDateTime.now().format(formatter));
            
            byte[] responseBytes = response.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
