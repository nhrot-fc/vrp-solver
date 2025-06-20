package com.vroute.ui;

import com.vroute.models.Environment;

import javafx.scene.layout.Pane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;

/**
 * Componente de UI para mostrar el mapa con vehículos, pedidos y bloqueos
 */
public class MapView extends BorderPane {

    private final Environment environment;
    private final Pane mapPane;
    private final int gridWidth;
    private final int gridHeight;
    private final int cellSize;

    /**
     * Constructor del mapa
     *
     * @param environment Entorno de simulación
     * @param width Ancho de la cuadrícula en celdas
     * @param height Alto de la cuadrícula en celdas
     * @param cellSize Tamaño de cada celda en píxeles
     */
    public MapView(Environment environment, int width, int height, int cellSize) {
        this.environment = environment;
        this.gridWidth = width;
        this.gridHeight = height;
        this.cellSize = cellSize;
        
        // Crear el panel del mapa
        mapPane = new Pane();
        mapPane.setPrefSize(width * cellSize, height * cellSize);
        
        // Crear un ScrollPane para poder mover el mapa si es más grande que la ventana
        ScrollPane scrollPane = new ScrollPane(mapPane);
        scrollPane.setPrefViewportHeight(600);
        scrollPane.setPrefViewportWidth(800);
        
        // Agregar al centro del BorderPane
        setCenter(scrollPane);
        setPadding(new Insets(10));
        
        // Dibujar el mapa inicialmente
        updateMap();
    }
    
    /**
     * Actualiza el mapa con la información actual del entorno
     */
    public void updateMap() {
        mapPane.getChildren().clear();
        
        // Dibujar la cuadrícula
        MapRenderer.drawGrid(mapPane, gridWidth, gridHeight, cellSize);
        
        // Dibujar los depósitos
        MapRenderer.drawDepots(mapPane, environment.getAuxDepots(), environment.getMainDepot(), cellSize);
        
        // Dibujar los pedidos
        MapRenderer.drawOrders(mapPane, environment.getOrderQueue(), environment, cellSize);
        
        // Dibujar los bloqueos activos
        MapRenderer.drawBlockages(mapPane, environment, environment.getCurrentTime(), cellSize);
        
        // Dibujar los vehículos
        MapRenderer.drawVehicles(mapPane, environment.getVehicles(), cellSize);
    }
    
    /**
     * Obtiene el panel del mapa
     * 
     * @return El panel donde se dibuja el mapa
     */
    public Pane getMapPane() {
        return mapPane;
    }
    
    /**
     * Obtiene el ancho de la cuadrícula
     * 
     * @return Ancho en celdas
     */
    public int getGridWidth() {
        return gridWidth;
    }
    
    /**
     * Obtiene el alto de la cuadrícula
     * 
     * @return Alto en celdas
     */
    public int getGridHeight() {
        return gridHeight;
    }
    
    /**
     * Obtiene el tamaño de la celda
     * 
     * @return Tamaño en píxeles
     */
    public int getCellSize() {
        return cellSize;
    }
}
