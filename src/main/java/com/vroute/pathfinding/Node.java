package com.vroute.pathfinding;

import com.vroute.models.Position;
import java.time.LocalDateTime; // Added import for LocalDateTime

public class Node implements Comparable<Node> {
    public Position position;
    public Node parent;
    public double gCost; // Cost from start to this node (distance in km)
    public double hCost; // Heuristic cost from this node to end (distance in km)
    public double fCost; // gCost + hCost
    public LocalDateTime timeAtNode; // Time when this node is reached

    public Node(Position position) {
        this.position = position;
        this.parent = null;
        this.gCost = Double.MAX_VALUE;
        this.hCost = 0;
        this.fCost = Double.MAX_VALUE;
        this.timeAtNode = null; // Initialize timeAtNode
    }

    public void reset() {
        this.parent = null;
        this.gCost = Double.MAX_VALUE;
        this.hCost = 0;
        this.fCost = Double.MAX_VALUE;
        this.timeAtNode = null;
    }

    public void calculateFCost() {
        this.fCost = this.gCost + this.hCost;
    }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.fCost, other.fCost);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return position.getX() == node.position.getX() && position.getY() == node.position.getY();
    }

    @Override
    public int hashCode() {
        return 31 * position.getX() + position.getY();
    }
}
