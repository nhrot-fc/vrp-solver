package com.vroute.models;

public class Constants {
    /*
     * =============================================
     * TIEMPOS Y DURACIONES
     * =============================================
     */
    // Tiempos en horas
    public static final int MAINTENANCE_DURATION_HOURS = 24;
    public static final int INCIDENT_TYPE_1_IMMOBILIZATION_HOURS = 2;
    public static final int INCIDENT_TYPE_2_IMMOBILIZATION_HOURS = 2;
    public static final int INCIDENT_TYPE_3_IMMOBILIZATION_HOURS = 4;
    public static final int MIN_PACKAGE_DELIVERY_TIME_HOURS = 4; // Initial suggested value

    // Tiempos en minutos
    public static final int GLP_SERVE_DURATION_MINUTES = 15;
    public static final int REFUEL_DURATION_MINUTES = 1;
    public static final int VEHICLE_GLP_TRANSFER_DURATION_MINUTES = 15;
    public static final int DEPOT_GLP_TRANSFER_TIME_MINUTES = 1;
    public static final int ROUTINE_MAINTENANCE_MINUTES = 5;

    /*
     * =============================================
     * LOCALIZACIÓN Y CONFIGURACIÓN GEOGRÁFICA
     * =============================================
     */
    // City Configuration
    public static final int CITY_LENGTH_X = 70; // Km
    public static final int CITY_WIDTH_Y = 50; // Km
    public static final int NODE_DISTANCE = 1; // Km

    // Storage Locations
    public static final Position CENTRAL_STORAGE_LOCATION = new Position(12, 8);
    public static final Position NORTH_INTERMEDIATE_STORAGE_LOCATION = new Position(42, 42);
    public static final Position EAST_INTERMEDIATE_STORAGE_LOCATION = new Position(63, 3);

    /*
     * =============================================
     * CAPACIDADES Y CARACTERÍSTICAS DE VEHÍCULOS
     * =============================================
     */
    // Vehículos - General
    public static final double VEHICLE_FUEL_CAPACITY_GAL = 25.0; // Gallons
    public static final double VEHICLE_AVG_SPEED = 80.0; // Km/h
    public static final String VEHICLE_CODE_FORMAT = "TTNN";
    public static final double CONSUMPTION_FACTOR = 360.0;

    // Capacidades GLP
    public static final int TA_GLP_CAPACITY_M3 = 25; // m³
    public static final int TB_GLP_CAPACITY_M3 = 15; // m³
    public static final int TC_GLP_CAPACITY_M3 = 10; // m³
    public static final int TD_GLP_CAPACITY_M3 = 5; // m³

    // Cantidades de vehículos
    public static final int TA_UNITS = 2;
    public static final int TB_UNITS = 4;
    public static final int TC_UNITS = 4;
    public static final int TD_UNITS = 10;

    /*
     * =============================================
     * PESOS Y MEDIDAS
     * =============================================
     */
    // Pesos para vehículos tipo TA
    public static final double TA_GROSS_WEIGHT_TARA_TON = 2.5; // Ton
    public static final double TA_GLP_WEIGHT_TON = 12.5; // Ton
    public static final double TA_COMBINED_WEIGHT_TON = 15.0; // Ton

    // Pesos para vehículos tipo TB
    public static final double TB_GROSS_WEIGHT_TARA_TON = 2.0; // Ton
    public static final double TB_GLP_WEIGHT_TON = 7.5; // Ton
    public static final double TB_COMBINED_WEIGHT_TON = 9.5; // Ton

    // Pesos para vehículos tipo TC
    public static final double TC_GROSS_WEIGHT_TARA_TON = 1.5; // Ton
    public static final double TC_GLP_WEIGHT_TON = 5.0; // Ton
    public static final double TC_COMBINED_WEIGHT_TON = 6.5; // Ton

    // Pesos para vehículos tipo TD
    public static final double TD_GROSS_WEIGHT_TARA_TON = 1.0; // Ton
    public static final double TD_GLP_WEIGHT_TON = 2.5; // Ton
    public static final double TD_COMBINED_WEIGHT_TON = 3.5; // Ton

    /*
     * =============================================
     * FORMATOS Y CONFIGURACIÓN DE ARCHIVOS
     * =============================================
     */
    // Archivos de mantenimiento
    public static final String PREVENTIVE_MAINTENANCE_FILE_NAME = "mantpreventivo";
    public static final String PREVENTIVE_MAINTENANCE_FILE_FORMAT = "aaaammdd:TTNN";

    // Archivos de pedidos
    public static final String ORDER_FILE_BASE_NAME = "ventas2025mm";
    public static final String ORDER_FILE_FORMAT = "##d##h##m:posx,posY,c-idCliente, m3, hLímite";

    // Archivos de incidentes
    public static final String INCIDENT_FILE_NAME = "averias.txt";
    public static final String INCIDENT_FILE_FORMAT = "tt_######_ti";

    // Otros archivos
    public static final String STREET_CLOSURE_FILE_BASE_NAME = "aaaamm.bloqueadas";

    /*
     * =============================================
     * PARÁMETROS OPERACIONALES
     * =============================================
     */
    // Parámetros de incidentes
    public static final double INCIDENT_ROUTE_OCCURRENCE_MIN_PERCENTAGE = 0.05;
    public static final double INCIDENT_ROUTE_OCCURRENCE_MAX_PERCENTAGE = 0.35;

    /*
     * =============================================
     * IDENTIFICADORES Y CONSTANTES MATEMÁTICAS
     * =============================================
     */
    // IDs
    public static final String MAIN_PLANT_ID = "MAIN_PLANT"; // ID for the main plant

    // Constantes matemáticas
    public static final double EPSILON = 1e-9; // For floating-point comparisons

    private Constants() {
        // Avoid initialization
    }
}
