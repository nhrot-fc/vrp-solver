package com.vroute.models;

public enum TipoIncidente {
    TI1("TI1", 2, 0, 0),
    TI2("TI2", 2, 1, 0),
    TI3("TI3", 4, 0, 1);

    private final String nombre;
    private final int duracionDetencionHoras;
    private final int turnosInoperativo;
    private final int diasInoperativo;

    TipoIncidente(String nombre, int duracionDetencionHoras, int turnosInoperativo, int diasInoperativo) {
        this.nombre = nombre;
        this.duracionDetencionHoras = duracionDetencionHoras;
        this.turnosInoperativo = turnosInoperativo;
        this.diasInoperativo = diasInoperativo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getDuracionDetencionHoras() {
        return duracionDetencionHoras;
    }

    public int getTurnosInoperativo() {
        return turnosInoperativo;
    }

    public int getDiasInoperativo() {
        return diasInoperativo;
    }
}
