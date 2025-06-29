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
    
    public Incident(String vehicleId, IncidentType type, Shift shift) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.shift = shift;
        this.resolved = false;
        this.transferableGlp = 0;
    }
    
    public String getVehicleId() {
        return vehicleId;
    }
    
    public IncidentType getType() {
        return type;
    }
    
    public Shift getShift() {
        return shift;
    }
    
    public LocalDateTime getOccurrenceTime() {
        return occurrenceTime;
    }
    
    public void setOccurrenceTime(LocalDateTime occurrenceTime) {
        this.occurrenceTime = occurrenceTime;
    }
    
    public Position getLocation() {
        return location;
    }
    
    public void setLocation(Position location) {
        this.location = location;
    }
    
    public boolean isResolved() {
        return resolved;
    }
    
    public void setResolved() {
        this.resolved = true;
    }
    
    public double getTransferableGlp() {
        return transferableGlp;
    }
    
    public void setTransferableGlp(double transferableGlp) {
        this.transferableGlp = transferableGlp;
    }
    
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

    @Override
    public Incident clone() {
        return new Incident(vehicleId, type, shift);
    }
}
