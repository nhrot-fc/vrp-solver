package com.vroute.operation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

public class ActionFactory {

    public static Action createDrivingAction(List<Position> path, double fuelConsumedGal, LocalDateTime startTime,
            LocalDateTime endTime) {
        Position endPosition = path.isEmpty() ? null : path.get(path.size() - 1);
        return new Action(ActionType.DRIVE, path, endPosition, startTime, endTime, null, 0,
                -Math.abs(fuelConsumedGal));
    }

    public static Action createRefuelingAction(Depot depot, Vehicle vehicle, LocalDateTime startTime) {
        Position endPosition = depot.getPosition();
        long refuelMinutes = Constants.REFUEL_DURATION_MINUTES;
        LocalDateTime endTime = startTime.plusMinutes(refuelMinutes);
        double fuelChangeGal = vehicle.getFuelCapacityGal() - vehicle.getCurrentFuelGal();
        return new Action(ActionType.REFUEL, null, endPosition, startTime, endTime, null, 0, fuelChangeGal);
    }

    public static Action createRefillingAction(Depot depot, int glpAmountAdded, LocalDateTime startTime) {
        Position endPosition = depot.getPosition();
        long transferMinutes = Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES;
        LocalDateTime endTime = startTime.plusMinutes(transferMinutes);
        return new Action(ActionType.RELOAD, null, endPosition, startTime, endTime, null, glpAmountAdded, 0);
    }

    public static Action createServingAction(Position position, Order order, int glpDispensedM3,
            LocalDateTime startTime) {
        long serveMinutes = Constants.GLP_SERVE_DURATION_MINUTES;
        LocalDateTime endTime = startTime.plusMinutes(serveMinutes);
        return new Action(ActionType.SERVE, null, position, startTime, endTime, order, -Math.abs(glpDispensedM3), 0);
    }

    public static Action createIdleAction(Position position, Duration duration, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plus(duration);
        return new Action(ActionType.WAIT, null, position, startTime, endTime, null, 0, 0);
    }

    public static Action createMaintenanceAction(Position position, Duration duration, LocalDateTime startTime) {
        LocalDateTime endTime = startTime.plus(duration);
        return new Action(ActionType.MAINTENANCE, null, position, startTime, endTime, null, 0, 0);
    }
}