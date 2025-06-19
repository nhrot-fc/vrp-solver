package com.vroute.solution.operations;

public interface RutaOperation {
    boolean execute();
    boolean undo();
    String getDescription();
} 