package com.vroute.utils;

import com.vroute.models.Posicion;

public final class Constants {

    // City dimensions
    public static final int CITY_WIDTH_KM = 70;
    public static final int CITY_HEIGHT_KM = 50;

    // Operational constants
    public static final double AVERAGE_SPEED_KMPH = 50.0;
    public static final int FUEL_CONSUMPTION_DIVISOR = 180;
    public static final int CUSTOMER_DELIVERY_TIME_MINUTES = 15;
    public static final int ROUTINE_MAINTENANCE_TIME_MINUTES = 15;
    public static final int TRANSFER_TIME_MINUTES = 15;
    public static final int MINIMUM_DELIVERY_HOURS = 4;

    // Supply points
    public static final Posicion MAIN_PLANT_LOCATION = new Posicion(12, 8);
    public static final Posicion NORTH_TANK_LOCATION = new Posicion(42, 42);
    public static final Posicion EAST_TANK_LOCATION = new Posicion(63, 3);
    public static final int INTERMEDIATE_TANK_CAPACITY_M3 = 160;

    private Constants() {
        // Private constructor to prevent instantiation
    }
}
