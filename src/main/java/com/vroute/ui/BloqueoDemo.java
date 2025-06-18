package com.vroute.ui;

import com.vroute.models.Bloqueo;
import com.vroute.models.Posicion;
import com.vroute.pathfinding.Pathfinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.utils.BloqueoLoader;
import com.vroute.utils.Constants;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demo simplificada de bloqueos y pathfinding
 */
public class BloqueoDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("V-Route - Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize((int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.9),
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.9));

            try {
                // Cargar bloqueos
                String bloqueosFile = System.getProperty("user.dir") +
                        File.separator + "data" +
                        File.separator + "bloqueos.20250419" +
                        File.separator + "202501.bloqueos.txt";

                System.out.println("Intentando cargar bloqueos desde: " + bloqueosFile);

                // Cargar el archivo real
                List<Bloqueo> bloqueos = BloqueoLoader.cargarBloqueosDesdeArchivo(bloqueosFile);
                System.out.println("\nBloqueos cargados: " + bloqueos.size());

                // Mostrar algunos detalles
                if (!bloqueos.isEmpty()) {
                    System.out.println("Primer bloqueo: " +
                            "\n - Inicio: " + bloqueos.get(0).getHoraInicio() +
                            "\n - Fin: " + bloqueos.get(0).getHoraFin() +
                            "\n - Puntos: " + bloqueos.get(0).getTramos().size());
                }

                // Configurar hora para la simulación
                LocalDateTime hora = LocalDateTime.of(2025, 1, 1, 12, 0);

                // Crear y configurar componentes
                com.vroute.ui.map.MapPanel mapPanel = new com.vroute.ui.map.MapPanel();
                com.vroute.models.Entorno entorno = new com.vroute.models.Entorno(
                        hora, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), bloqueos, null);

                // Actualizar el título de la ventana para incluir la fecha de simulación
                String fechaSimulacion = hora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                frame.setTitle("V-Route - Demo - Fecha de Simulación: " + fechaSimulacion);

                frame.add(mapPanel);
                mapPanel.setEntorno(entorno);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                mapPanel.fitToMap();

                // Generar rutas aleatorias
                generarRutas(mapPanel, entorno);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
            }
        });
    }

    private static void generarRutas(com.vroute.ui.map.MapPanel mapPanel, com.vroute.models.Entorno entorno) {
        mapPanel.limpiarRutas();

        Pathfinder pathfinder = new Pathfinder();
        Random random = new Random(42);
        int numRutas = 20; // Reducido a sólo 5 rutas

        System.out.println("Generando " + numRutas + " rutas aleatorias...");

        for (int i = 0; i < numRutas; i++) {
            Posicion origen = new Posicion(
                    random.nextInt(Constants.CITY_WIDTH_KM),
                    random.nextInt(Constants.CITY_HEIGHT_KM));

            Posicion destino = new Posicion(
                    random.nextInt(Constants.CITY_WIDTH_KM),
                    random.nextInt(Constants.CITY_HEIGHT_KM));

            PathResult ruta = pathfinder.getPath(
                    origen,
                    destino,
                    entorno.getHoraActual(),
                    entorno);

            if (ruta.getNodosDelCamino() != null && !ruta.getNodosDelCamino().isEmpty()) {
                mapPanel.agregarRuta("Ruta" + (i + 1), ruta.getNodosDelCamino());
                System.out.println("Ruta " + (i + 1) + ": " + origen + " → " + destino +
                        " (" + ruta.getDistancia() + " km)");
            } else {
                System.out.println("Ruta " + (i + 1) + ": No encontrada");
            }
        }
    }
}
