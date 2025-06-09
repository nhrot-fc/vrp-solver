package com.vroute.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a preventive maintenance task for a vehicle.
 * According to the README, maintenance occurs for a full day (00:00 to 23:59)
 * and a vehicle must return to the depot if maintenance begins during a route.
 */
public class MaintenanceTask {
    private final String vehicleId;
    private final LocalDate date;
    
    /**
     * Creates a new maintenance task for a specific vehicle on a specific date.
     * 
     * @param vehicleId The ID of the vehicle scheduled for maintenance
     * @param date The date when the maintenance will occur
     */
    public MaintenanceTask(String vehicleId, LocalDate date) {
        this.vehicleId = vehicleId;
        this.date = date;
    }
    
    /**
     * Gets the ID of the vehicle scheduled for maintenance.
     * 
     * @return The vehicle ID
     */
    public String getVehicleId() {
        return vehicleId;
    }
    
    /**
     * Gets the date when the maintenance will occur.
     * 
     * @return The maintenance date
     */
    public LocalDate getDate() {
        return date;
    }
    
    /**
     * Gets the start time of the maintenance (00:00 on the maintenance date).
     * 
     * @return The start time as a LocalDateTime
     */
    public LocalDateTime getStartTime() {
        return LocalDateTime.of(date, LocalTime.MIDNIGHT);
    }
    
    /**
     * Gets the end time of the maintenance (23:59:59 on the maintenance date).
     * 
     * @return The end time as a LocalDateTime
     */
    public LocalDateTime getEndTime() {
        return LocalDateTime.of(date, LocalTime.of(23, 59, 59));
    }
    
    /**
     * Checks if the maintenance task is active at a specific date and time.
     * 
     * @param dateTime The date and time to check
     * @return true if the maintenance is active at the specified time, false otherwise
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        LocalDate checkDate = dateTime.toLocalDate();
        return checkDate.equals(date);
    }
    
    @Override
    public String toString() {
        return "Maintenance [" + vehicleId + " on " + date + "]";
    }
}
