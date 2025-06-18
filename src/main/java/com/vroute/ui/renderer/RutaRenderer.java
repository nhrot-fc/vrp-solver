package com.vroute.ui.renderer;

import com.vroute.models.Posicion;
import com.vroute.ui.utils.MapTransformation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.List;

/**
 * Renderer para visualizar rutas en el mapa
 */
public class RutaRenderer implements Renderer<List<Posicion>> {
    // Color base para las rutas
    private static final Color COLOR_RUTA = new Color(50, 120, 220, 180);
    private static final Color COLOR_PUNTO_RUTA = new Color(30, 100, 180);
    
    // Grosor de línea
    private static final float GROSOR_LINEA = 3.0f;
    
    @Override
    public void renderizar(Graphics2D g2d, List<Posicion> ruta, MapTransformation transformation) {
        if (ruta == null || ruta.size() < 2) {
            return;
        }
        
        // Crear un array de puntos en coordenadas de pantalla
        Point[] puntos = new Point[ruta.size()];
        for (int i = 0; i < ruta.size(); i++) {
            puntos[i] = transformation.transformCoordsToScreen(ruta.get(i));
        }
        
        // Dibujar segmentos de línea entre cada par de puntos consecutivos
        Stroke originalStroke = g2d.getStroke();
        g2d.setColor(COLOR_RUTA);
        g2d.setStroke(new BasicStroke(GROSOR_LINEA, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        for (int i = 0; i < puntos.length - 1; i++) {
            g2d.drawLine(puntos[i].x, puntos[i].y, puntos[i + 1].x, puntos[i + 1].y);
        }
        
        // Dibujar flechas de dirección cada ciertos puntos
        for (int i = 1; i < puntos.length - 1; i += 3) {
            dibujarFlecha(g2d, puntos[i], puntos[i + 1]);
        }
        
        // Dibujar puntos en cada vértice de la ruta
        g2d.setStroke(originalStroke);
        for (int i = 0; i < puntos.length; i++) {
            // El primer y último punto son de un tamaño ligeramente más grande
            int tamanoCirculo = (i == 0 || i == puntos.length - 1) ? 5 : 3;
            
            g2d.setColor(COLOR_PUNTO_RUTA);
            g2d.fillOval(puntos[i].x - tamanoCirculo/2, puntos[i].y - tamanoCirculo/2, 
                        tamanoCirculo, tamanoCirculo);
                        
            // Etiqueta para el punto inicial y final
            if (i == 0) {
                g2d.setColor(Color.BLACK);
                g2d.drawString("Inicio", puntos[i].x + 5, puntos[i].y - 5);
            } else if (i == puntos.length - 1) {
                g2d.setColor(Color.BLACK);
                g2d.drawString("Fin", puntos[i].x + 5, puntos[i].y - 5);
            }
        }
    }
    
    /**
     * Dibuja una flecha de dirección entre dos puntos
     * @param g2d Contexto gráfico
     * @param desde Punto de inicio
     * @param hacia Punto de destino
     */
    private void dibujarFlecha(Graphics2D g2d, Point desde, Point hacia) {
        // Calcular la dirección y longitud del vector
        double dx = hacia.x - desde.x;
        double dy = hacia.y - desde.y;
        double longitud = Math.sqrt(dx * dx + dy * dy);
        
        // Normalizar el vector de dirección
        dx /= longitud;
        dy /= longitud;
        
        // Calcular punto en la mitad del segmento
        int puntoMedioX = (desde.x + hacia.x) / 2;
        int puntoMedioY = (desde.y + hacia.y) / 2;
        
        // Tamaño de la flecha
        int tamanoFlecha = 7;
        
        // Calcular los puntos para formar la punta de flecha
        int[] puntaX = new int[3];
        int[] puntaY = new int[3];
        
        // Punto en la dirección
        puntaX[0] = puntoMedioX + (int)(dx * tamanoFlecha);
        puntaY[0] = puntoMedioY + (int)(dy * tamanoFlecha);
        
        // Puntos perpendiculares (rotando 90 grados en ambas direcciones)
        puntaX[1] = puntoMedioX + (int)(dy * tamanoFlecha/2);
        puntaY[1] = puntoMedioY - (int)(dx * tamanoFlecha/2);
        
        puntaX[2] = puntoMedioX - (int)(dy * tamanoFlecha/2);
        puntaY[2] = puntoMedioY + (int)(dx * tamanoFlecha/2);
        
        // Dibujar la flecha
        g2d.setColor(COLOR_PUNTO_RUTA);
        g2d.fillPolygon(puntaX, puntaY, 3);
    }
}
