package com.vroute.alns.operators;

import com.vroute.models.Environment;
import com.vroute.solution.Solution;

/**
 * Interface for repair operators in ALNS.
 * Repair operators rebuild parts of a solution that have been destroyed.
 */
public interface RepairOperator {
    /**
     * Repairs a partially destroyed solution.
     * 
     * @param solution The partially destroyed solution
     * @param environment The current environment state
     * @return A complete solution
     */
    Solution repair(Solution solution, Environment environment);
    
    /**
     * Get the name of the operator for logging and adaptive weight adjustment.
     * 
     * @return The operator name
     */
    String getName();
}
