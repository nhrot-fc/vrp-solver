package com.vroute.operation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.vroute.models.Vehicle;
public class VehiclePlan {
    private final Vehicle vehicle;
    private final List<Action> actions; // La secuencia ordenada de acciones polimórficas
    private final LocalDateTime startTime; // Hora de inicio del plan
    // Atributos clave pre-calculados por el "Evaluador de Soluciones"
    private final boolean isFeasible; // ¿Cumple TODAS las restricciones?
    private final double cost; // El costo total de esta ruta (ej. costo de combustible)
    private final double totalDistanceKm;
    private final Duration totalDuration;

    public VehiclePlan(Vehicle vehicle, List<Action> actions, LocalDateTime startTime, boolean isFeasible, double cost, double totalDistanceKm, Duration totalDuration) {
        this.vehicle = vehicle;
        this.actions = actions;
        this.startTime = startTime;
        this.isFeasible = isFeasible;
        this.cost = cost;
        this.totalDistanceKm = totalDistanceKm;
        this.totalDuration = totalDuration;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }
    public boolean isFeasible() {
        return isFeasible;
    }
    public double getCost() {
        return cost;
    }
    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }
    public Duration getTotalDuration() {
        return totalDuration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        if (actions.isEmpty()) {
            return startTime;
        }
        Action lastAction = actions.get(actions.size() - 1);
        return lastAction.getEndTime();
    }

}