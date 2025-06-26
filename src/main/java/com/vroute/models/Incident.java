package com.vroute.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a vehicle incident (breakdown/malfunction) that can occur during operations.
 * According to the README, incidents can happen during routes and have different types with
 * different effects on vehicle availability.
 */
public class Incident {
    private final String vehicleId;
    private final IncidentType type;
    private final Shift shift;
    private LocalDateTime occurrenceTime;
    private Position location;
    private boolean resolved;
    private double transferableGlp; // Amount of GLP that can be transferred to another vehicle
    
    /**
     * Creates a new incident for a vehicle in a specific shift.
     * 
     * @param vehicleId The ID of the vehicle affected by the incident
     * @param type The type of incident
     * @param shift The shift when the incident occurs
     */
    public Incident(String vehicleId, IncidentType type, Shift shift) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.shift = shift;
        this.resolved = false;
        this.transferableGlp = 0;
    }
    
    /**
     * Gets the ID of the vehicle affected by the incident.
     * 
     * @return The vehicle ID
     */
    public String getVehicleId() {
        return vehicleId;
    }
    
    /**
     * Gets the type of the incident.
     * 
     * @return The incident type
     */
    public IncidentType getType() {
        return type;
    }
    
    /**
     * Gets the shift when the incident occurs.
     * 
     * @return The shift
     */
    public Shift getShift() {
        return shift;
    }
    
    /**
     * Gets the time when the incident occurred.
     * 
     * @return The occurrence time, or null if the incident hasn't occurred yet
     */
    public LocalDateTime getOccurrenceTime() {
        return occurrenceTime;
    }
    
    /**
     * Sets the time when the incident occurred.
     * 
     * @param occurrenceTime The occurrence time
     */
    public void setOccurrenceTime(LocalDateTime occurrenceTime) {
        this.occurrenceTime = occurrenceTime;
    }
    
    /**
     * Gets the location where the incident occurred.
     * 
     * @return The incident location, or null if the incident hasn't occurred yet
     */
    public Position getLocation() {
        return location;
    }
    
    /**
     * Sets the location where the incident occurred.
     * 
     * @param location The incident location
     */
    public void setLocation(Position location) {
        this.location = location;
    }
    
    /**
     * Checks if the incident has been resolved.
     * 
     * @return true if the incident has been resolved, false otherwise
     */
    public boolean isResolved() {
        return resolved;
    }
    
    /**
     * Sets the incident as resolved.
     */
    public void setResolved() {
        this.resolved = true;
    }
    
    /**
     * Gets the amount of GLP that can be transferred from the affected vehicle.
     * 
     * @return The transferable GLP amount in cubic meters
     */
    public double getTransferableGlp() {
        return transferableGlp;
    }
    
    /**
     * Sets the amount of GLP that can be transferred from the affected vehicle.
     * 
     * @param transferableGlp The transferable GLP amount in cubic meters
     */
    public void setTransferableGlp(double transferableGlp) {
        this.transferableGlp = transferableGlp;
    }
    
    /**
     * Calculates when the vehicle will be available again after the incident.
     * 
     * @return The LocalDateTime when the vehicle will be available again
     */
    public LocalDateTime calculateAvailabilityTime() {
        if (occurrenceTime == null) {
            return null;
        }
        
        // First add the immobilization time (the time the vehicle is stuck at the incident location)
        LocalDateTime availabilityTime = occurrenceTime.plusHours(type.getImmobilizationHours());
        
        // Then add the repair time if needed (time spent at depot)
        if (type.getRepairHours() > 0) {
            availabilityTime = availabilityTime.plusHours(type.getRepairHours());
        }
        
        return availabilityTime;
    }
    
    /**
     * Checks if the vehicle needs to return to depot after this incident.
     * 
     * @return true if the vehicle must return to depot, false if it can continue its route
     */
    public boolean requiresReturnToDepot() {
        return type.mustReturnToDepot();
    }
    
    @Override
    public String toString() {
        String status = resolved ? "✅" : "⚠️";
        String timeInfo;
        if (occurrenceTime != null) {
            timeInfo = occurrenceTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT));
        } else {
            timeInfo = "Not yet occurred";
        }
        
        return String.format("%s %s %s %s [GLP:%.1fm³] %s", 
                status,
                type.getEmoji(),
                vehicleId,
                timeInfo,
                transferableGlp,
                location != null ? location.toString() : "?");
    }
}
