package com.vroute.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Blockage {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<Position> blockagePoints;

    public Blockage(LocalDateTime startTime, LocalDateTime endTime, List<Position> blockagePoints) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.blockagePoints = new ArrayList<>(blockagePoints);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<Position> getBlockagePoints() {
        return blockagePoints;
    }

    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }

    public boolean isPathBlocked(Position from, Position to, LocalDateTime dateTime) {
        if (!isActiveAt(dateTime)) {
            return false;
        }

        for (int i = 0; i < blockagePoints.size() - 1; i++) {
            Position p1 = blockagePoints.get(i);
            Position p2 = blockagePoints.get(i + 1);

            if ((from.getX() == p1.getX() && from.getY() == p1.getY() && to.getX() == p2.getX()
                    && to.getY() == p2.getY()) ||
                    (from.getX() == p2.getX() && from.getY() == p2.getY() && to.getX() == p1.getX()
                            && to.getY() == p1.getY())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Blockage [");
        sb.append(startTime.toString());
        sb.append(" - ");
        sb.append(endTime.toString());
        sb.append("] Points: ");

        for (int i = 0; i < blockagePoints.size(); i++) {
            Position p = blockagePoints.get(i);
            sb.append("(").append(p.getX()).append(",").append(p.getY()).append(")");

            if (i < blockagePoints.size() - 1) {
                sb.append(" - ");
            }
        }

        return sb.toString();
    }
}
