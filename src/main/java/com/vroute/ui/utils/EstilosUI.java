package com.vroute.ui.utils;

import java.awt.Color;
import java.awt.Font;

/**
 * Clase de utilidad para mantener los colores y estilos comunes de la interfaz
 */
public class EstilosUI {
    // Paleta de colores principal
    public static final Color COLOR_FONDO_MAPA = new Color(240, 240, 240);
    public static final Color COLOR_CUADRICULA = new Color(210, 210, 210);
    public static final Color COLOR_BORDE_ELEMENTOS = new Color(50, 50, 50);
    
    // Colores para elementos de estado
    public static final Color COLOR_DISPONIBLE = new Color(0, 150, 50);
    public static final Color COLOR_EN_USO = new Color(50, 100, 200);
    public static final Color COLOR_ADVERTENCIA = new Color(200, 150, 0);
    public static final Color COLOR_ERROR = new Color(200, 0, 0);
    public static final Color COLOR_COMPLETADO = new Color(50, 150, 50);
    
    // Colores para diferentes tipos de vehículos
    public static final Color COLOR_VEHICULO_TA = new Color(0, 100, 200);
    public static final Color COLOR_VEHICULO_TB = new Color(0, 150, 220);
    public static final Color COLOR_VEHICULO_TC = new Color(100, 0, 180);
    public static final Color COLOR_VEHICULO_TD = new Color(150, 50, 200);
    
    // Fuentes
    public static final Font FUENTE_ETIQUETA = new Font("SansSerif", Font.PLAIN, 10);
    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 12);
    
    // Tamaños de elementos UI
    public static final int TAMANO_VEHICULO_BASE = 8;
    public static final int TAMANO_PEDIDO_BASE = 8;
    public static final int TAMANO_DEPOSITO = 12;
    public static final int TAMANO_PLANTA = 16;
    
    // Transparencias
    public static final int ALFA_BLOQUEO_ACTIVO = 180;
    public static final int ALFA_BLOQUEO_INACTIVO = 120;
    public static final int ALFA_RUTA = 180;
    
    /**
     * Obtiene un color por estado operativo de vehículo
     * @param estado Estado del vehículo
     * @return Color correspondiente
     */
    public static Color getColorPorEstadoVehiculo(String estado) {
        switch (estado) {
            case "DISPONIBLE":
                return COLOR_DISPONIBLE;
            case "EN_RUTA":
                return COLOR_EN_USO;
            case "EN_MANTENIMIENTO":
                return COLOR_ADVERTENCIA;
            case "AVERIADO":
                return COLOR_ERROR;
            default:
                return Color.GRAY;
        }
    }
    
    /**
     * Obtiene un color por estado de pedido
     * @param estado Estado del pedido
     * @return Color correspondiente
     */
    public static Color getColorPorEstadoPedido(String estado) {
        switch (estado) {
            case "PENDIENTE":
                return COLOR_ERROR;
            case "EN_RUTA":
                return COLOR_ADVERTENCIA;
            case "PARCIALMENTE_COMPLETADO":
                return COLOR_EN_USO;
            case "COMPLETADO":
                return COLOR_COMPLETADO;
            default:
                return Color.GRAY;
        }
    }
}
