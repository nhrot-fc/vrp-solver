package com.vroute.solution;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.vroute.models.Vehicle;

public class Route {
    private final Vehicle vehicle;
    private final List<RouteStop> stops;
    private final LocalDateTime startTime;

    public Route(Vehicle vehicle, List<RouteStop> stops, LocalDateTime startTime) {
        this.vehicle = vehicle;
        this.stops = stops;
        this.startTime = startTime;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<RouteStop> getStops() {
        return stops;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return String.format("🛣️ [Vehicle: %s, Stops: %d, Start Time: %s]",
                vehicle.getId(),
                stops.size(),
                startTime);
    }

    @Override
    public Route clone() {
        List<RouteStop> clonedStops = new ArrayList<>();
        for (RouteStop stop : stops) {
            clonedStops.add(stop.clone());
        }
        return new Route(vehicle.clone(), clonedStops, startTime);
    }
}
