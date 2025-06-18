package com.vroute.models;

import java.time.LocalDateTime;

public class RegistroServicio {
    private String idPedido;
    private String idVehiculo;
    private int cantidadServidaM3;
    private LocalDateTime horaDeServicio;

    public RegistroServicio(String idPedido, String idVehiculo, int cantidadServidaM3, LocalDateTime horaDeServicio) {
        this.idPedido = idPedido;
        this.idVehiculo = idVehiculo;
        this.cantidadServidaM3 = cantidadServidaM3;
        this.horaDeServicio = horaDeServicio;
    }

    public String getIdPedido() {
        return idPedido;
    }

    public String getIdVehiculo() {
        return idVehiculo;
    }

    public int getCantidadServidaM3() {
        return cantidadServidaM3;
    }

    public LocalDateTime getHoraDeServicio() {
        return horaDeServicio;
    }
}
