package com.vroute.assignation;

import com.vroute.models.Vehicle;

public class TabuMove {
    private final Vehicle sourceVehicle;
    private final int sourceInstructionIndex;
    private final Vehicle targetVehicle;
    private final int targetInstructionIndex;
    private final MoveType moveType;

    public enum MoveType {
        TRANSFER,
        SWAP,
        REORDER
    }

    public TabuMove(Vehicle sourceVehicle, int sourceInstructionIndex, Vehicle targetVehicle) {
        this.sourceVehicle = sourceVehicle;
        this.sourceInstructionIndex = sourceInstructionIndex;
        this.targetVehicle = targetVehicle;
        this.targetInstructionIndex = -1;
        this.moveType = MoveType.TRANSFER;
    }

    public TabuMove(Vehicle sourceVehicle, int sourceInstructionIndex, 
                    Vehicle targetVehicle, int targetInstructionIndex) {
        this.sourceVehicle = sourceVehicle;
        this.sourceInstructionIndex = sourceInstructionIndex;
        this.targetVehicle = targetVehicle;
        this.targetInstructionIndex = targetInstructionIndex;
        this.moveType = MoveType.SWAP;
    }

    public TabuMove(Vehicle vehicle, int sourceInstructionIndex, int targetInstructionIndex) {
        this.sourceVehicle = vehicle;
        this.targetVehicle = vehicle;
        this.sourceInstructionIndex = sourceInstructionIndex;
        this.targetInstructionIndex = targetInstructionIndex;
        this.moveType = MoveType.REORDER;
    }

    public Vehicle getSourceVehicle() {
        return sourceVehicle;
    }

    public int getSourceInstructionIndex() {
        return sourceInstructionIndex;
    }

    public Vehicle getTargetVehicle() {
        return targetVehicle;
    }

    public int getTargetInstructionIndex() {
        return targetInstructionIndex;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TabuMove tabuMove = (TabuMove) obj;
        
        return sourceVehicle.getId().equals(tabuMove.sourceVehicle.getId()) &&
               sourceInstructionIndex == tabuMove.sourceInstructionIndex &&
               targetVehicle.getId().equals(tabuMove.targetVehicle.getId()) &&
               targetInstructionIndex == tabuMove.targetInstructionIndex &&
               moveType == tabuMove.moveType;
    }

    @Override
    public int hashCode() {
        int result = sourceVehicle.getId().hashCode();
        result = 31 * result + sourceInstructionIndex;
        result = 31 * result + targetVehicle.getId().hashCode();
        result = 31 * result + targetInstructionIndex;
        result = 31 * result + moveType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        switch (moveType) {
            case TRANSFER:
                return String.format("TRANSFER: Vehicle %s, Instruction %d -> Vehicle %s", 
                        sourceVehicle.getId(), sourceInstructionIndex, targetVehicle.getId());
            case SWAP:
                return String.format("SWAP: Vehicle %s, Instruction %d <-> Vehicle %s, Instruction %d", 
                        sourceVehicle.getId(), sourceInstructionIndex, 
                        targetVehicle.getId(), targetInstructionIndex);
            case REORDER:
                return String.format("REORDER: Vehicle %s, Instruction %d -> Position %d", 
                        sourceVehicle.getId(), sourceInstructionIndex, targetInstructionIndex);
            default:
                return "Unknown move type";
        }
    }
}
