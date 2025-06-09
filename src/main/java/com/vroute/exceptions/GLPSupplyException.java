package com.vroute.exceptions;

/**
 * Exception thrown when GLP operations cannot be completed.
 */
public class GLPSupplyException extends Exception {
    public GLPSupplyException(String message) {
        super(message);
    }
}
