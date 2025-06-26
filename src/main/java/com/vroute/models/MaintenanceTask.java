package com.vroute.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MaintenanceTask {
    private final String vehicleId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int repeatMonths; // Number of months between repetitions (default 2)
    private boolean completed;

    public MaintenanceTask(String vehicleId, LocalDate date) {
        this.vehicleId = vehicleId;
        this.startTime = date.atStartOfDay(); // 00:00
        this.endTime = date.atTime(LocalTime.MAX); // 23:59:59.999999999
        this.repeatMonths = 2; // Default bimonthly repetition
        this.completed = false;
    }
    
    public MaintenanceTask(String vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        this.vehicleId = vehicleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatMonths = 2; // Default bimonthly repetition
        this.completed = false;
    }
    
    public MaintenanceTask(String vehicleId, LocalDateTime startTime, LocalDateTime endTime, int repeatMonths) {
        this.vehicleId = vehicleId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatMonths = repeatMonths;
        this.completed = false;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public LocalDate getDate() {
        return startTime.toLocalDate();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public long getDurationHours() {
        return ChronoUnit.HOURS.between(startTime, endTime);
    }
    
    public int getRepeatMonths() {
        return repeatMonths;
    }
    
    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public MaintenanceTask createNextTask() {
        LocalDateTime nextStart = startTime.plusMonths(repeatMonths);
        LocalDateTime nextEnd = endTime.plusMonths(repeatMonths);
        return new MaintenanceTask(vehicleId, nextStart, nextEnd, repeatMonths);
    }
    
    public static MaintenanceTask fromString(String record) {
        try {
            // Split the record into date and vehicle parts
            String[] parts = record.split(":");
            if (parts.length != 2) {
                return null;
            }
            
            // Parse the date part
            String datePart = parts[0];
            LocalDate date = LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // Parse the vehicle ID
            String vehicleId = parts[1];
            
            return new MaintenanceTask(vehicleId, date);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        String status = completed ? "✅" : "⏳";
        return String.format("%s 🔧 %s %s", 
                status,
                vehicleId,
                startTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)));
    }
    
    public String toRecordString() {
        return String.format("%s:%s", 
                startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                vehicleId);
    }
}
