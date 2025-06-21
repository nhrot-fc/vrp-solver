package com.vroute.solution;

import java.time.LocalDateTime;

import com.vroute.models.Position;

public interface RouteStop {
    Position getPosition();
    String getEntityID();
    LocalDateTime getArrivalTime();
}
