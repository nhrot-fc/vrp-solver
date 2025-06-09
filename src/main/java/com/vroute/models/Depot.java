package com.vroute.models;

public class Depot implements Stop {
    // unmutable attributes
    private final Position position;
    private final String id;
    protected final int glpCapacityM3;
    protected final boolean canRefuel;
    // mutable attributes
    protected int currentGlpM3;

    public Depot(String id, Position position, int glpCapacityM3, boolean canRefuel) {
        this.id = id;
        this.position = position;
        this.glpCapacityM3 = glpCapacityM3;
        this.canRefuel = canRefuel;
        this.currentGlpM3 = 0;
    }

    // Getters
    public String getId() { return id; }
    public int getGlpCapacityM3() { return glpCapacityM3; }
    public int getCurrentGlpM3() { return currentGlpM3; }
    public boolean canRefuel() { return canRefuel; }

    // Operations
    public void refillGLP() {
        this.currentGlpM3 = glpCapacityM3;
    }

    public void serveGLP(int amountM3) {
        this.currentGlpM3 -= amountM3;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public String toString() {
        String refuelIcon = canRefuel ? "⛽" : "";
        return String.format("🏭 %s %s [GLP: %d/%d m³] %s", 
                id, 
                refuelIcon,
                currentGlpM3, 
                glpCapacityM3, 
                position);
    }

    public Depot clone() {
        Depot clonedDepot = new Depot(this.id, this.position, this.glpCapacityM3, this.canRefuel);
        clonedDepot.currentGlpM3 = this.currentGlpM3;
        return clonedDepot;
    }
}
