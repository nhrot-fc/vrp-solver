package com.vroute.models;

import java.time.LocalDateTime;

public class Pedido {
    public enum Estado {
        PENDIENTE,
        EN_RUTA,
        PARCIALMENTE_COMPLETADO,
        COMPLETADO
    }

    private String id;
    private Posicion posicion;
    private int cantidadTotalGLP;
    private LocalDateTime horaRecepcion;
    private LocalDateTime horaLimite;
    private int cantidadSatisfechaGLP;
    private Estado estado;

    public Pedido(String id, Posicion ubicacion, int cantidadTotalGLP, LocalDateTime horaRecepcion,
            LocalDateTime horaLimite, int cantidadSatisfechaGLP, Estado estado) {
        this.id = id;
        this.posicion = ubicacion;
        this.cantidadTotalGLP = cantidadTotalGLP;
        this.horaRecepcion = horaRecepcion;
        this.horaLimite = horaLimite;
        this.cantidadSatisfechaGLP = cantidadSatisfechaGLP;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public Posicion getPosicion() {
        return posicion;
    }

    public int getCantidadTotalGLP() {
        return cantidadTotalGLP;
    }

    public LocalDateTime getHoraRecepcion() {
        return horaRecepcion;
    }

    public LocalDateTime getHoraLimite() {
        return horaLimite;
    }

    public int getCantidadSatisfechaGLP() {
        return cantidadSatisfechaGLP;
    }

    public Estado getEstado() {
        return estado;
    }
}
