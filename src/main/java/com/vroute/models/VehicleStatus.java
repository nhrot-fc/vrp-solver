package com.vroute.models;

/**
 * Enumeration of possible status values for vehicles.
 * Each status corresponds to one or more vehicle actions.
 */
public enum VehicleStatus {
    AVAILABLE("✅"), // Vehicle is ready for a new task
    DRIVING("🚗"), // Vehicle is driving
    SERVING("🛒"), // Vehicle is serving an order
    MAINTENANCE("🔧"), // Vehicle is undergoing maintenance
    REFUELING("⛽"), // Vehicle is refueling
    RELOADING("🛢️"), // Vehicle is reloading GLP
    IDLE("⏸️"), // Vehicle is idle, waiting for next task
    UNAVAILABLE("🚫"); // Vehicle is not available for any task
    
    private final String icon;
    
    VehicleStatus(String icon) {
        this.icon = icon;
    }
    
    public String getIcon() {
        return icon;
    }
    
    @Override
    public String toString() {
        return icon + " " + name();
    }
}