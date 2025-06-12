package com.vroute.operation;

import java.time.Duration;
import java.util.List;

import com.vroute.models.Constants;
import com.vroute.models.Order;
import com.vroute.models.Position;

public class Action {
    // General attributes for all actions
    private final ActionType type;
    private final Duration duration;
    private final Position destination;

    // Serving specific attributes
    private final int glpChangeM3;
    private final Order order;

    // Driving specific attributes
    private final double fuelChangeGal;
    private final List<Position> path;

    public Action(ActionType type, List<Position> path, Position endPosition,
            Duration duration, Order order, int glpChangeM3, double fuelChangeGal) {
        this.type = type;
        this.path = path;
        this.destination = endPosition;
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

    public Position getDestination() {
        return destination;
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
            case DRIVE:
                String pathLength = (path != null) ? String.format("| %d nodes", path.size()) : "";
                String distanceKm = (path != null && path.size() > 1)
                        ? String.format("| %04d km", (path.size() - 1) * Constants.NODE_DISTANCE)
                        : "";
                String fuelInfo = (fuelChangeGal != 0) ? String.format("| Fuel: %.2f gal", Math.abs(fuelChangeGal))
                        : "";

                sb.append(String.format("🚗  DRIVING     | To: %-15s | Time: %3d min %s %s %s",
                        destination, duration.toMinutes(), pathLength, distanceKm, fuelInfo));
                break;

            case REFUEL:
                sb.append(String.format("⛽  REFUELING   | Location: %-10s | Time: %3d min | Full tank restored",
                        destination, duration.toMinutes()));
                break;

            case RELOAD:
                sb.append(String.format("🛢️  REFILLING   | Location: %-10s | Time: %3d min | GLP: +%d m³",
                        destination, duration.toMinutes(), glpChangeM3));
                break;

            case SERVE:
                String orderDetails = "";
                if (order != null) {
                    orderDetails = String.format("| Client: %s | Due: %s",
                            order.getId(),
                            order.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
                }

                sb.append(String.format("🛒  SERVING     | Location: %-10s | Time: %3d min | GLP: -%d m³ %s",
                        destination, duration.toMinutes(), Math.abs(glpChangeM3), orderDetails));
                break;

            case MAINTENANCE:
                sb.append(String.format("🔧  MAINTENANCE | Location: %-10s | Time: %3d min | Routine service",
                        destination, duration.toMinutes()));
                break;

            case WAIT:
                sb.append(String.format("⏸️  IDLE        | Location: %-10s | Time: %3d min | Waiting",
                        destination, duration.toMinutes()));
                break;
        }

        return sb.toString();
    }
}
