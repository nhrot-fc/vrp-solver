package com.vroute.exceptions;

/**
 * Exception thrown when a path between two positions cannot be found.
 */
public class NoPathFoundException extends Exception {
    public NoPathFoundException(String message) {
        super(message);
    }
}
