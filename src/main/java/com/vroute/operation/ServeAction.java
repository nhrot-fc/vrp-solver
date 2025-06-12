package com.vroute.operation;

import java.time.LocalDateTime;
import com.vroute.models.Order;
import com.vroute.models.Stop;
import com.vroute.models.Vehicle;

public class ServeAction extends Action {
    private final Order order;
    private final int glpDischargedM3;

    public ServeAction(LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicleState, Order order, int glpDischargedM3) {
        super(ActionType.SERVE, startTime, endTime, vehicleState);
        this.order = order;
        this.glpDischargedM3 = glpDischargedM3;
    }

    public int getGlpDischargedM3() {
        return glpDischargedM3;
    }

    public Order getOrder() {
        return order;
    }

    @Override
    public Stop getDestination() {
        return order;
    }

    @Override
    public String getDescription() {
        return String.format("Entregar %d m³ de GLP en %s", glpDischargedM3, order.toString());
    }
}
