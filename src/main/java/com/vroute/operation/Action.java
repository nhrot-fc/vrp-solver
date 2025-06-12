package com.vroute.operation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.vroute.models.Constants;
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
    private final Order order;

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

        boolean actionCompleted = !currentTime.isBefore(expectedEndTime);
        double progressRatio = calculateProgressRatio(currentTime);

        System.out.println(
                String.format("Executing action: %s | Progress: %.2f%% | Time: %s",
                        this, progressRatio * 100, currentTime));
        switch (type) {
            case DRIVE:
                if (path != null && path.size() > 1) {
                    int pathProgress = Math.min(path.size() - 1, (int) Math.floor(progressRatio * (path.size() - 1)));

                    if (pathProgress >= 0) {
                        Position currentPosition = path.get(pathProgress);
                        vehicle.setCurrentPosition(currentPosition);

                        if (actionCompleted) {
                            vehicle.consumeFuel(Math.abs(fuelChangeGal));
                        } else if (pathProgress > 0) {
                            double partialDistance = calculatePartialPathDistance(0, pathProgress);
                            double fuelConsumed = (partialDistance / calculatePartialPathDistance(0, path.size() - 1))
                                    * Math.abs(fuelChangeGal);
                            vehicle.consumeFuel(fuelConsumed);
                        }
                    }

                    if (actionCompleted) {
                        vehicle.setCurrentPosition(destination);
                    }
                }

                vehicle.setStatus(VehicleStatus.DRIVING);
                break;
            case REFUEL:
                vehicle.setStatus(VehicleStatus.REFUELING);
                if (actionCompleted) {
                    // El repostaje se completa al final
                    vehicle.refuel();
                }
                break;

            case RELOAD:
                vehicle.setStatus(VehicleStatus.RELOADING);
                if (actionCompleted) {
                    vehicle.refill(glpChangeM3);
                }
                break;

            case SERVE:
                vehicle.setStatus(VehicleStatus.SERVING);
                if (actionCompleted && vehicle.canDispenseGLP(Math.abs(glpChangeM3)) && order != null) {
                    vehicle.serveOrder(order, Math.abs(glpChangeM3), currentTime);
                }
                break;

            case MAINTENANCE:
                // Mantenimiento programado - estado progresivo
                vehicle.setStatus(VehicleStatus.MAINTENANCE);
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
