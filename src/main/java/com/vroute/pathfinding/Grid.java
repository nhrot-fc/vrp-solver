package com.vroute.pathfinding;

import com.vroute.models.Position;
import java.util.ArrayList;
import java.util.List;

public class Grid {
    private final int width;
    private final int height;
    private final Node[][] nodes;

    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
        this.nodes = new Node[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                nodes[x][y] = new Node(new Position(x, y));
            }
        }
    }

    public Node getNode(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return nodes[x][y];
        }
        return null;
    }

    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        int x = node.position.getX();
        int y = node.position.getY();

        // Up
        if (y > 0) neighbors.add(nodes[x][y - 1]);
        // Down
        if (y < height - 1) neighbors.add(nodes[x][y + 1]);
        // Left
        if (x > 0) neighbors.add(nodes[x - 1][y]);
        // Right
        if (x < width - 1) neighbors.add(nodes[x + 1][y]);

        return neighbors;
    }

    public boolean isValidPosition(Position pos) {
        return pos.getX() >= 0 && pos.getX() < width && pos.getY() >= 0 && pos.getY() < height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
