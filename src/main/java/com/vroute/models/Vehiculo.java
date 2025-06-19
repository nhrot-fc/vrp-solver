package com.vroute.models;

public class Vehiculo {
    public enum Estado {
        DISPONIBLE,
        EN_RUTA,
        EN_MANTENIMIENTO,
        AVERIADO
    }

    private String id;
    private TipoVehiculo tipo;
    private Posicion posicionActual;
    private int cargaGLPActual;
    private double nivelCombustibleActual;
    private Estado estado;

    public Vehiculo(String id, TipoVehiculo tipo, Posicion posicionActual, int cargaGLPActual, double nivelCombustibleActual, Estado estado) {
        this.id = id;
        this.tipo = tipo;
        this.posicionActual = posicionActual;
        this.cargaGLPActual = cargaGLPActual;
        this.nivelCombustibleActual = nivelCombustibleActual;
        this.estado = estado;
    }

    public String getId() {
        return id;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public Posicion getPosicionActual() {
        return posicionActual;
    }

    public int getCargaGLPActual() {
        return cargaGLPActual;
    }

    public double getNivelCombustibleActual() {
        return nivelCombustibleActual;
    }

    public Estado getEstado() {
        return estado;
    }
}
