package com.vroute.models;

import com.vroute.utils.Constants;

public class Deposito {
    private String id;
    private Posicion posicion;
    private int capacidadGLP;
    private boolean esPlantaPrincipal;

    public Deposito(String id, Posicion ubicacion, boolean esPlantaPrincipal) {
        this.id = id;
        this.posicion = ubicacion;
        this.esPlantaPrincipal = esPlantaPrincipal;
        if (!esPlantaPrincipal) {
            this.capacidadGLP = Constants.INTERMEDIATE_TANK_CAPACITY_M3;
        } else {
            this.capacidadGLP = Integer.MAX_VALUE;
        }
    }

    public String getId() {
        return id;
    }

    public Posicion getPosicion() {
        return posicion;
    }

    public int getCapacidadGLP() {
        return capacidadGLP;
    }

    public boolean esPlantaPrincipal() {
        return esPlantaPrincipal;
    }
}
