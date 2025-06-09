package com.vroute.models;

import java.time.LocalTime;

/**
 * Represents the work shifts used for scheduling and incident management.
 * According to the README, there are 3 shifts per day.
 */
public enum Shift {
    T1(LocalTime.of(0, 0), LocalTime.of(7, 59)),
    T2(LocalTime.of(8, 0), LocalTime.of(15, 59)),
    T3(LocalTime.of(16, 0), LocalTime.of(23, 59));
    
    private final LocalTime startTime;
    private final LocalTime endTime;
    
    Shift(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    /**
     * Gets the start time of the shift.
     * 
     * @return The start time as a LocalTime
     */
    public LocalTime getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the end time of the shift.
     * 
     * @return The end time as a LocalTime
     */
    public LocalTime getEndTime() {
        return endTime;
    }
    
    /**
     * Gets the shift for a specific time.
     * 
     * @param time The time to check
     * @return The shift that contains the specified time
     */
    public static Shift getShiftForTime(LocalTime time) {
        for (Shift shift : values()) {
            if ((time.equals(shift.startTime) || time.isAfter(shift.startTime)) && 
                (time.equals(shift.endTime) || time.isBefore(shift.endTime))) {
                return shift;
            }
        }
        return T1; // Default to T1 if somehow not found
    }
    
    /**
     * Gets the next shift after the current one.
     * 
     * @return The next shift, cycling to T1 after T3
     */
    public Shift getNextShift() {
        switch(this) {
            case T1: return T2;
            case T2: return T3;
            case T3: return T1;
            default: return T1; // Should never happen
        }
    }
}
