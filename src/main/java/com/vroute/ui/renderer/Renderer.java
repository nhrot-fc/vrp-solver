package com.vroute.ui.renderer;

import com.vroute.ui.utils.MapTransformation;
import java.awt.Graphics2D;

/**
 * Interfaz para todos los renderers de elementos en el mapa
 * @param <T> Tipo de objeto a renderizar
 */
public interface Renderer<T> {
    /**
     * Renderiza un elemento en el gráfico
     * @param g2d Contexto gráfico
     * @param elemento Elemento a renderizar
     * @param transformation Transformación del mapa
     */
    void renderizar(Graphics2D g2d, T elemento, MapTransformation transformation);
}
