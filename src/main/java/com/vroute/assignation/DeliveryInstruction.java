package com.vroute.assignation;

import com.vroute.models.Order;
import com.vroute.models.Position;
import java.time.LocalDateTime;

public class DeliveryInstruction {
    private final Order originalOrder;
    private final int glpAmountToDeliver;

    public DeliveryInstruction(Order originalOrder, int glpAmountToDeliver) {
        this.originalOrder = originalOrder;
        this.glpAmountToDeliver = glpAmountToDeliver;
    }

    public Order getOriginalOrder() {
        return originalOrder;
    }

    public String getOrderId() {
        return originalOrder.getId();
    }

    public Position getCustomerPosition() {
        return originalOrder.getPosition();
    }

    public LocalDateTime getDueDate() {
        return originalOrder.getDueDate();
    }

    public int getGlpAmountToDeliver() {
        return glpAmountToDeliver;
    }
}
