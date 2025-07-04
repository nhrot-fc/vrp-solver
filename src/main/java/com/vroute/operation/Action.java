package com.vroute.operation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleStatus;

public class Action {
    // General attributes for all actions
    private final ActionType type;
    private final LocalDateTime expectedStartTime;
    private final LocalDateTime expectedEndTime;
    private final Position destination;

    // Serving specific attributes
    private final int glpChangeM3;
    private final Order order; // Keep for backward compatibility
    private final String orderId; // Use ID to find in environment

    // Driving specific attributes
    private final double fuelChangeGal;
    private final List<Position> path;

    public Action(ActionType type, List<Position> path, Position endPosition,
            LocalDateTime expectedStartTime, LocalDateTime expectedEndTime, Order order, int glpChangeM3,
            double fuelChangeGal) {
        this.type = type;
        this.path = path;
        this.destination = endPosition;
        this.expectedStartTime = expectedStartTime;
        this.expectedEndTime = expectedEndTime;
        this.order = order;
        this.orderId = (order != null) ? order.getId() : null;
        this.glpChangeM3 = glpChangeM3;
        this.fuelChangeGal = fuelChangeGal;
    }

    public ActionType getType() {
        return type;
    }

    public Duration getDuration() {
        return Duration.between(expectedStartTime, expectedEndTime);
    }

    public LocalDateTime getExpectedStartTime() {
        return expectedStartTime;
    }

    public LocalDateTime getExpectedEndTime() {
        return expectedEndTime;
    }

    public Position getDestination() {
        return destination;
    }

    public int getGlpChangeM3() {
        return glpChangeM3;
    }

