package com.vroute.pathfinding;

import com.vroute.models.Entorno;
import com.vroute.models.Posicion;
import com.vroute.models.Bloqueo;
import com.vroute.utils.Constants;

import java.time.LocalDateTime;
import java.util.*;

public class Pathfinder {
    public static PathResult getPath(Posicion inicio, Posicion fin, LocalDateTime horaSalida, Entorno entorno) {
        if (inicio == null || fin == null || horaSalida == null || entorno == null) {
            return new PathResult(0, 0, Collections.emptyList());
        }
        if (inicio.equals(fin)) {
            return new PathResult(0, 0, Collections.singletonList(inicio));
        }
        if (esBloqueado(inicio, horaSalida, entorno)) {
            return new PathResult(0, 0, Collections.emptyList());
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Posicion, Node> posicionANodo = new HashMap<>();
        Set<Posicion> closedSet = new HashSet<>();

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

                if (newX < 0 || newX >= Constants.CITY_WIDTH_KM ||
                        newY < 0 || newY >= Constants.CITY_HEIGHT_KM) {
                    continue;
                }

                Posicion vecino = new Posicion(newX, newY);

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

        return new PathResult(0, 0, Collections.emptyList());
    }

    private static boolean esBloqueado(Posicion posicion, LocalDateTime momento, Entorno entorno) {
        List<Bloqueo> bloqueosActivos = entorno.getBloqueosActivosEnMomento(momento);
        for (Bloqueo bloqueo : bloqueosActivos) {
            if (bloqueo.posicionEstaBloqueada(posicion, momento)) {
                return true;
            }
        }
        return false;
    }

    private static LocalDateTime calcularTiempoLlegada(LocalDateTime horaSalida, double distanciaKm) {
        long segundosViaje = (long) (distanciaKm / Constants.AVERAGE_SPEED_KMPH * 3600);
        return horaSalida.plusSeconds(segundosViaje);
    }

    private static double heuristica(Posicion a, Posicion b) {
        return a.distancia(b);
    }

    private static PathResult construirResultado(Node destinoNode) {

        List<Posicion> camino = new LinkedList<>();

        Node current = destinoNode;

        while (current != null) {
            camino.add(0, current.posicion);
            current = current.parent;
        }

        double distancia = Math.max(0, camino.size() - 1.0);

        double duracionHoras = distancia / Constants.AVERAGE_SPEED_KMPH;

        return new PathResult(distancia, duracionHoras, camino);

    }

    // Clase interna para los nodos de A*, ahora con tiempo
    private static class Node implements Comparable<Node> {
        final Posicion posicion;
        final Node parent;
        final double g;
        final double f;
        final LocalDateTime horaDeLlegada;

        Node(Posicion posicion, Node parent, double g, double h, LocalDateTime horaDeLlegada) {
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