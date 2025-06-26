package com.vroute.solution;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Position;

public class DepotStop implements RouteStop {
    private final Depot depot;
    private final LocalDateTime arrivalTime;
    private final int glpRecharge;

    public DepotStop(Depot depot, LocalDateTime arrivalTime, int glpRecharge) {
        this.depot = depot;
        this.arrivalTime = arrivalTime;
        this.glpRecharge = glpRecharge;
    }

    @Override
    public Position getPosition() {
        return depot.getPosition();
    }

    @Override
    public String getEntityID() {
        return depot.getId();
    }

    @Override
    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public int getGlpRecharge() {
        return glpRecharge;
    }

    @Override
    public String toString() {
        return String.format("🏭 %s [%s] [GLP: +%d m³] %s", 
                depot.getId(), 
                arrivalTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)), 
                glpRecharge, 
                depot.getPosition());
    }

    @Override
    public DepotStop clone() {
        return new DepotStop(depot, arrivalTime, glpRecharge);
    }
}
