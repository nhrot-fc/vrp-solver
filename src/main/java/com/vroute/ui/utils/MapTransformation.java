package com.vroute.ui.utils;

import com.vroute.models.Posicion;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * Clase de utilidad para transformar coordenadas del mapa a coordenadas de pantalla y viceversa
 */
public class MapTransformation {
    private static final double DEFAULT_SCALE = 10.0; // Píxeles por kilómetro (1 unidad = 1km)
    private AffineTransform transform;
    private AffineTransform inverseTransform;
    
    /**
     * Constructor que inicializa la transformación con valores predeterminados
     */
    public MapTransformation() {
        transform = new AffineTransform();
        transform.scale(DEFAULT_SCALE, DEFAULT_SCALE);
        transform.translate(20, 20); // Margen inicial
        updateInverseTransform();
    }
    
    /**
     * Actualiza la transformación inversa cuando cambia la transformación principal
     */
    private void updateInverseTransform() {
        try {
            inverseTransform = transform.createInverse();
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Transforma coordenadas del mapa (km) a coordenadas de pantalla (píxeles)
     * @param posicion Posición en el mapa (coordenadas de la ciudad)
     * @return Punto en la pantalla
     */
    public Point transformCoordsToScreen(Posicion posicion) {
        Point2D.Double mapPoint = new Point2D.Double(posicion.getX(), posicion.getY());
        Point2D.Double screenPoint = new Point2D.Double();
        transform.transform(mapPoint, screenPoint);
        return new Point((int) screenPoint.x, (int) screenPoint.y);
    }
    
    /**
     * Transforma coordenadas de pantalla (píxeles) a coordenadas del mapa (km)
     * @param x Coordenada X en la pantalla
     * @param y Coordenada Y en la pantalla
     * @return Posición en el mapa
     */
    public Posicion transformScreenToCoords(int x, int y) {
        Point2D.Double screenPoint = new Point2D.Double(x, y);
        Point2D.Double mapPoint = new Point2D.Double();
        inverseTransform.transform(screenPoint, mapPoint);
        return new Posicion((int) Math.round(mapPoint.x), (int) Math.round(mapPoint.y));
    }
    
    /**
     * Realiza operación de zoom centrado en un punto
     * @param scaleFactor Factor de escala (> 1 para acercar, < 1 para alejar)
     * @param zoomCenter Punto central del zoom
     */
    public void zoom(double scaleFactor, Point zoomCenter) {
        // Guardar las coordenadas del punto centro antes del zoom
        Point2D.Double beforeZoom = new Point2D.Double();
        inverseTransform.transform(new Point2D.Double(zoomCenter.x, zoomCenter.y), beforeZoom);
        
        // Aplicar el zoom
        transform.translate(zoomCenter.x, zoomCenter.y);
        transform.scale(scaleFactor, scaleFactor);
        transform.translate(-zoomCenter.x, -zoomCenter.y);
        updateInverseTransform();
        
        // Calcular el desplazamiento adicional para mantener el punto centrado
        Point2D.Double afterZoom = new Point2D.Double();
        transform.transform(beforeZoom, afterZoom);
        
        // Compensar el desplazamiento
        transform.translate(zoomCenter.x - afterZoom.x, zoomCenter.y - afterZoom.y);
        updateInverseTransform();
    }
    
    /**
     * Desplaza el mapa
     * @param dx Desplazamiento horizontal en píxeles
     * @param dy Desplazamiento vertical en píxeles
     */
    public void pan(double dx, double dy) {
        transform.translate(dx, dy);
        updateInverseTransform();
    }
    
    /**
     * Reinicia la transformación
     * @param width Ancho del panel
     * @param height Alto del panel
     */
    public void reset(int width, int height) {
        transform = new AffineTransform();
        transform.scale(DEFAULT_SCALE, DEFAULT_SCALE);
        transform.translate(width / (2 * DEFAULT_SCALE), height / (2 * DEFAULT_SCALE));
        updateInverseTransform();
    }
    
    /**
     * Ajusta la vista para que todo el mapa sea visible, centrando el mapa en el panel
     * @param panelWidth Ancho del panel
     * @param panelHeight Alto del panel
     * @param mapWidth Ancho del mapa en km
     * @param mapHeight Alto del mapa en km
     */
    public void fitToView(int panelWidth, int panelHeight, int mapWidth, int mapHeight) {
        // Calcular las escalas necesarias para ajustar el mapa en cada dimensión
        double scaleX = (panelWidth - 40) / (double) mapWidth;
        double scaleY = (panelHeight - 40) / (double) mapHeight;
        
        // Usar la escala menor para que todo el mapa sea visible
        double scale = Math.min(scaleX, scaleY);
        
        // Calcular el desplazamiento para centrar el mapa en el panel
        double offsetX = (panelWidth - (mapWidth * scale)) / 2;
        double offsetY = (panelHeight - (mapHeight * scale)) / 2;
        
        // Crear la nueva transformación
        transform = new AffineTransform();
        transform.translate(offsetX, offsetY); // Centro el mapa
        transform.scale(scale, scale);
        
        updateInverseTransform();
    }
}
