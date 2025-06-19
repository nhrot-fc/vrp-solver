package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Esta clase se encarga de evaluar la calidad de una solución asignación
 * basándose en criterios como entrega completa de órdenes, cumplimiento de plazos
 * y minimización de distancias.
 */
public class SolutionEvaluator {
    
    // Recompensas y penalizaciones para el score
    private static final double ORDER_DELIVERED_REWARD = 1000.0;      // Recompensa base por cada orden entregada
    private static final double ON_TIME_DELIVERY_BONUS = 500.0;       // Bonus adicional por entrega a tiempo
    private static final double EARLY_DELIVERY_BONUS_PER_MINUTE = 1.0; // Bonus adicional por cada minuto de anticipación (hasta un máximo)
    private static final int MAX_EARLY_BONUS_MINUTES = 30;            // Límite de minutos para bonus por anticipación
    
    // Penalizaciones
    private static final double INCOMPLETE_ORDER_PENALTY = 2000.0;    // Penalización por orden incompleta
    private static final double LATE_DELIVERY_PENALTY_PER_MINUTE = 10.0; // Penalización por minuto de retraso
    private static final double LATE_PENALTY_EXPONENT = 1.5;          // Exponente para penalización por retraso
    private static final double DISTANCE_PENALTY_PER_KM = 0.5;        // Pequeña penalización por km recorrido
    
    // Constructor privado para evitar instanciación
    private SolutionEvaluator() {}
    
    /**
     * Evalúa una solución asignando un score (mayor es mejor)
     * @param solution La solución a evaluar
     * @param environment El entorno con información de órdenes y vehículos
     * @return Un score donde mayor valor indica mejor solución
     */
    public static double evaluateSolution(Solution solution, Environment environment) {
        // Obtener información de órdenes pendientes y asignaciones una sola vez
        Map<String, Order> pendingOrdersMap = getPendingOrdersMap(environment);
        Map<String, Integer> assignedGlpByOrderId = getAssignedGlpByOrderId(solution);
        
        double reward = calculateCompletedOrdersReward(pendingOrdersMap, assignedGlpByOrderId);
        double timeBonus = calculateTimeBonus(solution, environment);
        double incompletePenalty = calculateIncompletePenalty(pendingOrdersMap, assignedGlpByOrderId);
        double distancePenalty = calculateDistancePenalty(solution);
        
        double totalScore = reward + timeBonus - incompletePenalty - distancePenalty;
        
        // Verificar si hay órdenes pendientes no atendidas
        Set<String> pendingOrdersNotCovered = checkForMissingOrders(pendingOrdersMap.keySet(), assignedGlpByOrderId.keySet());
        if (!pendingOrdersNotCovered.isEmpty()) {
            System.err.printf("Warning: %d pending orders not covered in solution: %s%n", 
                    pendingOrdersNotCovered.size(), pendingOrdersNotCovered);
            totalScore -= pendingOrdersNotCovered.size() * INCOMPLETE_ORDER_PENALTY * 2; // Doble penalización
        }
        
        return totalScore;
    }
    
    /**
     * Obtiene un mapa de todas las órdenes pendientes por ID
     */
    private static Map<String, Order> getPendingOrdersMap(Environment environment) {
        Map<String, Order> pendingOrdersMap = new HashMap<>();
        for (Order order : environment.getPendingOrders()) {
            pendingOrdersMap.put(order.getId(), order);
        }
        return pendingOrdersMap;
    }
    
    /**
     * Obtiene un mapa con la cantidad de GLP asignada por ID de orden
     */
    private static Map<String, Integer> getAssignedGlpByOrderId(Solution solution) {
        Map<String, Integer> assignedGlpByOrderId = new HashMap<>();
        Map<Vehicle, List<DeliveryInstruction>> assignments = solution.getVehicleOrderAssignments();
        
        for (List<DeliveryInstruction> instructions : assignments.values()) {
            for (DeliveryInstruction instruction : instructions) {
                String orderId = instruction.getOrderId();
                assignedGlpByOrderId.put(orderId, 
                    assignedGlpByOrderId.getOrDefault(orderId, 0) + instruction.getGlpAmountToDeliver());
            }
        }
        
        return assignedGlpByOrderId;
    }
    
