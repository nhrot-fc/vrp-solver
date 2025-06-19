package com.vroute.orchest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks and calculates statistics during the simulation.
 */
public class SimulationStats {
    // Time tracking
    private LocalDateTime simulationStartTime;
    private LocalDateTime simulationEndTime;
    private long realExecutionTimeMillis;
    
    // Order metrics
    private int totalOrders;
    private int deliveredOrders;
    private int lateDeliveries;
    
    // Vehicle metrics
    private double totalDistanceTraveled;
    private double totalFuelConsumed;
    private int totalVehicleBreakdowns;
    private int totalMaintenanceEvents;
    
    // Blockage metrics
    private int totalBlockages;
    private Duration totalBlockageDuration;
    
    // Algorithm metrics
    private int totalReplans;
    private long totalPlanningTimeMillis;
    
    // Detailed stats by vehicle
    private Map<String, VehicleStats> vehicleStatsMap = new HashMap<>();
    
    // Inner class to track per-vehicle statistics
    public static class VehicleStats {
        private String vehicleId;
        private int deliveries;
        private double distanceTraveled;
        private double fuelConsumed;
        private int breakdowns;
        private Duration operationalTime;
        private Duration idleTime;
        
        public VehicleStats(String vehicleId) {
            this.vehicleId = vehicleId;
            this.deliveries = 0;
            this.distanceTraveled = 0;
            this.fuelConsumed = 0;
            this.breakdowns = 0;
            this.operationalTime = Duration.ZERO;
            this.idleTime = Duration.ZERO;
        }
        
        // Getters and increment methods
        public String getVehicleId() {
            return vehicleId;
        }
        
        public int getDeliveries() {
            return deliveries;
        }
        
        public void incrementDeliveries() {
            this.deliveries++;
        }
        
        public double getDistanceTraveled() {
            return distanceTraveled;
        }
        
        public void addDistanceTraveled(double distance) {
            this.distanceTraveled += distance;
        }
        
        public double getFuelConsumed() {
            return fuelConsumed;
        }
        
        public void addFuelConsumed(double fuel) {
            this.fuelConsumed += fuel;
        }
        
        public int getBreakdowns() {
            return breakdowns;
        }
        
        public void incrementBreakdowns() {
            this.breakdowns++;
        }
        
        public Duration getOperationalTime() {
            return operationalTime;
        }
        
        public void addOperationalTime(Duration time) {
            this.operationalTime = this.operationalTime.plus(time);
        }
        
        public Duration getIdleTime() {
            return idleTime;
        }
        
        public void addIdleTime(Duration time) {
            this.idleTime = this.idleTime.plus(time);
        }
        
        public double getEfficiency() {
            long totalMinutes = operationalTime.plus(idleTime).toMinutes();
            return totalMinutes > 0 ? 
                   operationalTime.toMinutes() / (double)totalMinutes : 0;
        }
    }
    
    public SimulationStats() {
        resetStats();
    }
    
    public void resetStats() {
        simulationStartTime = null;
        simulationEndTime = null;
        realExecutionTimeMillis = 0;
        
        totalOrders = 0;
        deliveredOrders = 0;
        lateDeliveries = 0;
        
        totalDistanceTraveled = 0;
        totalFuelConsumed = 0;
        totalVehicleBreakdowns = 0;
        totalMaintenanceEvents = 0;
        
        totalBlockages = 0;
        totalBlockageDuration = Duration.ZERO;
        
        totalReplans = 0;
        totalPlanningTimeMillis = 0;
        
        vehicleStatsMap.clear();
    }
    
    public void startSimulation(LocalDateTime startTime) {
        this.simulationStartTime = startTime;
    }
    
    public void endSimulation(LocalDateTime endTime) {
        this.simulationEndTime = endTime;
    }
    
    public void setRealExecutionTimeMillis(long timeMillis) {
        this.realExecutionTimeMillis = timeMillis;
    }
    
    public void recordNewOrder() {
        this.totalOrders++;
    }
    
    public void recordDeliveredOrder(boolean isLate) {
        this.deliveredOrders++;
        if (isLate) {
            this.lateDeliveries++;
        }
    }
    
    public void recordVehicleOperation(String vehicleId, double distance, double fuel, Duration operationalTime) {
        VehicleStats stats = getOrCreateVehicleStats(vehicleId);
        stats.addDistanceTraveled(distance);
        stats.addFuelConsumed(fuel);
        stats.addOperationalTime(operationalTime);
        
        this.totalDistanceTraveled += distance;
        this.totalFuelConsumed += fuel;
    }
    
    public void recordVehicleIdle(String vehicleId, Duration idleTime) {
        VehicleStats stats = getOrCreateVehicleStats(vehicleId);
        stats.addIdleTime(idleTime);
    }
    
    public void recordVehicleDelivery(String vehicleId) {
        VehicleStats stats = getOrCreateVehicleStats(vehicleId);
        stats.incrementDeliveries();
    }
    
