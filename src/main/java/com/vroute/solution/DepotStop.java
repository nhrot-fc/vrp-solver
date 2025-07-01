package com.vroute.solution;

import java.util.List;

import com.vroute.models.Depot;
import com.vroute.models.Order;
import com.vroute.models.Position;

public class DepotStop implements RouteStop {
    private final Depot depot;
    private final int glpRecharge;
    private List<Position> path;

    public DepotStop(Depot depot, int glpRecharge) {
        this.depot = depot;
        this.glpRecharge = glpRecharge;
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
        return depot.getPosition();
    }

    @Override
    public Order getOrder() {
        return null;
    }

    @Override
    public Depot getDepot() {
        return depot;
    }

    @Override
    public int getGlpRecharge() {
        return glpRecharge;
    }

    @Override
    public int getGlpDelivery() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("🏭 %s [GLP: +%d m³] %s", depot.getId(), glpRecharge, depot.getPosition());
    }

    @Override
    public DepotStop clone() {
        return new DepotStop(depot.clone(), glpRecharge);
    }
}
