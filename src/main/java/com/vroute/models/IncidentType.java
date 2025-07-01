package com.vroute.models;

/**
 * Enum representing the different types of incidents that can occur to
 * vehicles.
 * As described in the README, there are 3 types with different durations and
 * effects.
 */
public enum IncidentType {
    // Type 1: 2 hours immobilization, 0 hours repair, vehicle continues route
    TI1(2, "🔧"),
    
    // Type 2: 2 hours immobilization, vehicle must return to depot, repair time depends on shift
    TI2(2, "⚙️"),
    
    // Type 3: 4 hours immobilization, vehicle must return to depot, 3 days (72h) repair
    TI3(4, "💥");

    private final int immobilizationHours;
    private final String emoji;

    IncidentType(int immobilizationHours, String emoji) {
        this.immobilizationHours = immobilizationHours;
        this.emoji = emoji;
    }

    public int getImmobilizationHours() {
        return immobilizationHours;
    }

    public boolean mustReturnToDepot() {
        return this != TI1;
    }

    public String getEmoji() {
        return emoji;
    }

    @Override
    public String toString() {
        return String.format("%s %s [⏱️ %dh]", name(), emoji, immobilizationHours);
    }
}
