package com.vroute.orchest;

public enum EventType {

    // Global events
    ORDER_ARRIVAL, // New order arrives
    VEHICLE_BREAKDOWN, // Vehicle breaks down
    BLOCKAGE_START, // Road blockage begins
    BLOCKAGE_END, // Road blockage ends
    MAINTENANCE_START, // Vehicle maintenance begins
    MAINTENANCE_END, // Vehicle maintenance ends
    GLP_DEPOT_REFILL, // GLP depot gets refilled
    SIMULATION_END, // End of simulation

    // Simulation events
    ORDER_DELIVERED, // Order is delivered
    GLP_DEPOT_UPDATED, // GLP depot status updated
    VEHICLE_ARRIVAL, // Vehicle arrives at a location
    VEHICLE_DEPARTURE, // Vehicle departs from a location
    VEHICLE_ARRIVES_MAIN_DEPOT
    
}