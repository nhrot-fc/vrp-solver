package com.vroute.solution;

import java.time.LocalDateTime;

import com.vroute.models.Posicion;

public interface Parada {
    public enum Tipo {
        DELIVERY,
        RECARGA
    }

    Tipo getTipo();
    Posicion getPosicion();
    int getCantidadGLPEntregada();
    int getCantidadGLPRecargada();
    double getDistanciaRecorrida();
    LocalDateTime getETA();
}
