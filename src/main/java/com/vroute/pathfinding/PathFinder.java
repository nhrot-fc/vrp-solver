package com.vroute.pathfinding;

import com.vroute.models.Environment;
import com.vroute.models.Position;
import com.vroute.models.Blockage;
import com.vroute.models.Constants;

import java.time.LocalDateTime;
import java.util.*;

public class PathFinder {
    /**
     * Finds a path between two positions, taking into account blockages at the given time.
     * 
     * @param entorno The environment containing blockage information
     * @param inicio Starting position
     * @param fin Destination position
     * @param horaSalida Departure time
     * @return A PathResult containing the path, arrival time, and total distance
     */
    public static PathResult findPath(Environment entorno, Position inicio, Position fin, LocalDateTime horaSalida) {
        if (inicio == null || fin == null || horaSalida == null || entorno == null) {
            return new PathResult(Collections.emptyList(), horaSalida, 0);
        }
        if (inicio.equals(fin)) {
            return new PathResult(Collections.singletonList(inicio), horaSalida, 0);
        }
        if (esBloqueado(inicio, horaSalida, entorno)) {
            return new PathResult(Collections.emptyList(), horaSalida, 0);
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Position, Node> posicionANodo = new HashMap<>();
        Set<Position> closedSet = new HashSet<>();

        Node startNode = new Node(inicio, null, 0, heuristica(inicio, fin), horaSalida);
        openSet.add(startNode);
        posicionANodo.put(inicio, startNode);

        int[][] direcciones = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.posicion.equals(fin)) {
                List<Position> path = construirResultado(current);
                double totalDistance = calcularDistanciaTotal(path);
                return new PathResult(path, current.estimatedArrivalTime, totalDistance);
            }

            closedSet.add(current.posicion);

            for (int[] dir : direcciones) {
                int newX = current.posicion.getX() + dir[0];
                int newY = current.posicion.getY() + dir[1];

                if (newX < 0 || newX >= Constants.CITY_LENGTH_X ||
                        newY < 0 || newY >= Constants.CITY_WIDTH_Y) {
                    continue;
                }

                Position vecino = new Position(newX, newY);

                if (closedSet.contains(vecino)) {
                    continue;
                }

                LocalDateTime tiempoLlegada = calcularTiempoLlegada(current.estimatedArrivalTime, 1);

                if (esBloqueado(vecino, tiempoLlegada, entorno)) {
                    continue;
                }

                double newG = current.g + 1;

                Node vecinoNode = posicionANodo.get(vecino);
                if (vecinoNode == null || newG < vecinoNode.g) {
                    double h = heuristica(vecino, fin);
                    Node newNode = new Node(vecino, current, newG, h, tiempoLlegada);

                    if (vecinoNode != null) {
                        openSet.remove(vecinoNode);
                    }

                    openSet.add(newNode);
                    posicionANodo.put(vecino, newNode);
                }
            }
        }

        return new PathResult(Collections.emptyList(), horaSalida, 0);
    }

    private static boolean esBloqueado(Position posicion, LocalDateTime momento, Environment entorno) {
        List<Blockage> bloqueosActivos = entorno.getActiveBlockagesAt(momento);
        for (Blockage bloqueo : bloqueosActivos) {
            if (bloqueo.posicionEstaBloqueada(posicion, momento)) {
                return true;
            }
        }
        return false;
    }

    private static LocalDateTime calcularTiempoLlegada(LocalDateTime horaSalida, double distanciaKm) {
        long segundosViaje = (long) (distanciaKm / Constants.VEHICLE_AVG_SPEED * 3600);
        return horaSalida.plusSeconds(segundosViaje);
    }

    private static double heuristica(Position a, Position b) {
        return a.distanceTo(b);
    }

    private static List<Position> construirResultado(Node destinoNode) {
        List<Position> camino = new LinkedList<>();
        Node current = destinoNode;
        while (current != null) {
            camino.add(0, current.posicion);
            current = current.parent;
        }
        return camino;
    }
    
    /**
     * Calculates the total distance of a path.
     * 
     * @param path List of positions representing the path
     * @return The total distance in kilometers
     */
    private static double calcularDistanciaTotal(List<Position> path) {
        double distancia = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            distancia += path.get(i).distanceTo(path.get(i + 1));
        }
        return distancia;
    }
}
