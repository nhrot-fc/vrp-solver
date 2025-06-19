package com.vroute;

import com.vroute.models.*;
import com.vroute.pathfinding.Pathfinder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    // Formatter para fechas
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        System.out.println("============== V-Route: Demostración del Optimizador Tabú ==============");
        Entorno entorno = crearEntornoPrueba();
        Pathfinder pathfinder = new Pathfinder();
    }

    private static Entorno crearEntornoPrueba() {
        // Fecha y hora actuales
        LocalDateTime horaActual = LocalDateTime.of(2025, 6, 18, 10, 0);

        // Crear vehículos de prueba
        List<Vehiculo> vehiculos = new ArrayList<>();

        // Camiones tipo TA (25 m³)
        vehiculos.add(new Vehiculo("TA01", TipoVehiculo.TA,
                new Posicion(12, 8), 25, 25.0, Vehiculo.Estado.DISPONIBLE));
        vehiculos.add(new Vehiculo("TA02", TipoVehiculo.TA,
                new Posicion(12, 8), 25, 25.0, Vehiculo.Estado.DISPONIBLE));

        // Camiones tipo TB (15 m³)
        vehiculos.add(new Vehiculo("TB01", TipoVehiculo.TB,
                new Posicion(12, 8), 15, 25.0, Vehiculo.Estado.DISPONIBLE));
        vehiculos.add(new Vehiculo("TB02", TipoVehiculo.TB,
                new Posicion(12, 8), 15, 25.0, Vehiculo.Estado.DISPONIBLE));

        // Camiones tipo TC (10 m³)
        vehiculos.add(new Vehiculo("TC01", TipoVehiculo.TC,
                new Posicion(12, 8), 10, 25.0, Vehiculo.Estado.DISPONIBLE));
        vehiculos.add(new Vehiculo("TC02", TipoVehiculo.TC,
                new Posicion(12, 8), 10, 25.0, Vehiculo.Estado.DISPONIBLE));

        // Camiones tipo TD (5 m³)
        vehiculos.add(new Vehiculo("TD01", TipoVehiculo.TD,
                new Posicion(12, 8), 5, 25.0, Vehiculo.Estado.DISPONIBLE));
        vehiculos.add(new Vehiculo("TD02", TipoVehiculo.TD,
                new Posicion(12, 8), 5, 25.0, Vehiculo.Estado.DISPONIBLE));

        // Crear pedidos de prueba (en diferentes ubicaciones de la ciudad)
        List<Pedido> pedidos = new ArrayList<>();

        // Pedidos cercanos a la planta principal
        LocalDateTime horaRecepcionPrueba = horaActual.minusHours(1); // Pedidos recibidos hace 1 hora
        pedidos.add(new Pedido("C001", new Posicion(15, 10), 5, horaRecepcionPrueba,
                horaActual.plusHours(4), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C002", new Posicion(18, 12), 8, horaRecepcionPrueba,
                horaActual.plusHours(6), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C003", new Posicion(20, 15), 12, horaRecepcionPrueba,
                horaActual.plusHours(5), 0, Pedido.Estado.PENDIENTE));

        // Pedidos en zona norte
        pedidos.add(new Pedido("C004", new Posicion(25, 30), 50, horaRecepcionPrueba,
                horaActual.plusHours(8), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C005", new Posicion(30, 35), 15, horaRecepcionPrueba,
                horaActual.plusHours(10), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C006", new Posicion(35, 40), 7, horaRecepcionPrueba,
                horaActual.plusHours(9), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C007", new Posicion(40, 45), 9, horaRecepcionPrueba,
                horaActual.plusHours(7), 0, Pedido.Estado.PENDIENTE));

        // Pedidos en zona este
        pedidos.add(new Pedido("C008", new Posicion(50, 10), 5, horaRecepcionPrueba,
                horaActual.plusHours(12), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C009", new Posicion(60, 5), 10, horaRecepcionPrueba,
                horaActual.plusHours(8), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C010", new Posicion(65, 15), 4, horaRecepcionPrueba,
                horaActual.plusHours(9), 0, Pedido.Estado.PENDIENTE));

        // Pedidos con prioridad alta (hora límite cercana)
        pedidos.add(new Pedido("C011", new Posicion(40, 20), 6, horaActual.minusMinutes(30),
                horaActual.plusHours(2), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C012", new Posicion(55, 25), 8, horaActual.minusMinutes(45),
                horaActual.plusHours(3), 0, Pedido.Estado.PENDIENTE));

        // Crear depósitos
        List<Deposito> depositos = new ArrayList<>();

        // Planta principal
        depositos.add(new Deposito("Planta Central", new Posicion(12, 8), true));

        // Tanques intermedios
        depositos.add(new Deposito("Tanque Norte", new Posicion(42, 42), false));
        depositos.add(new Deposito("Tanque Este", new Posicion(63, 3), false));

        // Crear bloqueos
        List<Bloqueo> bloqueos = new ArrayList<>();

        // Bloqueo en la zona central (de 9:00 a 15:00)
        List<Posicion> nodosBloqueados1 = Arrays.asList(
                new Posicion(20, 20),
                new Posicion(20, 25),
                new Posicion(25, 25),
                new Posicion(25, 20));
        bloqueos.add(new Bloqueo(
                horaActual.withHour(9).withMinute(0),
                horaActual.withHour(15).withMinute(0),
                nodosBloqueados1));

        // Bloqueo en la ruta al este (todo el día)
        List<Posicion> nodosBloqueados2 = Arrays.asList(
                new Posicion(40, 5),
                new Posicion(45, 5),
                new Posicion(45, 10),
                new Posicion(40, 10));
        bloqueos.add(new Bloqueo(
                horaActual.withHour(0).withMinute(0),
                horaActual.withHour(23).withMinute(59),
                nodosBloqueados2));

        // Mantenimientos programados
        List<Mantenimiento> mantenimientos = new ArrayList<>();
        // Programar mantenimiento para mañana al camión TD01
        mantenimientos.add(new Mantenimiento(
                "TD01",
                horaActual.plusDays(1).withHour(0).withMinute(0),
                horaActual.plusDays(1).withHour(23).withMinute(59)));

        return new Entorno(horaActual, vehiculos, pedidos, depositos, bloqueos, mantenimientos);
    }
}
