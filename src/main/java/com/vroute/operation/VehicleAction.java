package com.vroute.operation;

import java.time.Duration;
import java.util.List;

import com.vroute.models.Constants;
import com.vroute.models.Order;
import com.vroute.models.Position;

public class VehicleAction {
    // General attributes for all actions
    private final ActionType type;
    private final Duration duration;
    private final Position endPosition;

    // Serving specific attributes
    private final int glpChangeM3;
    private final Order order;

    // Driving specific attributes
    private final double fuelChangeGal;
    private final List<Position> path;

    public VehicleAction(ActionType type, List<Position> path, Position endPosition,
            Duration duration, Order order, int glpChangeM3, double fuelChangeGal) {
        this.type = type;
        this.path = path;
        this.endPosition = endPosition;
        this.duration = duration;
        this.order = order;
        this.glpChangeM3 = glpChangeM3;
        this.fuelChangeGal = fuelChangeGal;
    }

    public ActionType getType() {
        return type;
    }

    public Duration getDuration() {
        return duration;
    }

    public Position getEndPosition() {
        return endPosition;
    }

    public double getGlpChangeM3() {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        switch (type) {
            case DRIVING:
                String pathLength = (path != null) ? String.format("| %d nodes", path.size()) : "";
                String distanceKm = (path != null && path.size() > 1)
                        ? String.format("| %04d km", (path.size() - 1) * Constants.NODE_DISTANCE)
                        : "";
                String fuelInfo = (fuelChangeGal != 0) ? String.format("| Fuel: %.2f gal", Math.abs(fuelChangeGal))
                        : "";

                sb.append(String.format("🚗  DRIVING     | To: %-15s | Time: %3d min %s %s %s",
                        endPosition, duration.toMinutes(), pathLength, distanceKm, fuelInfo));
                break;

            case REFUELING:
                sb.append(String.format("⛽  REFUELING   | Location: %-10s | Time: %3d min | Full tank restored",
                        endPosition, duration.toMinutes()));
                break;

            case REFILLING:
                sb.append(String.format("🛢️  REFILLING   | Location: %-10s | Time: %3d min | GLP: +%d m³",
                        endPosition, duration.toMinutes(), glpChangeM3));
                break;

            case SERVING:
                String orderDetails = "";
                if (order != null) {
                    orderDetails = String.format("| Client: %s | Due: %s",
                            order.getId(),
                            order.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
                }

                sb.append(String.format("🛒  SERVING     | Location: %-10s | Time: %3d min | GLP: -%d m³ %s",
                        endPosition, duration.toMinutes(), Math.abs(glpChangeM3), orderDetails));
                break;

            case MAINTENANCE:
                sb.append(String.format("🔧  MAINTENANCE | Location: %-10s | Time: %3d min | Routine service",
                        endPosition, duration.toMinutes()));
                break;

            case IDLE:
                sb.append(String.format("⏸️  IDLE        | Location: %-10s | Time: %3d min | Waiting",
                        endPosition, duration.toMinutes()));
                break;

            case STORAGE_CHECK:
                sb.append(String.format("🔍  STORAGE CHK | Location: %-10s | Time: %3d min | Inventory verification",
                        endPosition, duration.toMinutes()));
                break;

            case TRANSFERRING:
                sb.append(String.format("🔄  TRANSFERRING| Location: %-10s | Time: %3d min | Resource transfer",
                        endPosition, duration.toMinutes()));
                break;
        }

        return sb.toString();
    }
}
