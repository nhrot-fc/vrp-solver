package com.vroute.operation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Stop;
import com.vroute.models.Vehicle;

public class ActionFactory {

    public static Action createDrivingAction(LocalDateTime startTime, List<Position> path, Stop destination,
            Duration duration, Vehicle vehicle,
            double fuelConsumedGal) {

        return new DriveAction(startTime, startTime.plus(duration), vehicle, path, destination, fuelConsumedGal);
    }

    public static Action createRefuelingAction(LocalDateTime startTime, Depot depot, Vehicle vehicle) {
        Duration duration = Duration.ofMinutes(Constants.REFUEL_DURATION_MINUTES);
        return new RefuelAction(startTime, startTime.plus(duration), vehicle, depot);
    }

    public static Action createRefillingAction(LocalDateTime startTime, Depot depot, Vehicle vehicle,
            int glpAmountAdded) {
        Duration duration = Duration.ofMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
        return new ReloadAction(startTime, startTime.plus(duration), vehicle, depot, glpAmountAdded);
    }

    public static Action createServingAction(LocalDateTime startTime, Position position, Vehicle vehicle, Order order,
            int glpDispensedM3) {
        Duration duration = Duration.ofMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
        return new ServeAction(startTime, startTime.plus(duration), vehicle, order, glpDispensedM3);
    }

    public static Action createIdleAction(LocalDateTime startTime, Position position, Vehicle vehicle,
            Duration duration) {
        return new WaitAction(startTime, startTime.plus(duration), vehicle);
    }
}