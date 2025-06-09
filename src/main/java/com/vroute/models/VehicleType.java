package com.vroute.models;

public enum VehicleType {
    TA(25, 2.5, 12.5), // capacityM3, tareWeightTon, maxGlpWeightTon
    TB(15, 2.0, 7.5),
    TC(10, 1.5, 5.0),
    TD(5, 1.0, 2.5);

    private final int capacityM3;
    private final double tareWeightTon;
    private final double maxGlpWeightTon; // Weight of GLP when tank is full

    VehicleType(int capacityM3, double tareWeightTon, double maxGlpWeightTon) {
        this.capacityM3 = capacityM3;
        this.tareWeightTon = tareWeightTon;
        this.maxGlpWeightTon = maxGlpWeightTon;
    }

    public int getCapacityM3() { return capacityM3; }
    public double getTareWeightTon() { return tareWeightTon; }
    public double getMaxGlpWeightTon() { return maxGlpWeightTon; }

    public double getCombinedWeightWhenFullTon() {
        return this.tareWeightTon + this.maxGlpWeightTon;
    }

    // GLP density: e.g., TA: 12.5 Ton / 25 m³ = 0.5 Ton/m³
    // This density is consistent across all vehicle types based on README data.
    public static final double GLP_DENSITY_TON_PER_M3 = 0.5;

    public double convertGlpM3ToTon(int glpM3) {
        return glpM3 * GLP_DENSITY_TON_PER_M3;
    }
    
    @Override
    public String toString() {
        return String.format("%s [🚚 %.1f t | 🛢️ %d m³]", 
                name(), 
                tareWeightTon, 
                capacityM3);
    }
}
