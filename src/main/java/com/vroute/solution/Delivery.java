package com.vroute.solution;

import com.vroute.models.Pedido;
import com.vroute.models.Posicion;
import java.time.LocalDateTime;

public class Delivery implements Parada {
    private final Pedido pedido;
    private final Posicion posicion;
    private final int cantidadGLPEntregada;
    private final double distanciaRecorrida;
    private final LocalDateTime eta;

    public Delivery(Pedido pedido, Posicion posicion, int cantidadGLPEntregada, double distanciaRecorrida, LocalDateTime eta) {
        this.pedido = pedido;
        this.posicion = posicion;
        this.cantidadGLPEntregada = cantidadGLPEntregada;
        this.distanciaRecorrida = distanciaRecorrida;
        this.eta = eta;
    }

    public Pedido getPedido() {
        return pedido;
    }

    @Override
    public Tipo getTipo() {
        return Tipo.DELIVERY;
    }

    @Override
    public Posicion getPosicion() {
        return posicion;
    }

    @Override
    public int getCantidadGLPEntregada() {
        return cantidadGLPEntregada;
    }

    @Override
    public int getCantidadGLPRecargada() {
        return 0;
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
