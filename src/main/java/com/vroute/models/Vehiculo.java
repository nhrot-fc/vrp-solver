package com.vroute.models;

public class Vehiculo {
    public enum EstadoOperativo {
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
    private EstadoOperativo estadoOperativo;

    public Vehiculo(String id, TipoVehiculo tipo, Posicion posicionActual, int cargaGLPActual, double nivelCombustibleActual, EstadoOperativo estadoOperativo) {
        this.id = id;
        this.tipo = tipo;
        this.posicionActual = posicionActual;
        this.cargaGLPActual = cargaGLPActual;
        this.nivelCombustibleActual = nivelCombustibleActual;
        this.estadoOperativo = estadoOperativo;
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

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }
}
