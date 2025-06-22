package com.vroute.pathfinding;

import com.vroute.models.Position;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Encapsulates the result of a path finding operation, including the path itself
 * and additional metadata like arrival times and total distance.
 */
public class PathResult {
    private final List<Position> path;
    private final LocalDateTime arrivalTime;
    private final double totalDistance;
    private final boolean pathFound;

    /**
     * Creates a new PathResult with the given path and metadata.
     * 
     * @param path List of positions representing the path
     * @param arrivalTime Estimated arrival time at the destination
     * @param totalDistance Total distance of the path in kilometers
     */
    public PathResult(List<Position> path, LocalDateTime arrivalTime, double totalDistance) {
        this.path = path;
        this.arrivalTime = arrivalTime;
        this.totalDistance = totalDistance;
        this.pathFound = !path.isEmpty();
    }

    /**
     * Returns the list of positions representing the path.
     * 
     * @return The path as a list of positions
     */
    public List<Position> getPath() {
        return path;
    }

    /**
     * Returns the estimated arrival time at the destination.
     * 
     * @return The arrival time
     */
    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Returns the total distance of the path in kilometers.
     * 
     * @return The distance in kilometers
     */
    public double getTotalDistance() {
        return totalDistance;
    }

    /**
     * Checks if a valid path was found.
     * 
     * @return true if a path was found, false otherwise
     */
    public boolean isPathFound() {
        return pathFound;
    }
} 