    /**
     * Verifica si hay órdenes pendientes que no estén incluidas en la solución
     */
    private static Set<String> checkForMissingOrders(Set<String> pendingOrderIds, Set<String> assignedOrderIds) {
        Set<String> missingOrderIds = new HashSet<>(pendingOrderIds);
        missingOrderIds.removeAll(assignedOrderIds);
        return missingOrderIds;
    }
    
    /**
     * Calcula la recompensa base por órdenes completadas
     */
    private static double calculateCompletedOrdersReward(Map<String, Order> pendingOrdersMap, Map<String, Integer> assignedGlpByOrderId) {
        double reward = 0.0;
        
        for (Map.Entry<String, Order> entry : pendingOrdersMap.entrySet()) {
            String orderId = entry.getKey();
            Order order = entry.getValue();
            int required = order.getRemainingGlpM3();
            int assigned = assignedGlpByOrderId.getOrDefault(orderId, 0);
            
            if (assigned >= required) {
                reward += ORDER_DELIVERED_REWARD;
            } else if (assigned > 0) {
                // Recompensa parcial proporcional a la cantidad asignada
                double completionRatio = (double) assigned / required;
                reward += ORDER_DELIVERED_REWARD * completionRatio * 0.5; // Solo 50% de la recompensa si es parcial
            }
        }
        
        return reward;
    }
    
    /**
     * Calcula bonus por entrega a tiempo o anticipada
     */
    private static double calculateTimeBonus(Solution solution, Environment environment) {
        double totalBonus = 0.0;
        LocalDateTime now = environment.getCurrentTime();
        Map<Vehicle, List<DeliveryInstruction>> assignments = solution.getVehicleOrderAssignments();

        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : assignments.entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<DeliveryInstruction> instructions = entry.getValue();

            Position currentPosition = vehicle.getCurrentPosition();
            double travelTimeMinutes = 0.0;

            for (DeliveryInstruction instruction : instructions) {
                double distance = currentPosition.distanceTo(instruction.getCustomerPosition());
                double travelTimeForThisLeg = (distance / 60.0) * 60.0; // Convertir a minutos
                travelTimeMinutes += travelTimeForThisLeg;

                LocalDateTime estimatedArrival = now.plusMinutes((long) travelTimeMinutes);
                LocalDateTime dueDate = instruction.getDueDate();

                if (estimatedArrival.isBefore(dueDate) || estimatedArrival.isEqual(dueDate)) {
                    // Entrega a tiempo - otorgar bonus base
                    totalBonus += ON_TIME_DELIVERY_BONUS;
                    
                    // Bonus adicional por entrega anticipada
                    long minutesEarly = java.time.Duration.between(estimatedArrival, dueDate).toMinutes();
                    totalBonus += Math.min(minutesEarly, MAX_EARLY_BONUS_MINUTES) * EARLY_DELIVERY_BONUS_PER_MINUTE;
                } else {
                    // Entrega tardía - penalización
                    long minutesLate = java.time.Duration.between(dueDate, estimatedArrival).toMinutes();
                    double penalty = Math.pow(minutesLate, LATE_PENALTY_EXPONENT) * LATE_DELIVERY_PENALTY_PER_MINUTE;
                    totalBonus -= penalty; // Resta de la bonificación total
                }

                currentPosition = instruction.getCustomerPosition();
            }
        }

