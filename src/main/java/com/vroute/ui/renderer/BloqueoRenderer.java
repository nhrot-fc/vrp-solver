package com.vroute.ui.renderer;

import com.vroute.models.Bloqueo;
import com.vroute.models.Entorno;
import com.vroute.models.Posicion;
import com.vroute.ui.utils.MapTransformation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Renderer para visualizar bloqueos activos en el mapa.
 * Esta clase solo se utiliza para los bloqueos activos, ya que el filtrado se hace en el Entorno.
 */
public class BloqueoRenderer implements Renderer<Bloqueo> {
    // Colores para bloqueos activos
    private static final Color COLOR_RELLENO = new Color(255, 0, 0, 200); // Rojo brillante semi-transparente
    private static final Color COLOR_CONTORNO = new Color(139, 0, 0);     // Rojo oscuro
    private static final Color COLOR_TEXTO = new Color(139, 0, 0);        // Rojo oscuro para texto
    
    // Constantes de estilo
    private static final float GROSOR_LINEA = 5.0f;       // Grosor de la línea de bloqueo
    private static final int TAMANO_MARCADOR = 6;         // Tamaño de los marcadores en los puntos
    private static final String TEXTO_ESTADO = "ACTIVO";  // Texto de estado para las etiquetas
    
    // Formato de fecha-hora para etiquetas
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM");
    
    /**
     * Constructor para BloqueoRenderer
     * @param entorno El entorno de simulación con la hora actual
     */
    public BloqueoRenderer(Entorno entorno) {
        // Ya no necesitamos guardar la referencia al entorno
        // porque solo renderizamos bloques activos
    }
    
    @Override
    public void renderizar(Graphics2D g2d, Bloqueo bloqueo, MapTransformation transformation) {
        List<Posicion> tramos = bloqueo.getTramos();
        if (tramos == null || tramos.isEmpty()) {
            return;
        }
        
        // Dibujar los segmentos de línea entre cada par de posiciones consecutivas
        for (int i = 0; i < tramos.size() - 1; i++) {
            Point inicio = transformation.transformCoordsToScreen(tramos.get(i));
            Point fin = transformation.transformCoordsToScreen(tramos.get(i + 1));
            
            // Dibujar línea gruesa (relleno)
            g2d.setColor(COLOR_RELLENO);
            g2d.setStroke(new BasicStroke(GROSOR_LINEA, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(inicio.x, inicio.y, fin.x, fin.y);
            
            // Dibujar contorno
            g2d.setColor(COLOR_CONTORNO);
            g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawLine(inicio.x, inicio.y, fin.x, fin.y);
            
            // Dibujar marcadores en puntos intermedios (excepto el último para evitar duplicados)
            if (i < tramos.size() - 2) {
                dibujarMarcador(g2d, fin, COLOR_CONTORNO, TAMANO_MARCADOR);
            }
        }
        
        // Dibujar marcadores en el primer y último punto
        Point primerPunto = transformation.transformCoordsToScreen(tramos.get(0));
        Point ultimoPunto = transformation.transformCoordsToScreen(tramos.get(tramos.size() - 1));
        dibujarMarcador(g2d, primerPunto, COLOR_CONTORNO, TAMANO_MARCADOR);
        dibujarMarcador(g2d, ultimoPunto, COLOR_CONTORNO, TAMANO_MARCADOR);
        
        // Mostrar información detallada del bloqueo en el punto medio
        if (tramos.size() > 1) {
            mostrarEtiqueta(g2d, bloqueo, tramos, transformation);
        }
    }
    
    /**
     * Dibuja la etiqueta informativa del bloqueo
     */
    private void mostrarEtiqueta(Graphics2D g2d, Bloqueo bloqueo, List<Posicion> tramos, MapTransformation transformation) {
        // Usar el punto medio para la etiqueta
        int puntoMedioIndex = tramos.size() / 2;
        Point puntoMedio = transformation.transformCoordsToScreen(tramos.get(puntoMedioIndex));
        
        // Configuración de la fuente para mejor legibilidad
        Font fuenteOriginal = g2d.getFont();
        Font fuenteEtiqueta = new Font(fuenteOriginal.getName(), Font.BOLD, fuenteOriginal.getSize());
        g2d.setFont(fuenteEtiqueta);
        
        // Preparar textos para la etiqueta con fechas y horas detalladas
        String horaInicioTexto = bloqueo.getHoraInicio().format(HORA_FORMATTER);
        String horaFinTexto = bloqueo.getHoraFin().format(HORA_FORMATTER);
        
        // Calcular duración
        long horas = ChronoUnit.HOURS.between(bloqueo.getHoraInicio(), bloqueo.getHoraFin());
        long dias = ChronoUnit.DAYS.between(bloqueo.getHoraInicio().toLocalDate(), 
                                          bloqueo.getHoraFin().toLocalDate());
        
        String duracionTexto;
        if (dias > 0) {
            duracionTexto = dias + "d " + (horas % 24) + "h";
        } else {
            duracionTexto = horas + "h";
        }
        
        // Texto completo de la etiqueta
        String textoEtiqueta = TEXTO_ESTADO + " (" + horaInicioTexto + " - " + horaFinTexto + ", " + duracionTexto + ")";
        
        // Calcular el tamaño del texto para posicionar correctamente
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = fuenteEtiqueta.getStringBounds(textoEtiqueta, frc);
        int anchoTexto = (int) bounds.getWidth();
        int altoTexto = (int) bounds.getHeight();
        
        // Dibujar fondo translúcido para el texto
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRect(puntoMedio.x - 5, puntoMedio.y - altoTexto - 5, anchoTexto + 10, altoTexto + 10);
        
        // Dibujar borde alrededor del fondo
        g2d.setColor(COLOR_CONTORNO);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRect(puntoMedio.x - 5, puntoMedio.y - altoTexto - 5, anchoTexto + 10, altoTexto + 10);
        
        // Dibujar el texto de la etiqueta
        g2d.setColor(COLOR_TEXTO);
        g2d.drawString(textoEtiqueta, puntoMedio.x, puntoMedio.y - 10);
        
        // Restaurar la fuente original
        g2d.setFont(fuenteOriginal);
    }
    
    /**
     * Dibuja un marcador en el punto de bloqueo
     */
    private void dibujarMarcador(Graphics2D g2d, Point punto, Color color, int tamano) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(1.8f));
        
        // Dibujar un círculo relleno con una X en medio
        g2d.fillOval(punto.x - tamano, punto.y - tamano, tamano * 2, tamano * 2);
        
        // Dibujar la X en blanco para que resalte
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawLine(punto.x - tamano/2, punto.y - tamano/2, punto.x + tamano/2, punto.y + tamano/2);
        g2d.drawLine(punto.x - tamano/2, punto.y + tamano/2, punto.x + tamano/2, punto.y - tamano/2);
    }
}
