package com.vroute.alns;

import java.util.Random;

/**
 * Implementation of Simulated Annealing for ALNS.
 * This class handles the acceptance criteria for new solutions.
 */
public class SimulatedAnnealing {
    private double temperature;
    private final double coolingRate;
    private final double finalTemperature;
    private final Random random;
    
    /**
     * Creates a new Simulated Annealing instance.
     * 
     * @param initialTemperature Starting temperature
     * @param coolingRate Rate at which temperature decreases (0-1)
     * @param finalTemperature Minimum temperature
     */
    public SimulatedAnnealing(double initialTemperature, double coolingRate, double finalTemperature) {
        this.temperature = initialTemperature;
        this.coolingRate = coolingRate;
        this.finalTemperature = finalTemperature;
        this.random = new Random();
    }
    
    /**
     * Decides whether to accept a new solution based on its cost and the current temperature.
     * 
     * @param currentCost Cost of the current solution
     * @param newCost Cost of the new solution
     * @return True if the new solution should be accepted
     */
    public boolean accept(double currentCost, double newCost) {
        // If the new solution is better, always accept it
        if (newCost <= currentCost) {
            return true;
        }
        
        // Otherwise, accept with a probability based on the temperature
        // and the cost difference
        double costDifference = newCost - currentCost;
        double acceptanceProbability = Math.exp(-costDifference / temperature);
        
        return random.nextDouble() < acceptanceProbability;
    }
    
    /**
     * Reduces the temperature according to the cooling rate.
     */
    public void cool() {
        temperature = Math.max(finalTemperature, temperature * coolingRate);
    }
    
    /**
     * Checks if the system has "frozen" (reached the final temperature).
     * 
     * @return True if the final temperature has been reached
     */
    public boolean isFrozen() {
        return temperature <= finalTemperature;
    }
    
    /**
     * Gets the current temperature.
     * 
     * @return The current temperature
     */
    public double getTemperature() {
        return temperature;
    }
    
    /**
     * Resets the temperature to the initial value.
     * 
     * @param initialTemperature The initial temperature to reset to
     */
    public void reset(double initialTemperature) {
        this.temperature = initialTemperature;
    }
}
