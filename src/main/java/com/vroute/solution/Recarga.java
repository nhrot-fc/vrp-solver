package com.vroute.solution;

import java.time.LocalDateTime;

import com.vroute.models.Deposito;
import com.vroute.models.Posicion;

public class Recarga implements Parada {
    private final Deposito deposito;
    private final int cantidadGLPRecargada;
    private final double distanciaRecorrida;
    private final LocalDateTime eta;

    public Recarga(Deposito deposito, int cantidadGLPRecargada, double distanciaRecorrida, LocalDateTime eta) {
        this.deposito = deposito;
        this.cantidadGLPRecargada = cantidadGLPRecargada;
        this.distanciaRecorrida = distanciaRecorrida;
        this.eta = eta;
    }

    public Deposito getDeposito() {
        return deposito;
    }

    @Override
    public Tipo getTipo() {
        return Tipo.RECARGA;
    }

    @Override
    public Posicion getPosicion() {
        return deposito.getPosicion();
    }

    @Override
    public int getCantidadGLPEntregada() {
        return 0;
    }

    @Override
    public int getCantidadGLPRecargada() {
        return cantidadGLPRecargada;
    }

    @Override
    public double getDistanciaRecorrida() {
        return distanciaRecorrida;
    }

    @Override
    public LocalDateTime getETA() {
        return eta;
    }
}