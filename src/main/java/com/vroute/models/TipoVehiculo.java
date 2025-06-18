package com.vroute.models;

public enum TipoVehiculo {
    TA("TA", 2.5, 25, 25.0),
    TB("TB", 2.0, 15, 25.0),
    TC("TC", 1.5, 10, 25.0),
    TD("TD", 1.0, 5, 25.0);

    private final String nombre;
    private final double pesoTara;
    private final int capacidadGLP;
    private final double capacidadCombustible;

    TipoVehiculo(String nombre, double pesoTara, int capacidadGLP, double capacidadCombustible) {
        this.nombre = nombre;
        this.pesoTara = pesoTara;
        this.capacidadGLP = capacidadGLP;
        this.capacidadCombustible = capacidadCombustible;
    }

    public String getNombre() {
        return nombre;
    }

    public double getPesoTara() {
        return pesoTara;
    }

    public int getCapacidadGLP() {
        return capacidadGLP;
    }

    public double getCapacidadCombustible() {
        return capacidadCombustible;
    }
}
