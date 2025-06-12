package com.vroute.operation;

import java.time.LocalDateTime;
import com.vroute.models.Depot;
import com.vroute.models.Stop;
import com.vroute.models.Vehicle;

public class ReloadAction extends Action {
    private final Depot depot;
    private final int glpAddedM3;

    public ReloadAction(LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicleState, Depot depot, int glpAddedM3) {
        super(ActionType.RELOAD, startTime, endTime, vehicleState);
        this.depot = depot;
        this.glpAddedM3 = glpAddedM3;
    }

    public int getGlpAddedM3() {
        return glpAddedM3;
    }

    @Override
    public Stop getDestination() {
        return depot;
    }

    @Override
    public String getDescription() {
        return String.format("Recargar %d m³ de GLP en %s", glpAddedM3, depot.toString());
    }
}
