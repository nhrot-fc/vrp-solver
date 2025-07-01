package com.vroute.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Incident {
    private final String vehicleId;
    private final IncidentType type;
    private final Shift shift;
    private final LocalDateTime occurrenceTime;

    public Incident(String vehicleId, IncidentType type, Shift shift, LocalDateTime occurrenceTime) {
        this.vehicleId = vehicleId;
        this.type = type;
        this.shift = shift;
        this.occurrenceTime = occurrenceTime;
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

    /**
     * Checks if the vehicle needs to return to depot due to this incident.
     * According to the README, only TI1 incidents allow vehicles to continue their routes.
     * TI2 and TI3 incidents require vehicles to return to the depot.
     * 
     * @return true if vehicle must return to depot, false otherwise
     */
    public boolean mustReturnToDepot() {
        return type.mustReturnToDepot();
    }

    /**
     * Calculates the availability time based on the incident type and occurrence time.
     * For TI1: Add immobilization hours (2h)
     * For TI2: Add immobilization hours (2h) and availability depends on shift:
     *         - If in T1: Available in T3 (same day)
     *         - If in T2: Available in T1 (next day)
     *         - If in T3: Available in T2 (next day)
     * For TI3: Add immobilization hours (4h) and 3 full days (72h)
     * 
     * @return The LocalDateTime when the vehicle will be available again
     */
    public LocalDateTime calculateAvailabilityTime() {
        if (occurrenceTime == null) {
            return null;
        }

        // First add immobilization hours (vehicle is stuck at the incident location)
        LocalDateTime availabilityTime = occurrenceTime.plusHours(type.getImmobilizationHours());

        // For TI1, just return after immobilization time (no repair time)
        if (type == IncidentType.TI1) {
            return availabilityTime;
        }
        
        // For TI2, availability depends on the shift
        else if (type == IncidentType.TI2) {
            switch (shift) {
                case T1:
                    // If occurs in T1 (day A) -> Available in T3 (day A)
                    return adjustTimeToShiftStart(occurrenceTime, Shift.T3);
                case T2:
                    // If occurs in T2 (day A) -> Available in T1 (day A+1)
                    return adjustTimeToShiftStart(occurrenceTime.plusDays(1), Shift.T1);
                case T3:
                    // If occurs in T3 (day A) -> Available in T2 (day A+1)
                    return adjustTimeToShiftStart(occurrenceTime.plusDays(1), Shift.T2);
                default:
                    return availabilityTime;
            }
        }
        
        // For TI3, add 3 days from occurrence
        else if (type == IncidentType.TI3) {
            // If occurs in T1, T2 or T3 (day A) -> Available in T1 (day A+3)
            return adjustTimeToShiftStart(occurrenceTime.plusDays(3), Shift.T1);
        }

        // Default behavior (shouldn't reach here)
        return availabilityTime;
    }

    /**
     * Helper method to adjust a time to the start of a specified shift.
     * 
     * @param dateTime The base date to adjust
     * @param targetShift The shift to adjust to
     * @return The adjusted LocalDateTime at the start of the target shift
     */
    private LocalDateTime adjustTimeToShiftStart(LocalDateTime dateTime, Shift targetShift) {
        return LocalDateTime.of(
            dateTime.toLocalDate(),
            targetShift.getStartTime()
        );
    }

    /**
     * Checks if the incident is resolved at the given time.
     * 
     * @param currentTime The time to check against
     * @return true if the incident is resolved, false otherwise
     */
    public boolean isResolved(LocalDateTime currentTime) {
        if (occurrenceTime == null) {
            return false;
        }
        LocalDateTime availabilityTime = calculateAvailabilityTime();
        return currentTime.isEqual(availabilityTime) || currentTime.isAfter(availabilityTime);
    }

    @Override
    public String toString() {
        String timeInfo;
        if (occurrenceTime != null) {
            timeInfo = occurrenceTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT));
        } else {
            timeInfo = "Not yet occurred";
        }

        return String.format("%s %s [%s - %s]", type.getEmoji(), vehicleId, timeInfo, shift);
    }

    @Override
    public Incident clone() {
        return new Incident(vehicleId, type, shift, occurrenceTime);
    }
}
