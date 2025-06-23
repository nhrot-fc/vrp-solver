package com.vroute.taboo;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of recent moves that are forbidden (tabu)
 */
public class TabuList {
    private final Map<String, Integer> tabuMoves;
    private final int tabuTenure;
    
    /**
     * Create a new tabu list with specified tabu tenure
     * @param tabuTenure number of iterations a move remains tabu
     */
    public TabuList(int tabuTenure) {
        this.tabuMoves = new HashMap<>();
        this.tabuTenure = tabuTenure;
    }
    
    /**
     * Check if a move is tabu
     * @param move The move to check
     * @return true if the move is tabu, false otherwise
     */
    public boolean isTabu(TabuMove move) {
        return tabuMoves.containsKey(move.getTabuKey());
    }
    
    /**
     * Add a move to the tabu list
     * @param move The move to make tabu
     * @param currentIteration The current iteration number
     */
    public void addMove(TabuMove move, int currentIteration) {
        tabuMoves.put(move.getTabuKey(), currentIteration + tabuTenure);
    }
    
    /**
     * Update the tabu list by removing expired tabu moves
     * @param currentIteration The current iteration number
     */
    public void updateTabuList(int currentIteration) {
        tabuMoves.entrySet().removeIf(entry -> entry.getValue() <= currentIteration);
    }
} 