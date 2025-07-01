package com.vroute.orchest;

import java.time.LocalDateTime;

/**
 * Represents an event in the system that may trigger replanning.
 * Events are ordered by their scheduled time.
 */
public class Event implements Comparable<Event> {
    private final EventType type;
    private final LocalDateTime time;
    private final Object data;

    public Event(EventType type, LocalDateTime time, Object data) {
        this.type = type;
        this.time = time;
        this.data = data;
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Object getData() {
        return data;
    }

    @Override
    public int compareTo(Event other) {
        return this.time.compareTo(other.time);
    }

    @Override
    public String toString() {
        return String.format("Event[%s, time=%s]",
                type, time.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    }
}