    public Order getOrder() {
        return order;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getFuelChangeGal() {
        return fuelChangeGal;
    }

    public List<Position> getPath() {
        return path;
    }

    public void execute(Vehicle vehicle, Environment environment,
            LocalDateTime currentTime) {
        if (currentTime.isBefore(expectedStartTime)) {
            return;
        }

        double progressRatio = calculateProgressRatio(currentTime);

        // System.out.println(
        // String.format("Executing action: %s | Progress: %.2f%% | Time: %s",
        // this, progressRatio * 100, currentTime));
        switch (type) {
            case DRIVE:
                if (path != null && path.size() > 1) {
                    int pathProgress = Math.min(path.size() - 1, (int) Math.floor(progressRatio * (path.size() - 1)));

                    if (pathProgress >= 0) {
                        Position currentPosition = path.get(pathProgress);
                        vehicle.setCurrentPosition(currentPosition);
                        double partialDistance = calculatePartialPathDistance(0, pathProgress);
                        double fuelConsumed = (partialDistance / calculatePartialPathDistance(0, path.size() - 1))
                                * Math.abs(fuelChangeGal);
                        vehicle.consumeFuel(fuelConsumed);
                    }
                }

                vehicle.setStatus(VehicleStatus.DRIVING);
                break;
            case REFUEL:
                vehicle.setStatus(VehicleStatus.REFUELING);
                vehicle.refuel();
                break;

            case RELOAD:
                vehicle.setStatus(VehicleStatus.RELOADING);
                vehicle.refill(glpChangeM3);
                // Update depot inventory - find depot by position
                Depot depot = findDepotByPosition(environment, destination);
                if (depot != null) {
                    depot.serveGLP(glpChangeM3);
                }
                break;

            case SERVE:
                vehicle.setStatus(VehicleStatus.SERVING);
                // Find the actual order in the environment using its ID
                if (orderId != null) {
                    Order environmentOrder = environment.findOrderById(orderId);
                    if (environmentOrder != null) {
                        vehicle.serveOrder(environmentOrder, glpChangeM3, currentTime);
                    } else {
                        // Fallback to the original order if not found in environment
                        vehicle.serveOrder(order, glpChangeM3, currentTime);
                    }
                } else {
                    // Fallback to the original order if no ID available
                    vehicle.serveOrder(order, glpChangeM3, currentTime);
                }
                break;

            case MAINTENANCE:
                // Mantenimiento programado - estado progresivo
                // vehicle.setStatus(VehicleStatus.MAINTENANCE);
                break;

            case WAIT:
                // Tiempo de espera - simplemente muestra el estado correcto
                vehicle.setStatus(VehicleStatus.IDLE);
                break;
        }
    }

    /**
     * Calcula la distancia parcial recorrida en el path desde el nodo inicial
     * hasta un nodo específico.
     * 
     * @param startNodeIndex Índice del nodo inicial
     * @param endNodeIndex   Índice del nodo final
     * @return Distancia en kilómetros
     */
    private double calculatePartialPathDistance(int startNodeIndex, int endNodeIndex) {
        if (path == null || startNodeIndex < 0 || endNodeIndex >= path.size() || startNodeIndex >= endNodeIndex) {
            return 0.0;
        }

        double distance = 0.0;
        for (int i = startNodeIndex; i < endNodeIndex; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            distance += current.distanceTo(next);
        }

        return distance;
    }

    /**
     * Calcula la proporción de progreso de una acción en un momento específico.
     * 
     * @param currentTime El tiempo actual
     * @return Un valor entre 0.0 (no iniciado) y 1.0 (completado)
     */
    private double calculateProgressRatio(LocalDateTime currentTime) {
        // Si la acción no ha comenzado todavía
        if (currentTime.isBefore(expectedStartTime)) {
            return 0.0;
        }

        // Si la acción ya ha terminado
        if (!currentTime.isBefore(expectedEndTime)) {
            return 1.0;
        }

        // Calcular progreso parcial
        double totalDurationMinutes = getDuration().toMinutes();
        if (totalDurationMinutes <= 0) {
            return 1.0; // Para evitar división por cero
        }

        double elapsedMinutes = Duration.between(expectedStartTime, currentTime).toMinutes();
        return Math.min(1.0, elapsedMinutes / totalDurationMinutes);
    }

    /**
     * Finds a depot by its position
     * @param environment The environment to search in
     * @param position The position to match
     * @return The depot at the given position, or null if not found
     */
    private Depot findDepotByPosition(Environment environment, Position position) {
        final double POSITION_TOLERANCE = 0.01; // Small tolerance in kilometers
        
        // Check main depot
        Depot mainDepot = environment.getMainDepot();
        if (mainDepot != null && mainDepot.getPosition().distanceTo(position) <= POSITION_TOLERANCE) {
            return mainDepot;
        }
        
        // Check auxiliary depots
        for (Depot depot : environment.getAuxDepots()) {
            if (depot.getPosition().distanceTo(position) <= POSITION_TOLERANCE) {
                return depot;
            }
        }
        
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Expected Start: %s | Expected End: %s ",
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").format(expectedStartTime),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm").format(expectedEndTime)));
        switch (type) {
            case DRIVE:
                String pathLength = (path != null) ? String.format("| %d nodes", path.size()) : "";
                String distanceKm = (path != null && path.size() > 1)
                        ? String.format("| %04d km", (path.size() - 1) * Constants.NODE_DISTANCE)
                        : "";
                String fuelInfo = (fuelChangeGal != 0) ? String.format("| Fuel: %.2f gal", Math.abs(fuelChangeGal))
                        : "";

                sb.append(String.format("🚗  DRIVING     | To: %-15s | Time: %3d min %s %s %s",
                        destination, getDuration().toMinutes(), pathLength, distanceKm, fuelInfo));
                break;

            case REFUEL:
                sb.append(String.format("⛽  REFUELING   | Location: %-10s | Time: %3d min | Full tank restored",
                        destination, getDuration().toMinutes()));
                break;

            case RELOAD:
                sb.append(String.format("🛢️  REFILLING   | Location: %-10s | Time: %3d min | GLP: +%d m³",
                        destination, getDuration().toMinutes(), glpChangeM3));
                break;

            case SERVE:
                String orderDetails = "";
                if (order != null) {
                    orderDetails = String.format("| Client: %s | Due: %s",
                            order.getId(),
                            order.getDueTime()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
                }

                sb.append(String.format("🛒  SERVING     | Location: %-10s | Time: %3d min | GLP: -%d m³ %s",
                        destination, getDuration().toMinutes(), Math.abs(glpChangeM3), orderDetails));
                break;

            case MAINTENANCE:
                sb.append(String.format("🔧  MAINTENANCE | Location: %-10s | Time: %3d min | Routine service",
                        destination, Duration.between(expectedStartTime, expectedEndTime).toMinutes()));
                break;

            case WAIT:
                sb.append(String.format("⏸️  IDLE        | Location: %-10s | Time: %3d min | Waiting",
                        destination, Duration.between(expectedStartTime, expectedEndTime).toMinutes()));
                break;
        }

        return sb.toString();
    }
}
