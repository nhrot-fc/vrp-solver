package com.vroute.orchest;

public enum EventType {
    // Global events
    ORDER_ARRIVAL,
    INCIDENT,
    MAINTENANCE,
    BLOCKAGE_START,
    BLOCKAGE_END,
    GLP_DEPOT_REFILL,

    // Simulation events
    ORDER_DELIVERED,
    GLP_DEPOT_UPDATED,
}