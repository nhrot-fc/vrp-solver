package com.vroute.solution;

import java.time.LocalDateTime;
import com.vroute.models.Pedido;
import com.vroute.models.Deposito;

public class ParadaFactory {
    
    /**
     * Crea una parada de tipo Delivery con los valores actualizados
     */
    public static Delivery crearDelivery(Pedido pedido, double distanciaRecorrida, LocalDateTime eta) {
        return new Delivery(
            pedido, 
            pedido.getPosicion(), 
            calcularCantidadGLPEntregada(pedido), 
            distanciaRecorrida, 
            eta
        );
    }
    
    /**
     * Crea una parada de tipo Recarga con los valores actualizados
     */
    public static Recarga crearRecarga(Deposito deposito, int cantidadGLPRecargada, double distanciaRecorrida, LocalDateTime eta) {
        return new Recarga(
            deposito,
            cantidadGLPRecargada,
            distanciaRecorrida,
            eta
        );
    }
    
    /**
     * Actualiza una parada existente de tipo Delivery con nuevos valores
     */
    public static Delivery actualizarDelivery(Delivery delivery, double distanciaRecorrida, LocalDateTime eta) {
        return new Delivery(
            delivery.getPedido(),
            delivery.getPosicion(),
            delivery.getCantidadGLPEntregada(),
            distanciaRecorrida,
            eta
        );
    }
    
    /**
     * Actualiza una parada existente de tipo Recarga con nuevos valores
     */
    public static Recarga actualizarRecarga(Recarga recarga, double distanciaRecorrida, LocalDateTime eta) {
        return new Recarga(
            recarga.getDeposito(),
            recarga.getCantidadGLPRecargada(),
            distanciaRecorrida,
            eta
        );
    }
    
    private static int calcularCantidadGLPEntregada(Pedido pedido) {
        return pedido.getCantidadTotalGLP() - pedido.getCantidadSatisfechaGLP();
    }
} 