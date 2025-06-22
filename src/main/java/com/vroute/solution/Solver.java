package com.vroute.solution;

import com.vroute.models.Environment;

/**
 * Interface for all solution algorithms in the V-Route system.
 */
public interface Solver {
    
    /**
     * Solves the vehicle routing problem using the specific algorithm implementation.
     * 
     * @param environment The environment containing all problem data (vehicles, depots, orders, etc.)
     * @return A solution with routes for the problem
     */
    Solution solve(Environment environment);
} 