package com.vroute.solution;

import com.vroute.models.Position;
import com.vroute.models.Order;

import java.util.List;

import com.vroute.models.Depot;

public interface RouteStop {
    void setPath(List<Position> path);

    List<Position> getPath();

    Position getPosition();

    RouteStop clone();

    // OrderStop
    Order getOrder();

    int getGlpDelivery();

    // DepotStop
    Depot getDepot();

    int getGlpRecharge();
}
