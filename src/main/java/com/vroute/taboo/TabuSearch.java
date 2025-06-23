package com.vroute.taboo;

import java.util.List;

import com.vroute.models.Environment;
import com.vroute.solution.Evaluator;
import com.vroute.solution.SIHSolver;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;

/**
 * Implementation of the Tabu Search algorithm for the V-Route problem.
 */
public class TabuSearch implements Solver {
    // Algorithm parameters
    private final int MAX_ITERATIONS = 1000;
    private final int TABU_TENURE = 10;
    private final int MAX_NEIGHBORHOOD_SIZE = 50;
    private final double ASPIRATION_FACTOR = 0.95;
    private final int MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;

    @Override
    public Solution solve(Environment env) {
        return null;
    }
}