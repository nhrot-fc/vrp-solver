package com.vroute.operation;

import java.time.LocalDateTime;
import com.vroute.models.Stop;
import com.vroute.models.Vehicle;

public class WaitAction extends Action {

    public WaitAction(LocalDateTime startTime, LocalDateTime endTime, Vehicle vehicleState) {
        super(ActionType.WAIT, startTime, endTime, vehicleState);
    }

    @Override
    public Stop getDestination() {
        return null;
    }
    @Override
    public String getDescription() {
        return String.format("Esperar en %s desde %s hasta %s", 
                null, 
                startTime.toString(), 
                endTime.toString());
    }
}
