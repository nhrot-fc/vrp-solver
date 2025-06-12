package com.vroute.operation;

public enum ActionType {
    DRIVE, // Vehicle is driving to a position
    REFUEL, // Vehicle is refueling with gasoline
    RELOAD, // Vehicle is refilling its GLP tank
    SERVE, // Vehicle is serving an order (discharging GLP)
    MAINTENANCE, // Vehicle is undergoing maintenance
    WAIT, // Vehicle is waiting
}