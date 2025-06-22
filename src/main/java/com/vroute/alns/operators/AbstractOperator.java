package com.vroute.alns.operators;

/**
 * Base class for ALNS operators with common functionality.
 */
public abstract class AbstractOperator {
    private final String name;
    private double weight;
    private double score;
    private int usageCount;
    
    /**
     * Creates a new operator with the specified name.
     * 
     * @param name The operator name
     */
    public AbstractOperator(String name) {
        this.name = name;
        this.weight = 1.0; // Initial weight
        this.score = 0.0;
        this.usageCount = 0;
    }
    
    /**
     * Get the name of the operator.
     * 
     * @return The operator name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the current weight of the operator.
     * Used for the adaptive selection process.
     * 
     * @return The operator weight
     */
    public double getWeight() {
        return weight;
    }
    
    /**
     * Set the weight of the operator.
     * 
     * @param weight The new weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    /**
     * Get the current score of the operator.
     * 
     * @return The operator score
     */
    public double getScore() {
        return score;
    }
    
    /**
     * Add to the operator's score.
     * 
     * @param scoreIncrement The amount to add to the score
     */
    public void addScore(double scoreIncrement) {
        this.score += scoreIncrement;
    }
    
    /**
     * Reset the score to zero.
     */
    public void resetScore() {
        this.score = 0.0;
    }
    
    /**
     * Increment the usage count.
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }
    
    /**
     * Get the number of times this operator has been used.
     * 
     * @return The usage count
     */
    public int getUsageCount() {
        return usageCount;
    }
}
