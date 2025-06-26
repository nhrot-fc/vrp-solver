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
    private final List<LocalDateTime> arrivalTimes;
    private final int distance;
    /**
     * Creates a new PathResult with the given path and metadata.
     * 
     * @param path List of positions representing the path
     * @param arrivalTime Estimated arrival time at the destination
     * @param totalDistance Total distance of the path in kilometers
     */
    public PathResult(List<Position> path, List<LocalDateTime> arrivalTimes, int totalDistance) {
        this.path = path;
        this.arrivalTimes = arrivalTimes;
        this.distance = totalDistance;
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
    public List<LocalDateTime> getArrivalTimes() {
        return arrivalTimes;
    }

    /**
     * Returns the total distance of the path in kilometers.
     * 
     * @return The distance in kilometers
     */
    public int getDistance() {
        return distance;
    }
} 