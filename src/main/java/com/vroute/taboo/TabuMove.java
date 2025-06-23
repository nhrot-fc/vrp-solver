package com.vroute.taboo;

import com.vroute.solution.Solution;

/**
 * Interface for all tabu search moves
 */
public interface TabuMove {
    
    /**
     * Apply this move to the given solution
     * @param solution Solution to modify
     * @return true if the move was applied successfully, false otherwise
     */
    boolean apply(Solution solution);
    
    /**
     * Get a hash key that uniquely identifies this move for tabu list purposes
     * @return String key representing the move
     */
    String getTabuKey();
} 