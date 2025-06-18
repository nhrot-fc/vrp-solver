package com.vroute.ui.renderer;

import com.vroute.models.Deposito;
import com.vroute.models.Posicion;
import com.vroute.ui.utils.MapTransformation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * Renderer para visualizar depósitos en el mapa
 */
public class DepositoRenderer implements Renderer<Deposito> {
    // Colores para depósitos
    private static final Color COLOR_PLANTA_PRINCIPAL = new Color(50, 50, 200);
    private static final Color COLOR_DEPOSITO_SECUNDARIO = new Color(100, 100, 220);
    private static final Color COLOR_CONTORNO = new Color(30, 30, 120);
    
    // Tamaños
    private static final int TAMANO_PLANTA = 16;
    private static final int TAMANO_DEPOSITO = 12;
    
    @Override
    public void renderizar(Graphics2D g2d, Deposito deposito, MapTransformation transformation) {
        Posicion posicion = deposito.getUbicacion();
        Point punto = transformation.transformCoordsToScreen(posicion);
        
        int tamano = deposito.esPlantaPrincipal() ? TAMANO_PLANTA : TAMANO_DEPOSITO;
        Color color = deposito.esPlantaPrincipal() ? COLOR_PLANTA_PRINCIPAL : COLOR_DEPOSITO_SECUNDARIO;
        
        // Dibujar círculo para representar el depósito
        g2d.setColor(color);
        g2d.fillOval(punto.x - tamano/2, punto.y - tamano/2, tamano, tamano);
        
        // Dibujar contorno
        g2d.setColor(COLOR_CONTORNO);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(punto.x - tamano/2, punto.y - tamano/2, tamano, tamano);
        
        // Dibujar una cruz en el centro
        g2d.drawLine(punto.x - tamano/4, punto.y, punto.x + tamano/4, punto.y);
        g2d.drawLine(punto.x, punto.y - tamano/4, punto.x, punto.y + tamano/4);
        
        // Dibujar ID del depósito
        g2d.setColor(Color.BLACK);
        g2d.drawString(deposito.getId(), punto.x + tamano/2 + 2, punto.y);
        
        // Si es planta principal, dibujar un segundo círculo para destacarlo
        if (deposito.esPlantaPrincipal()) {
            g2d.setColor(COLOR_CONTORNO);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawOval(punto.x - tamano/2 - 3, punto.y - tamano/2 - 3, tamano + 6, tamano + 6);
        }
    }
}
