package com.vroute;

import com.vroute.models.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Clase principal para probar el optimizador de rutas
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("======== V-Route: Test del Optimizador ========");

        // Crear el entorno de prueba
        Entorno entorno = crearEntornoPrueba();
        System.out.println("Entorno creado con " + entorno.getVehiculos().size() + " vehículos y "
                + entorno.getPedidos().size() + " pedidos");

        // Medir tiempo de ejecución
        long startTime = System.currentTimeMillis();

        // Ejecutar la optimización
        System.out.println("\nIniciando proceso de optimización...");

        long endTime = System.currentTimeMillis();

        // Mostrar resultados
        System.out.println("\n======== Resultados de la Optimización ========");
        System.out.println("Tiempo de ejecución: " + (endTime - startTime) + " ms");
    }

    /**
     * Crea un entorno simplificado para la prueba
     */
    private static Entorno crearEntornoPrueba() {
        // Fecha y hora actuales
        LocalDateTime horaActual = LocalDateTime.of(2025, 6, 18, 10, 0);

        // Crear vehículos de prueba
        List<Vehiculo> vehiculos = new ArrayList<>();

        // Camiones tipo TA (25 m³)
        vehiculos.add(new Vehiculo("TA01", TipoVehiculo.TA,
                new Posicion(12, 8), 25, 25.0, Vehiculo.EstadoOperativo.DISPONIBLE));
        vehiculos.add(new Vehiculo("TA02", TipoVehiculo.TA,
                new Posicion(12, 8), 25, 25.0, Vehiculo.EstadoOperativo.DISPONIBLE));

        // Camiones tipo TD (5 m³)
        vehiculos.add(new Vehiculo("TD01", TipoVehiculo.TD,
                new Posicion(12, 8), 5, 25.0, Vehiculo.EstadoOperativo.DISPONIBLE));
        vehiculos.add(new Vehiculo("TD02", TipoVehiculo.TD,
                new Posicion(12, 8), 5, 25.0, Vehiculo.EstadoOperativo.DISPONIBLE));

        // Crear pedidos de prueba (en diferentes ubicaciones de la ciudad)
        List<Pedido> pedidos = new ArrayList<>();

        // Pedidos cercanos a la planta principal
        LocalDateTime horaRecepcionPrueba = horaActual.minusHours(1); // Pedidos recibidos hace 1 hora
        pedidos.add(new Pedido("C001", new Posicion(15, 10), 5, horaRecepcionPrueba,
                horaActual.plusHours(4), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C002", new Posicion(18, 12), 8, horaRecepcionPrueba,
                horaActual.plusHours(6), 0, Pedido.Estado.PENDIENTE));

        // Pedidos en zona norte
        pedidos.add(new Pedido("C003", new Posicion(25, 30), 10, horaRecepcionPrueba,
                horaActual.plusHours(8), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C004", new Posicion(30, 35), 15, horaRecepcionPrueba,
                horaActual.plusHours(10), 0, Pedido.Estado.PENDIENTE));

        // Pedidos en zona este
        pedidos.add(new Pedido("C005", new Posicion(50, 10), 5, horaRecepcionPrueba,
                horaActual.plusHours(12), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("C006", new Posicion(60, 5), 10, horaRecepcionPrueba,
                horaActual.plusHours(8), 0, Pedido.Estado.PENDIENTE));

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
                new Posicion(25, 25));
        bloqueos.add(new Bloqueo(
                horaActual.withHour(9).withMinute(0),
                horaActual.withHour(15).withMinute(0),
                nodosBloqueados1));

        // Bloqueo en la ruta al este (todo el día)
        List<Posicion> nodosBloqueados2 = Arrays.asList(
                new Posicion(40, 5),
                new Posicion(45, 5));
        bloqueos.add(new Bloqueo(
                horaActual.withHour(0).withMinute(0),
                horaActual.withHour(23).withMinute(59),
                nodosBloqueados2));

        // Mantenimientos programados
        List<Mantenimiento> mantenimientos = new ArrayList<>();

        return new Entorno(horaActual, vehiculos, pedidos, depositos, bloqueos, mantenimientos);
    }
}
