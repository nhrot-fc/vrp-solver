package com.vroute.ui.renderer;

import com.vroute.models.Pedido;
import com.vroute.models.Posicion;
import com.vroute.ui.utils.MapTransformation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * Renderer para visualizar pedidos en el mapa
 */
public class PedidoRenderer implements Renderer<Pedido> {
    // Colores por estado del pedido
    private static final Color COLOR_PENDIENTE = new Color(200, 0, 0);
    private static final Color COLOR_EN_RUTA = new Color(200, 150, 0);
    private static final Color COLOR_PARCIAL = new Color(0, 150, 150);
    private static final Color COLOR_COMPLETADO = new Color(0, 150, 0);
    
    // Tamaño base del pedido en píxeles
    private static final int TAMANO_BASE = 8;
    
    @Override
    public void renderizar(Graphics2D g2d, Pedido pedido, MapTransformation transformation) {
        Posicion posicion = pedido.getPosicion();
        Point punto = transformation.transformCoordsToScreen(posicion);
        
        // Determinar el tamaño basado en la cantidad de GLP pedida
        // Mínimo 5m³, máximo 30m³ (ajustamos el tamaño visual proporcionalmente)
        int cantidadGLP = pedido.getCantidadTotalGLP();
        int tamanoPedido = TAMANO_BASE;
        
        if (cantidadGLP > 5) {
            // Ajuste logarítmico para que visualmente no haya tanta diferencia
            tamanoPedido += Math.min(8, (int) (Math.log(cantidadGLP) * 2));
        }
        
        // Seleccionar color según estado
        Color color;
        switch (pedido.getEstado()) {
            case PENDIENTE:
                color = COLOR_PENDIENTE;
                break;
            case EN_RUTA:
                color = COLOR_EN_RUTA;
                break;
            case PARCIALMENTE_COMPLETADO:
                color = COLOR_PARCIAL;
                break;
            case COMPLETADO:
                color = COLOR_COMPLETADO;
                break;
            default:
                color = Color.GRAY;
        }
        
        // Dibujar el pedido como un rombo
        int[] xPoints = {
            punto.x, 
            punto.x + tamanoPedido / 2,
            punto.x, 
            punto.x - tamanoPedido / 2
        };
        
        int[] yPoints = {
            punto.y - tamanoPedido / 2,
            punto.y, 
            punto.y + tamanoPedido / 2,
            punto.y
        };
        
        // Dibujar fondo
        g2d.setColor(color);
        g2d.fillPolygon(xPoints, yPoints, 4);
        
        // Dibujar contorno
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawPolygon(xPoints, yPoints, 4);
        
        // Dibujar ID del pedido
        g2d.drawString(pedido.getId(), punto.x + tamanoPedido, punto.y);
        
        // Indicador visual de cantidad satisfecha
        if (pedido.getCantidadSatisfechaGLP() > 0) {
            double ratio = (double) pedido.getCantidadSatisfechaGLP() / pedido.getCantidadTotalGLP();
            int anchoIndicador = (int) (tamanoPedido * ratio);
            
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.fillRect(punto.x - tamanoPedido/2, punto.y + tamanoPedido + 2, 
                        tamanoPedido, 3);
                        
            g2d.setColor(COLOR_COMPLETADO);
            g2d.fillRect(punto.x - tamanoPedido/2, punto.y + tamanoPedido + 2, 
                        anchoIndicador, 3);
        }
    }
}
