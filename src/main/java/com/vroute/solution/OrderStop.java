package com.vroute.solution;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.vroute.models.Constants;
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
        return String.format("📦 %s [%s] [GLP: %d m³] %s", 
                orderId, 
                arrivalTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)), 
                glpDelivery, 
                position);
    }

    public int getGlpDelivery() {
        return glpDelivery;
    }

    @Override
    public OrderStop clone() {
        return new OrderStop(orderId, position, arrivalTime, glpDelivery);
    }
}
