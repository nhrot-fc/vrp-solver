package com.vroute.solution;

import java.util.List;

import com.vroute.models.Vehicle;

public class Route {
    private final String id;
    private final Vehicle vehicle;
    private final List<RouteStop> stops;

    public Route(String id, Vehicle vehicle, List<RouteStop> stops) {
        this.id = id;
        this.vehicle = vehicle;
        this.stops = stops;
    }

    public String getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<RouteStop> getStops() {
        return stops;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", vehicle=" + vehicle +
                ", stops=" + stops +
                '}';
    }
}
