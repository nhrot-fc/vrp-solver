package com.vroute.models;

import java.util.Objects;

public class Posicion {
    private int x;
    private int y;

    public Posicion(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Posicion posicion = (Posicion) o;
        return x == posicion.x && y == posicion.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Posicion{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public double distancia(Posicion otra) {
        return Math.abs(this.x - otra.x) + Math.abs(this.y - otra.y);
    }
}
