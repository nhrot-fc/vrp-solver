package com.vroute.taboo;

import com.vroute.solution.Solution;

/**
 * Interface representing a generic tabu move in the solution space.
 */
public interface TabuMove {
    /**
     * Apply this move to the given solution.
     * 
     * @param solution The solution to modify
     * @return The modified solution after applying the move
     */
    Solution apply(Solution solution);
    
    /**
     * Get a key that uniquely identifies this move for tabu list storage.
     * 
     * @return A string representation of the move for tabu list
     */
    String getTabuKey();
} 