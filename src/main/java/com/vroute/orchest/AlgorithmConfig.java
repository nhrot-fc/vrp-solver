package com.vroute.orchest;

/**
 * Holds configuration parameters for the routing and planning algorithms.
 */
public class AlgorithmConfig {

    // Algorithm execution parameters
    private int algorithmJumpValue; // Sa - Salto del algoritmo
    private int consumptionJumpValue; // Sc - Salto del consumo
    private int executionTimeSeconds; // Ta - Tiempo de ejecución del algoritmo

    // Business rules parameters
    private int minimumDeliveryTimeHours; // Minimum time required for deliveries (default 4h)
    
    // Simulation parameters
    private int simulationStepMinutes; // How many minutes to advance in each simulation step
    private int simulationMaxDays; // Maximum days to simulate
    
    // Penalty factors for the planning algorithm
    private double lateDeliveryPenaltyFactor;
    private double fuelConsumptionWeight;
    private double distanceWeight;

    /**
     * Creates default configuration with reasonable values.
     */
    public static AlgorithmConfig createDefault() {
        AlgorithmConfig config = new AlgorithmConfig();
        
        // Default algorithm parameters
        config.setAlgorithmJumpValue(120); // 2 hours interval for replanning
        config.setConsumptionJumpValue(10); // Default Sc
        config.setExecutionTimeSeconds(30); // Default Ta
        
        // Business defaults
        config.setMinimumDeliveryTimeHours(4); // Default 4 hours for delivery
        
        // Simulation defaults
        config.setSimulationStepMinutes(5); // 5-minute steps
        config.setSimulationMaxDays(7); // 7-day simulation
        
        // Penalty defaults
        config.setLateDeliveryPenaltyFactor(2.0);
        config.setFuelConsumptionWeight(1.0);
        config.setDistanceWeight(1.0);
        
        return config;
    }

    // Getters and setters
    public int getAlgorithmJumpValue() {
        return algorithmJumpValue;
    }

    public void setAlgorithmJumpValue(int algorithmJumpValue) {
        this.algorithmJumpValue = algorithmJumpValue;
    }

    public int getConsumptionJumpValue() {
        return consumptionJumpValue;
    }

    public void setConsumptionJumpValue(int consumptionJumpValue) {
        this.consumptionJumpValue = consumptionJumpValue;
    }

    public int getExecutionTimeSeconds() {
        return executionTimeSeconds;
    }

    public void setExecutionTimeSeconds(int executionTimeSeconds) {
        this.executionTimeSeconds = executionTimeSeconds;
    }

    public int getMinimumDeliveryTimeHours() {
        return minimumDeliveryTimeHours;
    }

    public void setMinimumDeliveryTimeHours(int minimumDeliveryTimeHours) {
        this.minimumDeliveryTimeHours = minimumDeliveryTimeHours;
    }

    public int getSimulationStepMinutes() {
        return simulationStepMinutes;
    }

    public void setSimulationStepMinutes(int simulationStepMinutes) {
        this.simulationStepMinutes = simulationStepMinutes;
    }

    public int getSimulationMaxDays() {
        return simulationMaxDays;
    }

    public void setSimulationMaxDays(int simulationMaxDays) {
        this.simulationMaxDays = simulationMaxDays;
    }

    public double getLateDeliveryPenaltyFactor() {
        return lateDeliveryPenaltyFactor;
    }

    public void setLateDeliveryPenaltyFactor(double lateDeliveryPenaltyFactor) {
        this.lateDeliveryPenaltyFactor = lateDeliveryPenaltyFactor;
    }

    public double getFuelConsumptionWeight() {
        return fuelConsumptionWeight;
    }

    public void setFuelConsumptionWeight(double fuelConsumptionWeight) {
        this.fuelConsumptionWeight = fuelConsumptionWeight;
    }

    public double getDistanceWeight() {
        return distanceWeight;
    }

    public void setDistanceWeight(double distanceWeight) {
        this.distanceWeight = distanceWeight;
    }
}
