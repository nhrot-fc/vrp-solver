package com.vroute.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Bloqueo {
    private final LocalDateTime horaInicio;
    private final LocalDateTime horaFin;
    private final List<Posicion> tramos;
    private final Set<Posicion> puntosBloqueados;

    public Bloqueo(LocalDateTime horaInicio, LocalDateTime horaFin, List<Posicion> tramos) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.tramos = tramos != null ? Collections.unmodifiableList(new ArrayList<>(tramos)) : Collections.emptyList();
        this.puntosBloqueados = precomputarPuntos(tramos);
    }

    private static Set<Posicion> precomputarPuntos(List<Posicion> tramos) {
        if (tramos == null || tramos.size() < 2) {
            return Collections.emptySet();
        }

        Set<Posicion> puntos = new HashSet<>();
        for (int i = 0; i < tramos.size() - 1; i++) {
            Posicion p1 = tramos.get(i);
            Posicion p2 = tramos.get(i + 1);

            // Rellenar los puntos en el segmento
            if (p1.getY() == p2.getY()) { // Segmento horizontal
                for (int x = Math.min(p1.getX(), p2.getX()); x <= Math.max(p1.getX(), p2.getX()); x++) {
                    puntos.add(new Posicion(x, p1.getY()));
                }
            } else if (p1.getX() == p2.getX()) { // Segmento vertical
                for (int y = Math.min(p1.getY(), p2.getY()); y <= Math.max(p1.getY(), p2.getY()); y++) {
                    puntos.add(new Posicion(p1.getX(), y));
                }
            }
        }
        return Collections.unmodifiableSet(puntos); // Devolver Set inmutable
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public LocalDateTime getHoraFin() {
        return horaFin;
    }

    public List<Posicion> getTramos() {
        return tramos;
    }

    public Set<Posicion> getPuntosBloqueados() {
        return puntosBloqueados;
    }

    /**
     * Determina si un bloqueo está activo en un momento dado.
     * Un bloqueo está activo si el momento especificado está entre horaInicio (inclusive) y horaFin (exclusive).
     * 
     * @param momento El momento para verificar
     * @return true si el bloqueo está activo en ese momento, false en caso contrario
     */
    public boolean estaActivo(LocalDateTime momento) {
        return !momento.isBefore(horaInicio) && momento.isBefore(horaFin);
    }

    /**
     * Verifica si una posición específica está bloqueada en un momento dado.
     * 
     * @param posicion Posición a verificar
     * @param momento Momento a verificar
     * @return true si la posición está bloqueada en ese momento, false en caso contrario
     */
    public boolean posicionEstaBloqueada(Posicion posicion, LocalDateTime momento) {
        return estaActivo(momento) && puntosBloqueados.contains(posicion);
    }

    @Override
    public String toString() {
        return "Bloqueo{" +
               "inicio=" + horaInicio +
               ", fin=" + horaFin +
               ", puntos=" + puntosBloqueados.size() +
               ", tramos=" + tramos.size() +
               '}';
    }
}