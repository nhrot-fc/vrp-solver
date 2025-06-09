package com.vroute.operation;

public enum ActionType {
    DRIVING, // Vehicle is driving to a position
    REFUELING, // Vehicle is refueling with gasoline
    REFILLING, // Vehicle is refilling its GLP tank
    SERVING, // Vehicle is serving an order (discharging GLP)
    MAINTENANCE, // Vehicle is undergoing maintenance
    TRANSFERRING, // Vehicle is transferring GLP to/from another vehicle
    IDLE, // Vehicle is idle
    STORAGE_CHECK // Vehicle is performing storage checks
}