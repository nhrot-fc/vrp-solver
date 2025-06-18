package com.vroute.models;

public class Incidente {
    private String turno;
    private String idVehiculo;
    private TipoIncidente tipo;

    public Incidente(String turno, String idVehiculo, TipoIncidente tipo) {
        this.turno = turno;
        this.idVehiculo = idVehiculo;
        this.tipo = tipo;
    }

    public String getTurno() {
        return turno;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public TipoIncidente getTipo() {
        return tipo;
    }
}
