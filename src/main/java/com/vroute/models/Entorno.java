package com.vroute.models;

import java.time.LocalDateTime;
import java.util.List;

public class Entorno {
    private final LocalDateTime horaActual;
    private final List<Vehiculo> vehiculos;
    private final List<Pedido> pedidos;
    private final List<Deposito> depositos;
    private final List<Bloqueo> bloqueos;
    private final List<Mantenimiento> mantenimientos;

    public Entorno(LocalDateTime horaActual, List<Vehiculo> vehiculos, List<Pedido> pedidos, List<Deposito> depositos,
            List<Bloqueo> bloqueos, List<Mantenimiento> mantenimientos) {
        this.horaActual = horaActual;
        this.vehiculos = vehiculos;
        this.pedidos = pedidos;
        this.depositos = depositos;
        this.bloqueos = bloqueos;
        this.mantenimientos = mantenimientos;
    }

    public LocalDateTime getHoraActual() {
        return horaActual;
    }

    public List<Vehiculo> getVehiculos() {
        return vehiculos;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public List<Deposito> getDepositos() {
        return depositos;
    }

    public List<Bloqueo> getBloqueos() {
        return bloqueos;
    }

    public List<Mantenimiento> getMantenimientos() {
        return mantenimientos;
    }

    public List<Bloqueo> getBloqueosActivos() {
        if (bloqueos == null) {
            return java.util.Collections.emptyList();
        }

        return bloqueos.stream()
                .filter(bloqueo -> bloqueo.estaActivo(horaActual))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Bloqueo> getBloqueosActivosEnMomento(LocalDateTime momento) {
        if (momento == null) {
            throw new IllegalArgumentException("La hora actual no puede ser nula");
        }
        if (bloqueos == null) {
            return java.util.Collections.emptyList();
        }

        return bloqueos.stream()
                .filter(bloqueo -> bloqueo.estaActivo(momento))
                .collect(java.util.stream.Collectors.toList());
    }
}
