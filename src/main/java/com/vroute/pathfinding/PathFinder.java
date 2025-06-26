package com.vroute.pathfinding;

import com.vroute.models.Environment;
import com.vroute.models.Position;
import com.vroute.models.Blockage;
import com.vroute.models.Constants;

import java.time.LocalDateTime;
import java.util.*;

public class PathFinder {
    public static PathResult findPath(Environment env, Position inicio, Position fin, LocalDateTime horaSalida) {
        if (inicio == null || fin == null || horaSalida == null || env == null) {
            return null;
        }
        if (inicio.equals(fin)) {
            return new PathResult(Collections.singletonList(inicio), Collections.singletonList(horaSalida), 0);
        }
        if (esBloqueado(inicio, horaSalida, env)) {
            return null;
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
                List<Node> path = construirResultado(current);
                int totalDistance = calcularDistancia(path);

                List<Position> positions = new ArrayList<>();
                List<LocalDateTime> arrivalTimes = new ArrayList<>();
                for (Node node : path) {
                    positions.add(node.posicion);
                    arrivalTimes.add(node.estimatedArrivalTime);
                }
                return new PathResult(positions, arrivalTimes, totalDistance);
            }

            closedSet.add(current.posicion);

            for (int[] dir : direcciones) {
                int newX = current.posicion.getX() + dir[0];
                int newY = current.posicion.getY() + dir[1];

                if (newX < 0 || newX > Constants.CITY_LENGTH_X ||
                        newY < 0 || newY > Constants.CITY_WIDTH_Y) {
                    continue;
                }

                Position vecino = new Position(newX, newY);

                if (closedSet.contains(vecino)) {
                    continue;
                }

                LocalDateTime tiempoLlegada = calcularTiempoLlegada(current.estimatedArrivalTime, 1);

                if (esBloqueado(vecino, tiempoLlegada, env)) {
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

        return null;
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

    private static List<Node> construirResultado(Node destinoNode) {
        List<Node> camino = new LinkedList<>();
        Node current = destinoNode;
        while (current != null) {
            camino.add(0, current);
            current = current.parent;
        }
        return camino;
    }

    private static int calcularDistancia(List<Node> path) {
        int distancia = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            distancia += path.get(i).posicion.distanceTo(path.get(i + 1).posicion);
        }
        return distancia;
    }
}
