package com.vroute.orchest;

import java.time.LocalDateTime;

/**
 * Represents an event in the system that may trigger replanning.
 * Events are ordered by their scheduled time.
 */
public class Event implements Comparable<Event> {
    private final EventType type;
    private final LocalDateTime time;
    private final String entityId;  // ID of related entity (vehicle, order, blockage, etc.)
    private final Object data;      // Additional event data

    public Event(EventType type, LocalDateTime time, String entityId, Object data) {
        this.type = type;
        this.time = time;
        this.entityId = entityId;
        this.data = data;
    }

    public Event(EventType type, LocalDateTime time, String entityId) {
        this(type, time, entityId, null);
    }

    public Event(EventType type, LocalDateTime time) {
        this(type, time, null, null);
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getEntityId() {
        return entityId;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    @Override
    public int compareTo(Event other) {
        return this.time.compareTo(other.time);
    }

    @Override
    public String toString() {
        return String.format("Event[%s, time=%s, entity=%s]", 
            type, time.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")), 
            entityId != null ? entityId : "N/A");
    }
}
