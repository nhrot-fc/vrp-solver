package com.vroute.pathfinding;

import com.vroute.models.Position;
import com.vroute.models.Environment;
import com.vroute.models.Constants;
import java.time.LocalDateTime;
import java.util.*;

public class PathFinder {
    private static final double EDGE_WEIGHT_KM = Constants.NODE_DISTANCE;
    private static final double VEHICLE_SPEED_KMPH = Constants.VEHICLE_AVG_SPEED;
    private static final double TIME_PER_EDGE_HOURS = EDGE_WEIGHT_KM / VEHICLE_SPEED_KMPH;
    
    // Private constructor to prevent instantiation
    private PathFinder() {
        // This class should not be instantiated
    }

    public static List<Position> findPath(Environment environment, Position startPos, Position endPos, LocalDateTime startTime) {
        Grid grid = environment.getGrid();
        Node startNode = grid.getNode(startPos.getX(), startPos.getY());
        Node endNode = grid.getNode(endPos.getX(), endPos.getY());

        if (startNode == null || endNode == null) {
            return new ArrayList<>();
        }
        
        // Check if the start or end nodes are blocked
        if (environment.isNodeBlocked(startPos, startTime) || environment.isNodeBlocked(endPos, startTime)) {
            return new ArrayList<>();  // No path possible if start or destination is blocked
        }

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                grid.getNode(x, y).reset();
            }
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();

        startNode.gCost = 0;
        startNode.hCost = calculateHeuristic(startNode, endNode);
        startNode.timeAtNode = startTime; 
        startNode.calculateFCost();
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node currentNode = openSet.poll();

            if (currentNode.equals(endNode)) {
                return reconstructPath(currentNode);
            }

            closedSet.add(currentNode);

            for (Node neighbor : grid.getNeighbors(currentNode)) {
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                long timeToNeighborNanos = (long) (TIME_PER_EDGE_HOURS * 3600 * 1_000_000_000L);
                LocalDateTime timeAtNeighbor = currentNode.timeAtNode.plusNanos(timeToNeighborNanos);
                
                // Check if the neighbor node is blocked
                if (environment.isNodeBlocked(neighbor.position, timeAtNeighbor) || 
                    environment.isPathBlocked(currentNode.position, neighbor.position, timeAtNeighbor)) {
                    continue;
                }

                double tentativeGCost = currentNode.gCost + EDGE_WEIGHT_KM;
                
                if (tentativeGCost < neighbor.gCost) {
                    neighbor.parent = currentNode;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = calculateHeuristic(neighbor, endNode);
                    neighbor.timeAtNode = timeAtNeighbor;
                    neighbor.calculateFCost();

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    private static double calculateHeuristic(Node a, Node b) {
        return (Math.abs(a.position.getX() - b.position.getX()) +
                Math.abs(a.position.getY() - b.position.getY())) * EDGE_WEIGHT_KM;
    }

    private static List<Position> reconstructPath(Node endNode) {
        List<Position> path = new ArrayList<>();
        Node currentNode = endNode;
        while (currentNode != null) {
            path.add(currentNode.position);
            currentNode = currentNode.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
