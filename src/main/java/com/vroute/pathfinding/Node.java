package com.vroute.pathfinding;

import java.time.LocalDateTime;

import com.vroute.models.Position;

public class Node implements Comparable<Node> {
    // Attributes
    final Position posicion;
    final Node parent;
    final double g;
    final double f;
    final LocalDateTime estimatedArrivalTime;

    Node(Position posicion, Node parent, double g, double h, LocalDateTime estimatedArrivalTime) {
        this.posicion = posicion;
        this.parent = parent;
        this.g = g;
        this.f = g + h;
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    @Override
    public int compareTo(Node other) {
        return Double.compare(this.f, other.f);
    }
}
