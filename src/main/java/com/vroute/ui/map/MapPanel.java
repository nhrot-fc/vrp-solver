package com.vroute.ui.map;

import com.vroute.models.Bloqueo;
import com.vroute.models.Deposito;
import com.vroute.models.Entorno;
import com.vroute.models.Pedido;
import com.vroute.models.Posicion;
import com.vroute.models.Vehiculo;
import com.vroute.ui.renderer.BloqueoRenderer;
import com.vroute.ui.renderer.DepositoRenderer;
import com.vroute.ui.renderer.PedidoRenderer;
import com.vroute.ui.renderer.RutaRenderer;
import com.vroute.ui.renderer.VehiculoRenderer;
import com.vroute.ui.utils.MapTransformation;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel principal para visualizar el mapa y todos sus elementos
 */
public class MapPanel extends JPanel {
    // Constantes para el tamaño del mapa
    public static final int CITY_WIDTH_KM = 70;
    public static final int CITY_HEIGHT_KM = 50;
    public static final Color GRID_COLOR = new Color(200, 200, 200);
    public static final Color BACKGROUND_COLOR = new Color(240, 240, 240);
    
    // Transformación del mapa (zoom, pan)
    private MapTransformation transformation;
    
    // Estado del entorno
    private Entorno entorno;
    
    // Renderers para los diferentes elementos
    private Map<String, List<Posicion>> rutasActivas;
    private RutaRenderer rutaRenderer;
    private VehiculoRenderer vehiculoRenderer;
    private PedidoRenderer pedidoRenderer;
    private DepositoRenderer depositoRenderer;
    private BloqueoRenderer bloqueoRenderer;
    
    // Estado de la visualización
    private boolean showGrid = true;
    
    /**
     * Constructor del panel de mapa
     */
    public MapPanel() {
        transformation = new MapTransformation();
        rutasActivas = new HashMap<>();
        
        // Inicializar los renderers
        rutaRenderer = new RutaRenderer();
        vehiculoRenderer = new VehiculoRenderer();
        pedidoRenderer = new PedidoRenderer();
        depositoRenderer = new DepositoRenderer();
        // El BloqueoRenderer se inicializará cuando se establezca un entorno
        bloqueoRenderer = null;
        
        setBackground(BACKGROUND_COLOR);
        
        // Agregar un listener para redimensionamiento automático
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                fitToMap(); // Ajustar el mapa cuando cambie el tamaño del panel
            }
        });
    }
    
    // Se eliminan los controladores de evento del mouse para simplificar la interfaz
    
    /**
     * Actualiza el entorno que se debe visualizar
     * @param entorno El nuevo estado del entorno
     */
    public void setEntorno(Entorno entorno) {
        this.entorno = entorno;
        // Actualizar el bloqueoRenderer con el nuevo entorno
        bloqueoRenderer = new BloqueoRenderer(entorno);
        repaint();
    }
    
    /**
     * Agrega una ruta para visualización
     * @param rutaId ID único para la ruta
     * @param puntos Lista de posiciones que conforman la ruta
     */
    public void agregarRuta(String rutaId, List<Posicion> puntos) {
        rutasActivas.put(rutaId, puntos);
        repaint();
    }
    
    /**
     * Elimina una ruta
     * @param rutaId ID de la ruta a eliminar
     */
    public void eliminarRuta(String rutaId) {
        rutasActivas.remove(rutaId);
        repaint();
    }
    
    /**
     * Elimina todas las rutas
     */
    public void limpiarRutas() {
        rutasActivas.clear();
        repaint();
    }
    
    /**
     * Activa o desactiva la visualización de la cuadrícula
     */
    public void toggleGrid() {
        showGrid = !showGrid;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Configurar antialiasing para mejor calidad visual
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dibujar la cuadrícula si está activada
        if (showGrid) {
            dibujarCuadricula(g2d);
        }
        
        // Dibujar todas las rutas activas
        for (Map.Entry<String, List<Posicion>> ruta : rutasActivas.entrySet()) {
            rutaRenderer.renderizar(g2d, ruta.getValue(), transformation);
        }
        
        // Dibujar elementos del entorno si hay uno disponible
        if (entorno != null && bloqueoRenderer != null) {
            // 3. Dibujar bloqueos activos (en rojo, destacados)
            for (Bloqueo bloqueo : entorno.getBloqueosActivos()) {
                bloqueoRenderer.renderizar(g2d, bloqueo, transformation);
            }
            
            // Dibujar depósitos
            if (entorno.getDepositos() != null) {
                for (Deposito deposito : entorno.getDepositos()) {
                    depositoRenderer.renderizar(g2d, deposito, transformation);
                }
            }
            
            // Dibujar pedidos
            if (entorno.getPedidos() != null) {
                for (Pedido pedido : entorno.getPedidos()) {
                    pedidoRenderer.renderizar(g2d, pedido, transformation);
                }
            }
            
            // Dibujar vehículos
            if (entorno.getVehiculos() != null) {
                for (Vehiculo vehiculo : entorno.getVehiculos()) {
                    vehiculoRenderer.renderizar(g2d, vehiculo, transformation);
                }
            }
        }
        
        g2d.dispose();
    }
    
    /**
     * Dibuja la cuadrícula del mapa
     */
    private void dibujarCuadricula(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(0.5f));
        
        // Dibujar líneas horizontales
        for (int y = 0; y <= CITY_HEIGHT_KM; y++) {
            Point startPoint = transformation.transformCoordsToScreen(new Posicion(0, y));
            Point endPoint = transformation.transformCoordsToScreen(new Posicion(CITY_WIDTH_KM, y));
            g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
        }
        
        // Dibujar líneas verticales
        for (int x = 0; x <= CITY_WIDTH_KM; x++) {
            Point startPoint = transformation.transformCoordsToScreen(new Posicion(x, 0));
            Point endPoint = transformation.transformCoordsToScreen(new Posicion(x, CITY_HEIGHT_KM));
            g2d.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
        }
    }
    
    /**
     * Reinicia la transformación del mapa (zoom y pan)
     */
    public void resetTransformation() {
        transformation.reset(getWidth(), getHeight());
        repaint();
    }
    
    /**
     * Ajusta el zoom para que todo el mapa sea visible con un pequeño margen
     */
    public void fitToMap() {
        // Agregar un margen del 5% alrededor del mapa para mejor visualización
        transformation.fitToView(
            getWidth(), 
            getHeight(), 
            (int)(CITY_WIDTH_KM * 1.05), 
            (int)(CITY_HEIGHT_KM * 1.05)
        );
        repaint();
    }
    
    @Override
    public Dimension getPreferredSize() {
        // Obtener dimensiones de la pantalla
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        // Calcular el tamaño para mantener proporción 4:3 y usar mayor parte de la pantalla
        int maxWidth = (int)(screenSize.width * 0.85);
        int maxHeight = (int)(screenSize.height * 0.85);
        
        int width, height;
        
        // Mantener relación 4:3
        if (maxWidth / 4 > maxHeight / 3) {
            // Limitar por altura
            height = maxHeight;
            width = height * 4 / 3;
        } else {
            // Limitar por anchura
            width = maxWidth;
            height = width * 3 / 4;
        }
        
        return new Dimension(width, height);
    }
}
