package com.vroute.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Blockage {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<Position> lines;
    private final Set<Position> blockagePoints;

    public Blockage(LocalDateTime startTime, LocalDateTime endTime, List<Position> blockagePoints) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.lines = new ArrayList<>(blockagePoints);
        this.blockagePoints = precomputarPuntos(blockagePoints);
    }

    private static Set<Position> precomputarPuntos(List<Position> tramos) {
        if (tramos == null || tramos.size() < 2) {
            return Collections.emptySet();
        }

        Set<Position> puntos = new HashSet<>();
        for (int i = 0; i < tramos.size() - 1; i++) {
            Position p1 = tramos.get(i);
            Position p2 = tramos.get(i + 1);

            // Rellenar los puntos en el segmento
            if (p1.getY() == p2.getY()) { // Segmento horizontal
                for (int x = Math.min(p1.getX(), p2.getX()); x <= Math.max(p1.getX(), p2.getX()); x++) {
                    puntos.add(new Position(x, p1.getY()));
                }
            } else if (p1.getX() == p2.getX()) { // Segmento vertical
                for (int y = Math.min(p1.getY(), p2.getY()); y <= Math.max(p1.getY(), p2.getY()); y++) {
                    puntos.add(new Position(p1.getX(), y));
                }
            }
        }
        return Collections.unmodifiableSet(puntos);
    }

    public List<Position> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Set<Position> getBlockagePoints() {
        return blockagePoints;
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }

    public boolean posicionEstaBloqueada(Position posicion, LocalDateTime momento) {
        return isActiveAt(momento) && blockagePoints.contains(posicion);
    }

    public boolean isCompleted(LocalDateTime dateTime) {
        return dateTime.isAfter(endTime);
    }

    @Override
    public String toString() {
        return String.format("🚧 [%s - %s] %d points", 
            startTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
            endTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)), 
            blockagePoints.size());
    }

    @Override
    public Blockage clone() {
        return new Blockage(startTime, endTime, lines);
    }
}
