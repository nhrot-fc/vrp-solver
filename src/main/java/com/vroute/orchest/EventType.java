package com.vroute.orchest;

public enum EventType {
    ORDER_ARRIVAL, // New order arrives
    VEHICLE_BREAKDOWN, // Vehicle breaks down
    BLOCKAGE_START, // Road blockage begins
    BLOCKAGE_END, // Road blockage ends
    MAINTENANCE_START, // Vehicle maintenance begins
    MAINTENANCE_END, // Vehicle maintenance ends
    GLP_DEPOT_REFILL, // GLP depot gets refilled
    SIMULATION_END // End of simulation
}