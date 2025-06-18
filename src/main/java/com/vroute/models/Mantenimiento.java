package com.vroute.models;

import java.time.LocalDateTime;

public class Mantenimiento {
    private String idVehiculo;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;

    public Mantenimiento(String idVehiculo, LocalDateTime horaInicio, LocalDateTime horaFin) {
        this.idVehiculo = idVehiculo;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public LocalDateTime getHoraInicio() {
        return horaInicio;
    }

    public LocalDateTime getHoraFin() {
        return horaFin;
    }
}
