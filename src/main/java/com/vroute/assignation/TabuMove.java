package com.vroute.assignation;

import com.vroute.models.Order;
import com.vroute.models.Vehicle;

/**
 * Representa un movimiento en el contexto del algoritmo Tabu Search.
 * Un movimiento puede ser:
 * - Mover una entrega de un vehículo a otro
 * - Intercambiar entregas entre vehículos
 * - Etc.
 */
public class TabuMove {
    private final Vehicle sourceVehicle;
    private final Vehicle targetVehicle;
    private final Order order;
    private final MoveType moveType;
    private final int deliveryAmount;

    public TabuMove(Vehicle sourceVehicle, Vehicle targetVehicle, Order order,
            MoveType moveType, int deliveryAmount) {
        this.sourceVehicle = sourceVehicle;
        this.targetVehicle = targetVehicle;
        this.order = order;
        this.moveType = moveType;
        this.deliveryAmount = deliveryAmount;
    }

    public Vehicle getSourceVehicle() {
        return sourceVehicle;
    }

    public Vehicle getTargetVehicle() {
        return targetVehicle;
    }

    public Order getOrder() {
        return order;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public int getDeliveryAmount() {
        return deliveryAmount;
    }

    /**
     * Devuelve el atributo inverso del movimiento para ser almacenado en la lista
     * tabú.
     * Esto previene que se deshaga inmediatamente el movimiento.
     */
    public TabuMove getInverseMove() {
        return new TabuMove(
                targetVehicle,
                sourceVehicle,
                order,
                moveType,
                deliveryAmount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        TabuMove other = (TabuMove) obj;
        return sourceVehicle.getId().equals(other.sourceVehicle.getId()) &&
                targetVehicle.getId().equals(other.targetVehicle.getId()) &&
                order.getId().equals(other.order.getId()) &&
                moveType == other.moveType &&
                deliveryAmount == other.deliveryAmount;
    }

    @Override
    public int hashCode() {
        int result = sourceVehicle.getId().hashCode();
        result = 31 * result + targetVehicle.getId().hashCode();
        result = 31 * result + order.getId().hashCode();
        result = 31 * result + moveType.hashCode();
        result = 31 * result + deliveryAmount;
        return result;
    }
}
