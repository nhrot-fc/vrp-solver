package com.vroute.operation;

import java.time.LocalDateTime;
import com.vroute.models.Depot;
import com.vroute.models.Stop;
import com.vroute.models.Vehicle;

public class RefuelAction extends Action {
    private final Depot depot;
    private final double fuelAddedGal;

    public RefuelAction(LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicleState, Depot depot) {
        super(ActionType.REFUEL, startTime, endTime, vehicleState);
        this.depot = depot;
        this.fuelAddedGal = vehicleState.getFuelCapacityGal() - vehicleState.getCurrentFuelGal();
    }

    public double getFuelAddedGal() {
        return fuelAddedGal;
    }

    @Override
    public Stop getDestination() {
        return depot;
    }

    @Override
    public String getDescription() {
        return String.format("Repostar %.2f galones de combustible en %s", fuelAddedGal, depot.toString());
    }
}
