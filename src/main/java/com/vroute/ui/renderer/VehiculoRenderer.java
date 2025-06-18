package com.vroute.ui.renderer;

import com.vroute.models.Posicion;
import com.vroute.models.Vehiculo;
import com.vroute.ui.utils.MapTransformation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;

/**
 * Renderer para visualizar vehículos en el mapa
 */
public class VehiculoRenderer implements Renderer<Vehiculo> {
    // Colores por tipo de vehículo
    private static final Map<String, Color> COLOR_POR_TIPO = new HashMap<>();
    static {
        COLOR_POR_TIPO.put("TA", new Color(0, 100, 200)); // Azul
        COLOR_POR_TIPO.put("TB", new Color(0, 150, 220)); // Azul claro
        COLOR_POR_TIPO.put("TC", new Color(100, 0, 180)); // Morado
        COLOR_POR_TIPO.put("TD", new Color(150, 50, 200)); // Lila
    }
    
    // Colores por estado operativo
    private static final Map<Vehiculo.EstadoOperativo, Color> COLOR_POR_ESTADO = new HashMap<>();
    static {
        COLOR_POR_ESTADO.put(Vehiculo.EstadoOperativo.DISPONIBLE, new Color(0, 150, 0)); // Verde
        COLOR_POR_ESTADO.put(Vehiculo.EstadoOperativo.EN_RUTA, new Color(50, 100, 200)); // Azul
        COLOR_POR_ESTADO.put(Vehiculo.EstadoOperativo.EN_MANTENIMIENTO, new Color(200, 150, 0)); // Naranja
        COLOR_POR_ESTADO.put(Vehiculo.EstadoOperativo.AVERIADO, new Color(200, 0, 0)); // Rojo
    }
    
    // Tamaño base para dibujar el vehículo (se ajusta según zoom)
    private static final int TAMANO_BASE = 8;
    
    @Override
    public void renderizar(Graphics2D g2d, Vehiculo vehiculo, MapTransformation transformation) {
        Posicion posicion = vehiculo.getPosicionActual();
        Point punto = transformation.transformCoordsToScreen(posicion);
        
        // Determinar el tamaño basado en el tipo de vehículo
        int tamanoVehiculo = TAMANO_BASE;
        switch (vehiculo.getTipo()) {
            case TA:
                tamanoVehiculo = TAMANO_BASE + 5;
                break;
            case TB:
                tamanoVehiculo = TAMANO_BASE + 3;
                break;
            case TC:
                tamanoVehiculo = TAMANO_BASE + 2;
                break;
            case TD:
                tamanoVehiculo = TAMANO_BASE;
                break;
        }
        
        // Dibujar contorno según tipo de vehículo
        Color colorTipo = COLOR_POR_TIPO.getOrDefault(
            vehiculo.getTipo().getNombre(), new Color(100, 100, 100));
        g2d.setColor(colorTipo);
        
        // Crear forma de camión (rectángulo con triángulo adelante, como un camión visto desde arriba)
        Polygon truck = new Polygon();
        truck.addPoint(punto.x - tamanoVehiculo, punto.y - tamanoVehiculo/2);
        truck.addPoint(punto.x - tamanoVehiculo/2, punto.y - tamanoVehiculo/2);
        truck.addPoint(punto.x, punto.y);
        truck.addPoint(punto.x - tamanoVehiculo/2, punto.y + tamanoVehiculo/2);
        truck.addPoint(punto.x - tamanoVehiculo, punto.y + tamanoVehiculo/2);
        
        g2d.fill(truck);
        
        // Dibujar indicador de estado
        Color colorEstado = COLOR_POR_ESTADO.getOrDefault(
            vehiculo.getEstadoOperativo(), new Color(100, 100, 100));
        g2d.setColor(colorEstado);
        g2d.fillOval(punto.x - tamanoVehiculo/4, punto.y - tamanoVehiculo/4, 
                     tamanoVehiculo/2, tamanoVehiculo/2);
                     
        // Dibujar ID del vehículo
        g2d.setColor(Color.BLACK);
        g2d.drawString(vehiculo.getId(), punto.x + tamanoVehiculo/2, punto.y);
        
        // Indicador visual de nivel de GLP
        double nivelRelativoGLP = vehiculo.getCargaGLPActual() / (double) vehiculo.getTipo().getCapacidadGLP();
        int anchoBarraGLP = tamanoVehiculo;
        int altoBarraGLP = 3;
        
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRect(punto.x - tamanoVehiculo, punto.y + tamanoVehiculo + 2, 
                     anchoBarraGLP, altoBarraGLP);
                     
        g2d.setColor(new Color(50, 150, 250));
        g2d.fillRect(punto.x - tamanoVehiculo, punto.y + tamanoVehiculo + 2, 
                     (int)(anchoBarraGLP * nivelRelativoGLP), altoBarraGLP);
    }
}
