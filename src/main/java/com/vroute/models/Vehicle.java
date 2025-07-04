package com.vroute.models;

import java.time.LocalDateTime;

public class Vehicle {
    // unmutable attributes
    private final String id;
    private final VehicleType type;
    private final int glpCapacityM3;
    private final double fuelCapacityGal;

    // mutable attributes
    private Position currentPosition;
    private int currentGlpM3;
    private double currentFuelGal;
    private VehicleStatus status;

    public Vehicle(String id, VehicleType type, Position currentPosition) {
        this.id = id;
        this.type = type;
        this.glpCapacityM3 = type.getCapacityM3();
        this.fuelCapacityGal = Constants.VEHICLE_FUEL_CAPACITY_GAL;
        this.currentPosition = currentPosition;
        this.currentGlpM3 = 0;
        this.currentFuelGal = this.fuelCapacityGal;
        this.status = VehicleStatus.AVAILABLE;
    }

    // Getters
    public String getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public int getGlpCapacityM3() {
        return glpCapacityM3;
    }

    public double getFuelCapacityGal() {
        return fuelCapacityGal;
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public int getCurrentGlpM3() {
        return currentGlpM3;
    }

    public double getCurrentFuelGal() {
        return currentFuelGal;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    // Setters
    public void setCurrentPosition(Position position) {
        this.currentPosition = position;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    // Operations
    public void consumeFuel(double distanceKm) {
        double combinedWeight = this.type.convertGlpM3ToTon(this.currentGlpM3) + this.type.getTareWeightTon();
        double fuelConsumedGallons = Math.abs((distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR);
        this.currentFuelGal = Math.max(0, this.currentFuelGal - fuelConsumedGallons);
    }

    public double calculateFuelNeeded(double distanceKm) {
        double combinedWeight = this.type.convertGlpM3ToTon(this.currentGlpM3)
                + this.type.getTareWeightTon();
        return Math.abs((distanceKm * combinedWeight) / Constants.CONSUMPTION_FACTOR);
    }

    public void refuel() {
        this.currentFuelGal = this.fuelCapacityGal;
    }

    public void dispenseGlp(int glpVolumeM3) {
        this.currentGlpM3 = Math.max(0, this.currentGlpM3 - Math.abs(glpVolumeM3));
    }

    public boolean canDispenseGLP(int glpVolumeM3) {
        return this.currentGlpM3 >= Math.abs(glpVolumeM3);
    }

    public void refill(int glpVolumeM3) {
        this.currentGlpM3 = Math.min(this.glpCapacityM3, this.currentGlpM3 + Math.abs(glpVolumeM3));
    }

    public void serveOrder(Order order, int glpVolumeM3, LocalDateTime serveDate) {
        int absoluteVolume = Math.abs(glpVolumeM3);
        this.dispenseGlp(absoluteVolume);
        order.recordDelivery(absoluteVolume, this.id, serveDate);
    }

    // Clone
    public Vehicle clone() {
        Vehicle clonedVehicle = new Vehicle(this.id, this.type, this.currentPosition.clone());
        clonedVehicle.currentGlpM3 = this.currentGlpM3;
        clonedVehicle.currentFuelGal = this.currentFuelGal;
        clonedVehicle.status = this.status;
        return clonedVehicle;
    }

    @Override
    public String toString() {
        return String.format("🚛 %s-%s %s [🛢️ %d/%d m³][⛽ %.1f/%.1f gal] %s",
                type.name(),
                id,
                status.getIcon(),
                currentGlpM3,
                glpCapacityM3,
                currentFuelGal,
                fuelCapacityGal,
                currentPosition.toString());
    }
}
