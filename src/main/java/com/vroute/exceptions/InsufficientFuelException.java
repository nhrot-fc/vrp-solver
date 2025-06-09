package com.vroute.exceptions;

/**
 * Exception thrown when a vehicle has insufficient fuel to complete an operation.
 */
public class InsufficientFuelException extends Exception {
    public InsufficientFuelException(String message) {
        super(message);
    }
}
