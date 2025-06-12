package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.operation.VehiclePlan;

/**
 * Clase encargada de evaluar una solución y calcular su costo total.
 * Implementada con métodos estáticos para facilitar su uso.
 */
public class SolutionEvaluator {
    
    private static final double ORDER_NOT_ASSIGNED_PENALTY = 1000.0;
    private static final double LATE_DELIVERY_PENALTY_MULTIPLIER = 2.0;
    private static final double VEHICLE_USAGE_COST = 100.0;
    private static final double FUEL_COST_PER_GALLON = 15.0;
    
    /**
     * Evalúa una solución, calculando su costo total.
     * @param solution La solución a evaluar
     * @param env El entorno actual para evaluar restricciones contextuales
     * @return La misma solución con el costo actualizado
     */
    public static Solution evaluate(Solution solution, Environment env) {
        double totalCost = 0.0;
        
        // Sumar el costo de todos los planes de vehículo
        for (VehiclePlan plan : solution.getVehiclePlans().values()) {
            // Costo base por usar el vehículo
            totalCost += VEHICLE_USAGE_COST;
            
            // Costo del plan (distancia, tiempo, etc.)
            totalCost += plan.getCost();
            
            // Costo de combustible (si está disponible en el plan)
            // Asumiendo que el costo es proporcional a la distancia
            totalCost += plan.getTotalDistanceKm() * FUEL_COST_PER_GALLON / 10;
        }
        
        // Añadir penalización por órdenes no asignadas
        for (Order order : solution.getUnassignedOrders()) {
            totalCost += calculatePenalty(order, env);
        }
        
        // Crear nueva solución con el costo actualizado
        return new Solution(
            solution.getVehiclePlans(), 
            solution.getUnassignedOrders(), 
            totalCost
        );
    }
    
    /**
     * Calcula la penalización por una orden no asignada.
     * @param order La orden no asignada
     * @param env El entorno actual para evaluar restricciones contextuales
     * @return El valor de la penalización
     */
    private static double calculatePenalty(Order order, Environment env) {
        double basePenalty = ORDER_NOT_ASSIGNED_PENALTY;
        
        // Si la orden ya está atrasada con respecto al tiempo actual,
        // aumentar la penalización
        if (order.isOverdue(env.getCurrentTime())) {
            basePenalty *= LATE_DELIVERY_PENALTY_MULTIPLIER;
        }
        
        // Ajuste por volumen de GLP solicitado
        // Órdenes más grandes tienen mayor penalización por no ser atendidas
        basePenalty *= (1.0 + (order.getRemainingGlpM3() / 10.0));
        
        return basePenalty;
    }
}
