package com.vroute.solution;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.vroute.models.Vehiculo;

public class Ruta {
    private final Vehiculo vehiculo;
    private final List<Parada> paradas;
    private final double distanciaTotal;
    private final int glpTotalEntregado;
    private final int glpTotalRecargado;
    private final LocalDateTime tiempoEstimadoFinalizacion;

    public Ruta(Vehiculo vehiculo, List<Parada> paradas) {
        this.vehiculo = vehiculo;
        this.paradas = Collections.unmodifiableList(new ArrayList<>(paradas));
        this.distanciaTotal = calcularDistanciaTotal();
        this.glpTotalEntregado = calcularGLPTotalEntregado();
        this.glpTotalRecargado = calcularGLPTotalRecargado();
        this.tiempoEstimadoFinalizacion = calcularTiempoEstimadoFinalizacion();
    }

    public Vehiculo getVehiculo() {
        return vehiculo;
    }

    public List<Parada> getParadas() {
        return paradas;
    }

    public double getDistanciaTotal() {
        return distanciaTotal;
    }

    public int getGLPTotalEntregado() {
        return glpTotalEntregado;
    }

    public int getGLPTotalRecargado() {
        return glpTotalRecargado;
    }
    
    public LocalDateTime getTiempoEstimadoFinalizacion() {
        return tiempoEstimadoFinalizacion;
    }
    
    public boolean esFactible() {
        // Check if the route is feasible with the current vehicle
        return vehiculo.getTipo().getCapacidadGLP() >= glpTotalEntregado;
    }
    
    // Calculate methods
    private double calcularDistanciaTotal() {
        return paradas.stream().mapToDouble(Parada::getDistanciaRecorrida).sum();
    }

    private int calcularGLPTotalEntregado() {
        return paradas.stream().mapToInt(Parada::getCantidadGLPEntregada).sum();
    }

    private int calcularGLPTotalRecargado() {
        return paradas.stream().mapToInt(Parada::getCantidadGLPRecargada).sum();
    }
    
    private LocalDateTime calcularTiempoEstimadoFinalizacion() {
        if (paradas.isEmpty()) {
            return LocalDateTime.now();
        }
        return paradas.get(paradas.size() - 1).getETA();
    }
}
