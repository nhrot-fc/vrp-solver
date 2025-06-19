package com.vroute.pathfinding;

import com.vroute.models.Environment;
import com.vroute.models.Position;
import com.vroute.models.Blockage;
import com.vroute.models.Constants;

import java.time.LocalDateTime;
import java.util.*;

public class PathFinder {
    public static List<Position> findPath(Environment entorno, Position inicio, Position fin, LocalDateTime horaSalida) {
        if (inicio == null || fin == null || horaSalida == null || entorno == null) {
            return Collections.emptyList();
        }
        if (inicio.equals(fin)) {
            return Collections.singletonList(inicio);
        }
        if (esBloqueado(inicio, horaSalida, entorno)) {
            return Collections.emptyList();
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
                return construirResultado(current);
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

                LocalDateTime tiempoLlegada = calcularTiempoLlegada(current.horaDeLlegada, 1);

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

        return Collections.emptyList();
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

    // Clase interna para los nodos de A*, ahora con tiempo
    private static class Node implements Comparable<Node> {
        final Position posicion;
        final Node parent;
        final double g;
        final double f;
        final LocalDateTime horaDeLlegada;

        Node(Position posicion, Node parent, double g, double h, LocalDateTime horaDeLlegada) {
            this.posicion = posicion;
            this.parent = parent;
            this.g = g;
            this.f = g + h;
            this.horaDeLlegada = horaDeLlegada;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }
}
