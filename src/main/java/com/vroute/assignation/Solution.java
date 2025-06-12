package com.vroute.assignation;

import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.operation.VehiclePlan;

import java.util.List;
import java.util.Map;

public class Solution {
    // El plan completo: un mapa que asocia cada vehículo con su ruta.
    private final Map<Vehicle, VehiclePlan> vehiclePlans;

    // Pedidos que, en esta solución, no pudieron ser atendidos.
    // En una solución final y válida, esta lista debe estar vacía.
    private final List<Order> unassignedOrders;

    // El costo total de la solución, es la suma de los costos de cada ruta
    // más las penalizaciones por restricciones no cumplidas (ej. pedidos no
    // asignados).
    // Este es el valor que Tabu Search intentará minimizar.
    private final double totalCost;

    public Solution(Map<Vehicle, VehiclePlan> plans, List<Order> unassignedOrders, double totalCost) {
        this.vehiclePlans = plans;
        this.unassignedOrders = unassignedOrders;
        this.totalCost = totalCost;
    }

    public Map<Vehicle, VehiclePlan> getVehiclePlans() {
        return vehiclePlans;
    }

    public List<Order> getUnassignedOrders() {
        return unassignedOrders;
    }

    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Verifica si la solución es factible, es decir, si cumple con todas las
     * restricciones.
     * En particular, una solución es factible si no hay pedidos sin asignar.
     */
    public boolean isFeasible() {
        return unassignedOrders.isEmpty();
    }
}