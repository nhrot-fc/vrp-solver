package com.vroute.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Maintenance {
    private final String vehicleId;
    private final LocalDate assignedDate;
    private LocalDateTime realStarttime;
    private LocalDateTime endTime;

    public Maintenance(String vehicleId, LocalDate assignedDate) {
        this.vehicleId = vehicleId;
        this.assignedDate = assignedDate;
        this.realStarttime = null;
        this.endTime = null;
    }

    public void setRealStarttime(LocalDateTime realStarttime) {
        this.realStarttime = realStarttime;
        this.endTime = realStarttime.plusHours(24);
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public LocalDateTime getRealStarttime() {
        return realStarttime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getDurationHours() {
        return ChronoUnit.HOURS.between(realStarttime, endTime);
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(realStarttime) && !dateTime.isAfter(endTime);
    }

    public boolean isCompleted(LocalDateTime referenceTime) {
        return referenceTime.isAfter(endTime);
    }

    public Maintenance createNextTask() {
        int repeatMonths = 2;
        LocalDate nextAssignedDate = assignedDate.plusMonths(repeatMonths);
        return new Maintenance(vehicleId, nextAssignedDate);
    }

    @Override
    public String toString() {
        return String.format("🔧 %s [%s]", vehicleId,
                DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT).format(realStarttime));
    }
}
