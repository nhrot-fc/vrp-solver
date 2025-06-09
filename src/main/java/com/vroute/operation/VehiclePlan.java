package com.vroute.operation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleStatus;

public class VehiclePlan {
    private final Vehicle vehicle;
    private final List<VehicleAction> actions;
    private final LocalDateTime startTime;
    private final List<Order> servedOrders;
    private final double totalDistanceKm;
    private final double totalGlpDeliveredM3;
    private final double totalFuelConsumedGal;

    public VehiclePlan(Vehicle vehicle, List<VehicleAction> actions, LocalDateTime startTime) {
        this.vehicle = vehicle;
        this.actions = new ArrayList<>(actions);
        this.startTime = startTime;

        List<Order> servedOrdersList = new ArrayList<>();
        double distKm = 0;
        int glpDelivered = 0;
        double fuelConsumed = 0;

        for (VehicleAction action : this.actions) {
            if (action.getType() == ActionType.SERVING && action.getOrder() != null) {
                servedOrdersList.add(action.getOrder());
                glpDelivered += Math.abs(action.getGlpChangeM3());
            }
            if (action.getType() == ActionType.DRIVING) {
                Position pathStart = action.getPath().getFirst();
                for (Position pos : action.getPath()) {
                    distKm += pathStart.distanceTo(pos);
                    pathStart = pos;
                }
                fuelConsumed += Math.abs(action.getFuelChangeGal());
            }
        }
        this.servedOrders = Collections.unmodifiableList(servedOrdersList);
        this.totalDistanceKm = distKm;
        this.totalGlpDeliveredM3 = glpDelivered;
        this.totalFuelConsumedGal = fuelConsumed;
    }

    // Getters
    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<VehicleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<Order> getServedOrders() {
        return Collections.unmodifiableList(servedOrders);
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getTotalGlpDeliveredM3() {
        return totalGlpDeliveredM3;
    }

    public double getTotalFuelConsumedGal() {
        return totalFuelConsumedGal;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Position getFinalPosition() {
        return actions.isEmpty() ? vehicle.getCurrentPosition() : actions.get(actions.size() - 1).getEndPosition();
    }

    public VehicleStatus getStatusAt(LocalDateTime time) {
        if (this.actions.isEmpty()) {
            return this.vehicle.getStatus();
        }

        if (time.isBefore(this.startTime)) {
            return this.vehicle.getStatus();
        }

        LocalDateTime currentActionStartTime = this.startTime;
        for (VehicleAction action : this.actions) {
            LocalDateTime currentActionEndTime = currentActionStartTime.plus(action.getDuration());

            if (!time.isBefore(currentActionStartTime) && time.isBefore(currentActionEndTime)) {
                switch (action.getType()) {
                    case DRIVING:
                        return VehicleStatus.DRIVING;
                    case REFUELING:
                        return VehicleStatus.REFUELING;
                    case REFILLING:
                        return VehicleStatus.REFILLING;
                    case SERVING:
                        return VehicleStatus.SERVING;
                    case MAINTENANCE:
                        return VehicleStatus.MAINTENANCE;
                    case TRANSFERRING:
                        return VehicleStatus.TRANSFERRING;
                    case IDLE:
                        return VehicleStatus.IDLE;
                    case STORAGE_CHECK:
                        return VehicleStatus.MAINTENANCE;
                    default:
                        return VehicleStatus.UNAVAILABLE;
                }
            }
            currentActionStartTime = currentActionEndTime; // Move to the start time of the next action
        }

        return VehicleStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Vehicle Plan for %s [%.1f km, %.1f m³ GLP, %.1f gal fuel]",
                vehicle.getId(), totalDistanceKm, totalGlpDeliveredM3, totalFuelConsumedGal));

        LocalDateTime currentActionTime = this.startTime;
        for (int i = 0; i < actions.size(); i++) {
            VehicleAction action = actions.get(i);
            sb.append(String.format("\n%d. [%s] %s",
                    i + 1,
                    currentActionTime.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                    action.toString())); // VehicleAction already has a good toString()
            currentActionTime = currentActionTime.plus(action.getDuration());
        }
        return sb.toString();
    }
}
