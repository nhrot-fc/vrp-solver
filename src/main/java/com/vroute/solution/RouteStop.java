package com.vroute.solution;

import com.vroute.models.Position;
import com.vroute.models.Order;
import com.vroute.models.Depot;

public interface RouteStop {
    Position getPosition();

    RouteStop clone();

    // OrderStop
    Order getOrder();

    int getGlpDelivery();

    // DepotStop
    Depot getDepot();

    int getGlpRecharge();
}
