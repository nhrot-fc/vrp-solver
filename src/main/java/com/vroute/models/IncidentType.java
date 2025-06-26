package com.vroute.models;

/**
 * Enum representing the different types of incidents that can occur to vehicles.
 * As described in the README, there are 3 types with different durations and effects.
 */
public enum IncidentType {
    /**
     * Type 1: Repairable on site (e.g., flat tire)
     * - 2 hours immobilization
     * - Vehicle can continue its route afterward
     */
    TI1(2, 0, "🔧"),
    
    /**
     * Type 2: Requires repair (e.g., engine obstruction)
     * - 2 hours immobilization
     * - 1 shift of inactivity for repairs
     * - Vehicle must return to depot
     */
    TI2(2, 1, "⚙️"),
    
    /**
     * Type 3: Serious incident (e.g., collision)
     * - 4 hours immobilization
     * - 3 days of inactivity for repairs
     * - Vehicle must return to depot
     */
    TI3(4, 72, "💥"); // 72 hours = 3 days
    
    private final int immobilizationHours;
    private final int repairHours;
    private final String emoji;
    
    IncidentType(int immobilizationHours, int repairHours, String emoji) {
        this.immobilizationHours = immobilizationHours;
        this.repairHours = repairHours;
        this.emoji = emoji;
    }
    
    /**
     * Gets the immobilization time in hours.
     * 
     * @return The number of hours the vehicle is immobilized at the incident location
     */
    public int getImmobilizationHours() {
        return immobilizationHours;
    }
    
    /**
     * Gets the repair time in hours.
     * 
     * @return The number of hours the vehicle needs to spend in the depot for repairs
     */
    public int getRepairHours() {
        return repairHours;
    }
    
    /**
     * Determines if the vehicle must return to depot after the incident.
     * 
     * @return true if the vehicle must return to depot, false if it can continue its route
     */
    public boolean mustReturnToDepot() {
        return this != TI1;
    }
    
    /**
     * Gets the emoji representing this incident type.
     * 
     * @return The emoji as a String
     */
    public String getEmoji() {
        return emoji;
    }
    
    @Override
    public String toString() {
        return String.format("%s %s [⏱️ %dh+%dh]", 
                name(), 
                emoji, 
                immobilizationHours, 
                repairHours);
    }
}
