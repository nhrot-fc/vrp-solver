package com.vroute.taboo;

import com.vroute.models.Environment;
import com.vroute.solution.Solution;

public interface TabuMove {
    public enum MoveType {
        REARRANGE,
        SWAP_VEHICLE,
        SWAP_STOP
    }

    Solution apply(Environment env, Solution solution);
}
