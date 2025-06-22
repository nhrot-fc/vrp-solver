package com.vroute.alns.operators;

import com.vroute.models.Environment;
import com.vroute.solution.Solution;

/**
 * Interface for destruction operators in ALNS.
 * Destruction operators remove parts of a solution to allow reconstruction.
 */
public interface DestroyOperator {
    /**
     * Destroys (removes) parts of the solution.
     * 
     * @param solution The current solution
     * @param environment The current environment state
     * @param removalCount The number of elements to remove
     * @return A partially destroyed solution
     */
    Solution destroy(Solution solution, Environment environment, int removalCount);
    
    /**
     * Get the name of the operator for logging and adaptive weight adjustment.
     * 
     * @return The operator name
     */
    String getName();
}
