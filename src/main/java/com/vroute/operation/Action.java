package com.vroute.operation;

import java.time.Duration;

import java.time.LocalDateTime;
import com.vroute.models.Vehicle;
import com.vroute.models.Stop;

public abstract class Action {
    // Atributos comunes a TODA acción
    protected final ActionType type;
    protected final LocalDateTime startTime;
    protected final LocalDateTime endTime;

    // Estado del vehículo al iniciar la acción
    protected final Vehicle vehicleState;

    public Action(ActionType type, LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicleState) {
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.vehicleState = vehicleState;
    }

    public ActionType getType() {
        return type;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Vehicle getVehicleState() {
        return vehicleState;
    }

    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    // Métodos abstractos que cada subclase deberá implementar
    public abstract Stop getDestination();

    public abstract String getDescription();
}