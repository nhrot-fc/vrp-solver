package com.vroute.operation;

import java.time.LocalDateTime;
import java.util.List;
import com.vroute.models.Position;
import com.vroute.models.Stop;
import com.vroute.models.Vehicle;

public class DriveAction extends Action {
    private final Stop destination;
    private final List<Position> path; // La secuencia de nodos a seguir
    private final double fuelChangeGal; // El consumo específico de este viaje

    public DriveAction(LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicleState, List<Position> path, Stop destination, double fuelChange) {
        super(ActionType.DRIVE, startTime, endTime, vehicleState);
        this.destination = destination;
        this.path = path;
        this.fuelChangeGal = fuelChange;
    }

    public List<Position> getPath() {
        return path;
    }
    
    public double getFuelChangeGal() {
        return fuelChangeGal;
    }

    @Override
    public Stop getDestination() { return this.destination; }
    
    @Override
    public String getDescription() {
        return "Conducir hacia " + destination.toString();
    }
}