    public void recordVehicleBreakdown(String vehicleId) {
        VehicleStats stats = getOrCreateVehicleStats(vehicleId);
        stats.incrementBreakdowns();
        this.totalVehicleBreakdowns++;
    }
    
    public void recordMaintenanceEvent() {
        this.totalMaintenanceEvents++;
    }
    
    public void incrementTotalDeliveries(int count) {
        this.deliveredOrders += count;
    }
    
    public void recordBlockage(Duration duration) {
        this.totalBlockages++;
        this.totalBlockageDuration = this.totalBlockageDuration.plus(duration);
    }
    
    public void incrementTotalReplans() {
        this.totalReplans++;
    }
    
    public void recordReplan(long planningTimeMillis) {
        this.totalReplans++;
        this.totalPlanningTimeMillis += planningTimeMillis;
    }
    
    private VehicleStats getOrCreateVehicleStats(String vehicleId) {
        return vehicleStatsMap.computeIfAbsent(vehicleId, VehicleStats::new);
    }
    
    // Getters for all statistics
    public LocalDateTime getSimulationStartTime() {
        return simulationStartTime;
    }

    public LocalDateTime getSimulationEndTime() {
        return simulationEndTime;
    }
    
    public Duration getSimulationDuration() {
        if (simulationStartTime != null && simulationEndTime != null) {
            return Duration.between(simulationStartTime, simulationEndTime);
        }
        return Duration.ZERO;
    }

    public long getRealExecutionTimeMillis() {
        return realExecutionTimeMillis;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public int getDeliveredOrders() {
        return deliveredOrders;
    }
    
    public int getPendingOrders() {
        return totalOrders - deliveredOrders;
    }

    public int getLateDeliveries() {
        return lateDeliveries;
    }
    
    public double getOnTimeDeliveryRate() {
        return deliveredOrders > 0 ? 
               (deliveredOrders - lateDeliveries) / (double)deliveredOrders : 0;
    }

    public double getTotalDistanceTraveled() {
        return totalDistanceTraveled;
    }

    public double getTotalFuelConsumed() {
        return totalFuelConsumed;
    }
    
    public double getAverageFuelEfficiency() {
        return totalDistanceTraveled > 0 ? 
               totalDistanceTraveled / totalFuelConsumed : 0;
    }

    public int getTotalVehicleBreakdowns() {
        return totalVehicleBreakdowns;
    }

    public int getTotalMaintenanceEvents() {
        return totalMaintenanceEvents;
    }

    public int getTotalBlockages() {
        return totalBlockages;
    }

    public Duration getTotalBlockageDuration() {
        return totalBlockageDuration;
    }

    public int getTotalReplans() {
        return totalReplans;
    }

    public long getTotalPlanningTimeMillis() {
        return totalPlanningTimeMillis;
    }
    
    public double getAveragePlanningTimeMillis() {
        return totalReplans > 0 ? 
               totalPlanningTimeMillis / (double)totalReplans : 0;
    }
    
    public Map<String, VehicleStats> getVehicleStats() {
        return new HashMap<>(vehicleStatsMap);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Simulation Statistics =====\n");
        
        Duration simDuration = getSimulationDuration();
        sb.append(String.format("Simulation time: %d days, %d hours, %d minutes\n", 
                  simDuration.toDays(), 
                  simDuration.toHoursPart(), 
                  simDuration.toMinutesPart()));
        
        sb.append(String.format("Real execution time: %.2f seconds\n", 
                  realExecutionTimeMillis / 1000.0));
        
        sb.append(String.format("Orders: %d total, %d delivered (%.1f%%), %d late (%.1f%%)\n",
                  totalOrders, 
                  deliveredOrders, 
                  totalOrders > 0 ? (deliveredOrders * 100.0 / totalOrders) : 0,
                  lateDeliveries,
                  deliveredOrders > 0 ? (lateDeliveries * 100.0 / deliveredOrders) : 0));
        
        sb.append(String.format("Distance traveled: %.2f km, Fuel consumed: %.2f gallons\n",
                  totalDistanceTraveled, totalFuelConsumed));
        
        sb.append(String.format("Fuel efficiency: %.2f km/gallon\n",
                  getAverageFuelEfficiency()));
        
        sb.append(String.format("Breakdowns: %d, Maintenance events: %d\n",
                  totalVehicleBreakdowns, totalMaintenanceEvents));
        
        sb.append(String.format("Blockages: %d, Total blockage time: %s\n",
                  totalBlockages, formatDuration(totalBlockageDuration)));
        
        sb.append(String.format("Replans: %d, Average planning time: %.2f ms\n",
                  totalReplans, getAveragePlanningTimeMillis()));
        
        return sb.toString();
    }
    
    private String formatDuration(Duration duration) {
        return String.format("%dd %dh %dm", 
                  duration.toDays(), 
                  duration.toHoursPart(), 
                  duration.toMinutesPart());
    }
}