        return totalBonus;
    }
    
    /**
     * Calcula penalización por órdenes incompletas
     */
    private static double calculateIncompletePenalty(Map<String, Order> pendingOrdersMap, Map<String, Integer> assignedGlpByOrderId) {
        double penalty = 0.0;
        
        for (Map.Entry<String, Order> entry : pendingOrdersMap.entrySet()) {
            String orderId = entry.getKey();
            Order order = entry.getValue();
            int required = order.getRemainingGlpM3();
            int assigned = assignedGlpByOrderId.getOrDefault(orderId, 0);
            
            if (assigned < required) {
                // Penalización proporcional a la cantidad no asignada
                double missingRatio = (double)(required - assigned) / required;
                penalty += INCOMPLETE_ORDER_PENALTY * missingRatio * missingRatio; // Penalización cuadrática
            }
        }
        
        return penalty;
    }
    
    /**
     * Calcula penalización por distancia recorrida
     */
    private static double calculateDistancePenalty(Solution solution) {
        return solution.getTotalDistance() * DISTANCE_PENALTY_PER_KM;
    }
    
    /**
     * Obtiene un resumen detallado de la evaluación
     */
    public static String getDetailedEvaluation(Solution solution, Environment environment) {
        // Obtener información de órdenes pendientes y asignaciones una sola vez
        Map<String, Order> pendingOrdersMap = getPendingOrdersMap(environment);
        Map<String, Integer> assignedGlpByOrderId = getAssignedGlpByOrderId(solution);
        
        double reward = calculateCompletedOrdersReward(pendingOrdersMap, assignedGlpByOrderId);
        double timeBonus = calculateTimeBonus(solution, environment);
        double incompletePenalty = calculateIncompletePenalty(pendingOrdersMap, assignedGlpByOrderId);
        double distancePenalty = calculateDistancePenalty(solution);
        
        double totalScore = reward + timeBonus - incompletePenalty - distancePenalty;
        
        // Verificar órdenes perdidas
        Set<String> missingOrders = checkForMissingOrders(pendingOrdersMap.keySet(), assignedGlpByOrderId.keySet());
        double missingOrdersPenalty = !missingOrders.isEmpty() ? 
            missingOrders.size() * INCOMPLETE_ORDER_PENALTY * 2 : 0;
            
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Solution Evaluation (higher is better):\n"));
        sb.append(String.format("- Total pending orders: %d\n", pendingOrdersMap.size()));
        sb.append(String.format("- Total orders in solution: %d\n", assignedGlpByOrderId.size()));
        sb.append(String.format("- Delivery Coverage: %.1f%%\n", 
                calculateOrderCoveragePercentage(pendingOrdersMap, assignedGlpByOrderId)));
        sb.append(String.format("- Completed Orders Reward: +%.2f\n", reward));
        sb.append(String.format("- Time Delivery Bonus: +%.2f\n", timeBonus));
        sb.append(String.format("- Incomplete Orders Penalty: -%.2f\n", incompletePenalty));
        sb.append(String.format("- Distance Penalty (%.2f km): -%.2f\n", 
                solution.getTotalDistance(), distancePenalty));
                
        if (!missingOrders.isEmpty()) {
            sb.append(String.format("- Missing Orders Penalty (%d orders): -%.2f\n", 
                    missingOrders.size(), missingOrdersPenalty));
            sb.append(String.format("- Missing Order IDs: %s\n", missingOrders));
        }
        
        sb.append(String.format("- Total Score: %.2f\n", totalScore - missingOrdersPenalty));
        
        return sb.toString();
    }
    
    /**
     * Calcula el porcentaje de cobertura de órdenes
     */
    private static double calculateOrderCoveragePercentage(Map<String, Order> pendingOrdersMap, Map<String, Integer> assignedGlpByOrderId) {
        if (pendingOrdersMap.isEmpty()) {
            return 100.0; // No hay órdenes pendientes
        }
        
        int covered = 0;
        for (String orderId : assignedGlpByOrderId.keySet()) {
            if (pendingOrdersMap.containsKey(orderId)) {
                covered++;
            }
        }
        
        return (covered * 100.0) / pendingOrdersMap.size();
    }
} 