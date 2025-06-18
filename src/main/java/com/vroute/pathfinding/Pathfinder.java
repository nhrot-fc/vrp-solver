package com.vroute.pathfinding;

import com.vroute.models.Entorno;
import com.vroute.models.Posicion;
import com.vroute.models.Bloqueo;
import com.vroute.utils.Constants;

import java.time.LocalDateTime;
import java.util.*;

public class Pathfinder {
    public PathResult getPath(Posicion inicio, Posicion fin, LocalDateTime horaSalida, Entorno entorno) {
        // Validaciones básicas
        if (inicio == null || fin == null || horaSalida == null || entorno == null) {
            return new PathResult(0, 0, Collections.emptyList());
        }

        // Si origen y destino son iguales, devolver camino con un solo punto
        if (inicio.equals(fin)) {
            return new PathResult(0, 0, Collections.singletonList(inicio));
        }

        if (esBloqueado(inicio, horaSalida, entorno)) {
            return new PathResult(0, 0, Collections.emptyList());
        }

        // Inicializar estructuras para el algoritmo A*
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Posicion, Node> posicionANodo = new HashMap<>();
        Set<Posicion> closedSet = new HashSet<>();

        // Añadir nodo inicial a la lista abierta
        Node startNode = new Node(inicio, null, 0, heuristica(inicio, fin), horaSalida);
        openSet.add(startNode);
        posicionANodo.put(inicio, startNode);

        // Direcciones posibles: arriba, derecha, abajo, izquierda
        int[][] direcciones = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // Verificar si hemos llegado al destino
            if (current.posicion.equals(fin)) {
                return construirResultado(current);
            }

            closedSet.add(current.posicion);

            // Explorar vecinos
            for (int[] dir : direcciones) {
                int newX = current.posicion.getX() + dir[0];
                int newY = current.posicion.getY() + dir[1];

                // Validar límites de la ciudad
                if (newX < 0 || newX >= Constants.CITY_WIDTH_KM ||
                        newY < 0 || newY >= Constants.CITY_HEIGHT_KM) {
                    continue;
                }

                Posicion vecino = new Posicion(newX, newY);

                // Comprobar si ya fue explorado
                if (closedSet.contains(vecino)) {
                    continue;
                }

                // Calcular tiempo de llegada a este nuevo nodo
                LocalDateTime tiempoLlegada = calcularTiempoLlegada(current.horaDeLlegada, 1);

                // Verificar si hay bloqueo en esta posición y momento
                if (esBloqueado(vecino, tiempoLlegada, entorno)) {
                    continue;
                }

                // Calcular costo g (distancia recorrida)
                double newG = current.g + 1; // 1 unidad de distancia entre celdas adyacentes

                // Si vecino no está en openSet o tiene un costo mejor que antes
                Node vecinoNode = posicionANodo.get(vecino);
                if (vecinoNode == null || newG < vecinoNode.g) {
                    // Crear o actualizar nodo vecino
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

        // Si llegamos aquí, no se encontró camino
        return new PathResult(0, 0, Collections.emptyList());
    }

    private boolean esBloqueado(Posicion posicion, LocalDateTime momento, Entorno entorno) {
        // Verificar bloqueos activos en este momento
        List<Bloqueo> bloqueosActivos = entorno.getBloqueosActivosEnMomento(momento);
        for (Bloqueo bloqueo : bloqueosActivos) {
            if (bloqueo.posicionEstaBloqueada(posicion, momento)) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime calcularTiempoLlegada(LocalDateTime horaSalida, double distanciaKm) {
        // Calcular tiempo de recorrido basado en la velocidad media
        long segundosViaje = (long) (distanciaKm / Constants.AVERAGE_SPEED_KMPH * 3600);
        return horaSalida.plusSeconds(segundosViaje);
    }

    private double heuristica(Posicion a, Posicion b) {
        return a.distancia(b);
    }

    private PathResult construirResultado(Node destinoNode) {

        List<Posicion> camino = new LinkedList<>();

        Node current = destinoNode;

        while (current != null) {
            camino.add(0, current.posicion); // Añadir al principio para evitar el reverse
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
        final double g; // costo (distancia) desde el inicio
        final double f; // g + h
        final LocalDateTime horaDeLlegada; // Momento de llegada a este nodo

        Node(Posicion posicion, Node parent, double g, double h, LocalDateTime horaDeLlegada) {
            this.posicion = posicion;
            this.parent = parent;
            this.g = g;
            this.f = g + h; // La heurística A* sigue basándose en la distancia, no en el tiempo
            this.horaDeLlegada = horaDeLlegada;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }
}