package com.vroute.operation;

import java.time.Duration;
import java.util.List;

import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

public class ActionFactory {

    public static Action createDrivingAction(List<Position> path, Duration duration, double fuelConsumedGal) {
        Position endPosition = path.isEmpty() ? null : path.get(path.size() - 1);
        return new Action(ActionType.DRIVE, path, endPosition, duration, null, 0,
                -Math.abs(fuelConsumedGal));
    }

    public static Action createRefuelingAction(Depot depot, Vehicle vehicle) {
        Position endPosition = depot.getPosition();
        long refuelMinutes = Constants.REFUEL_DURATION_MINUTES;
        Duration duration = Duration.ofMinutes(refuelMinutes);
        double fuelChangeGal = vehicle.getFuelCapacityGal() - vehicle.getCurrentFuelGal();
        return new Action(ActionType.REFUEL, null, endPosition, duration, null, 0, fuelChangeGal);
    }

    public static Action createRefillingAction(Depot depot, int glpAmountAdded) {
        Position endPosition = depot.getPosition();
        long transferMinutes = Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES;
        Duration duration = Duration.ofMinutes(transferMinutes);
        return new Action(ActionType.RELOAD, null, endPosition, duration, null, glpAmountAdded, 0);
    }

    public static Action createServingAction(Position position, Order order, int glpDispensedM3) {
        Duration duration = Duration.ofMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
        // GLP change for the vehicle is negative when dispensing
        return new Action(ActionType.SERVE, null, position, duration, order, -Math.abs(glpDispensedM3), 0);
    }

    public static Action createIdleAction(Position position, Duration duration) {
        return new Action(ActionType.WAIT, null, position, duration, null, 0, 0);
    }

    public static Action createMaintenanceAction(Position position, Duration duration) {
        return new Action(ActionType.MAINTENANCE, null, position, duration, null, 0, 0);
    }
}