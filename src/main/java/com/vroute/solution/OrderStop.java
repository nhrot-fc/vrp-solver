package com.vroute.solution;

import com.vroute.models.Position;
import com.vroute.models.Order;

import java.util.List;

import com.vroute.models.Depot;

public class OrderStop implements RouteStop {
    private final Order order;
    private final Position position;
    private final int glpDelivery;
    private List<Position> path;

    public OrderStop(Order order, Position position, int glpDelivery) {
        this.order = order;
        this.position = position;
        this.glpDelivery = glpDelivery;
    }

    @Override
    public void setPath(List<Position> path) {
        this.path = path;
    }

    @Override
    public List<Position> getPath() {
        return path;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public Order getOrder() {
        return order;
    }

    @Override
    public Depot getDepot() {
        return null;
    }

    @Override
    public int getGlpRecharge() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("📦 %s [GLP: %d m³] %s", order.getId(), glpDelivery, position);
    }

    @Override
    public int getGlpDelivery() {
        return glpDelivery;
    }

    @Override
    public OrderStop clone() {
        return new OrderStop(order, position, glpDelivery);
    }
}
