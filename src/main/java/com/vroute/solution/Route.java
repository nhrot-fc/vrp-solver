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

    public List<OrderStop> getOrderStops() {
        List<OrderStop> orderStops = new ArrayList<>();
        for (RouteStop stop : stops) {
            if (stop instanceof OrderStop) {
                orderStops.add((OrderStop) stop);
            }
        }
        return orderStops;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🛣️ [Vehicle: ").append(vehicle.getId()).append(", Stops: ").append(stops.size())
                .append(", Start Time: ").append(startTime).append("]\n");
        for (RouteStop stop : stops) {
            sb.append("\t").append(stop.toString()).append("\n");
        }
        return sb.toString();
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
