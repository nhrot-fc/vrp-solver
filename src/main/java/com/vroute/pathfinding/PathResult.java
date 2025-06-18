package com.vroute.pathfinding;

import com.vroute.models.Posicion;

import java.util.List;

public class PathResult {
    private double distancia;
    private double duracion;
    private List<Posicion> nodosDelCamino;

    public PathResult(double distancia, double duracion, List<Posicion> nodosDelCamino) {
        this.distancia = distancia;
        this.duracion = duracion;
        this.nodosDelCamino = nodosDelCamino;
    }

    public double getDistancia() {
        return distancia;
    }

    public double getDuracion() {
        return duracion;
    }

    public List<Posicion> getNodosDelCamino() {
        return nodosDelCamino;
    }
}
