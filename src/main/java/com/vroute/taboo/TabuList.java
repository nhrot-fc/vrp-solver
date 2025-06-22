package com.vroute.taboo;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the tabu list which stores the recently performed moves
 * to prevent cycling during the tabu search.
 */
public class TabuList {
    private final Map<String, Integer> tabuMap;
    private final int defaultTenure;
    
    /**
     * Creates a new tabu list with the default tenure.
     * 
     * @param defaultTenure The number of iterations a move remains tabu
     */
    public TabuList(int defaultTenure) {
        this.tabuMap = new HashMap<>();
        this.defaultTenure = defaultTenure;
    }
    
    /**
     * Add a move to the tabu list.
     * 
     * @param move The move to add
     * @param currentIteration Current iteration of the search
     */
    public void addMove(TabuMove move, int currentIteration) {
        String key = move.getTabuKey();
        tabuMap.put(key, currentIteration + defaultTenure);
    }
    
    /**
     * Add a move to the tabu list with a custom tenure.
     * 
     * @param move The move to add
     * @param currentIteration Current iteration of the search
     * @param tenure The number of iterations this move remains tabu
     */
    public void addMove(TabuMove move, int currentIteration, int tenure) {
        String key = move.getTabuKey();
        tabuMap.put(key, currentIteration + tenure);
    }
    
    /**
     * Check if a move is in the tabu list.
     * 
     * @param move The move to check
     * @param currentIteration Current iteration of the search
     * @return true if the move is tabu, false otherwise
     */
    public boolean isTabu(TabuMove move, int currentIteration) {
        String key = move.getTabuKey();
        Integer expiration = tabuMap.get(key);
        
        if (expiration == null) {
            return false;
        }
        
        return expiration > currentIteration;
    }
    
    /**
     * Updates the tabu list by removing expired entries.
     * 
     * @param currentIteration Current iteration of the search
     */
    public void update(int currentIteration) {
        tabuMap.entrySet().removeIf(entry -> entry.getValue() <= currentIteration);
    }
    
    /**
     * Clear the tabu list.
     */
    public void clear() {
        tabuMap.clear();
    }
    
    /**
     * Returns the size of the tabu list.
     * 
     * @return The number of moves currently in the tabu list
     */
    public int size() {
        return tabuMap.size();
    }
} 