package com.vroute.solution;

import java.time.LocalDateTime;

import com.vroute.models.Position;

public class OrderStop implements RouteStop {
    private final String orderId;
    private final Position position;
    private final LocalDateTime arrivalTime;
    private final int glpDelivery;

    public OrderStop(String orderId, Position position, LocalDateTime arrivalTime, int glpDelivery) {
        this.orderId = orderId;
        this.position = position;
        this.arrivalTime = arrivalTime;
        this.glpDelivery = glpDelivery;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public String getEntityID() {
        return orderId;
    }

    @Override
    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public String toString() {
        return "OrderStop{" +
                "orderId='" + orderId + '\'' +
                ", position=" + position +
                ", arrivalTime=" + arrivalTime +
                '}';
    }

    public int getGlpDelivery() {
        return glpDelivery;
    }
}
