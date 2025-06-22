package com.vroute.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vroute.solution.Solution;

public class Environment {
    private final List<Vehicle> vehicles;
    private final Depot mainDepot;
    private final List<Depot> auxDepots;

    private LocalDateTime currentTime;
    private final List<Order> orderQueue;
    private final List<Blockage> activeBlockages;
    private final List<Incident> incidentRegistry;
    private final List<MaintenanceTask> maintenanceTasks;
    private List<Solution> currentSolution;

    public Environment(List<Vehicle> vehicles, Depot mainDepot, List<Depot> auxDepots,
            LocalDateTime referenceDateTime) {
        this.currentTime = referenceDateTime;
        this.vehicles = new ArrayList<>(vehicles);
        this.mainDepot = mainDepot;
        this.auxDepots = new ArrayList<>(auxDepots);
        this.orderQueue = new ArrayList<>();
        this.activeBlockages = new ArrayList<>();
        this.incidentRegistry = new ArrayList<>();
        this.maintenanceTasks = new ArrayList<>();
        this.currentSolution = new ArrayList<>();
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<Depot> getAuxDepots() {
        return Collections.unmodifiableList(auxDepots);
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public List<Order> getOrderQueue() {
        return Collections.unmodifiableList(orderQueue);
    }

    public List<Blockage> getActiveBlockages() {
        return Collections.unmodifiableList(activeBlockages);
    }

    public List<Incident> getIncidentRegistry() {
        return Collections.unmodifiableList(incidentRegistry);
    }

    public List<MaintenanceTask> getMaintenanceTasks() {
        return Collections.unmodifiableList(maintenanceTasks);
    }

    public List<Solution> getCurrentSolution() {
        return Collections.unmodifiableList(currentSolution);
    }

    public void setCurrentSolution(List<Solution> currentSolution) {
        this.currentSolution = new ArrayList<>(currentSolution);
    }

    public Depot getMainDepot() {
        return mainDepot;
    }

    public void setCurrentTime(LocalDateTime newTime) {
        this.currentTime = newTime;
    }

    public void addOrder(Order order) {
        orderQueue.add(order);
    }

    public void addOrders(List<Order> orders) {
        orderQueue.addAll(orders);
    }

    public int removeDeliveredOrders() {
        int initialSize = orderQueue.size();
        orderQueue.removeIf(Order::isDelivered);
        return initialSize - orderQueue.size();
    }

    public List<Order> getPendingOrders() {
        return orderQueue.stream()
                .filter(order -> !order.isDelivered())
                .collect(Collectors.toList());
    }

    public List<Order> getOverdueOrders() {
        return orderQueue.stream()
                .filter(order -> !order.isDelivered() && order.isOverdue(currentTime))
                .collect(Collectors.toList());
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    public void addAuxDepot(Depot depot) {
        auxDepots.add(depot);
    }

    public void addBlockage(Blockage blockage) {
        activeBlockages.add(blockage);
    }

    public void addBlockages(List<Blockage> blockages) {
        activeBlockages.addAll(blockages);
    }

    public List<Blockage> getActiveBlockagesAt(LocalDateTime dateTime) {
        return activeBlockages.stream()
                .filter(b -> b.isActiveAt(dateTime))
                .collect(Collectors.toList());
    }

    public void addIncident(Incident incident) {
        incidentRegistry.add(incident);
    }

    public void addIncidents(List<Incident> incidents) {
        incidentRegistry.addAll(incidents);
    }

    public List<Incident> getActiveIncidentsForVehicle(String vehicleId) {
        return incidentRegistry.stream()
                .filter(incident -> incident.getVehicleId().equals(vehicleId) &&
                        !incident.isResolved() &&
                        incident.getOccurrenceTime() != null &&
                        !currentTime.isBefore(incident.getOccurrenceTime()))
                .collect(Collectors.toList());
    }

    public void addMaintenanceTask(MaintenanceTask task) {
        maintenanceTasks.add(task);
    }

    public void addMaintenanceTasks(List<MaintenanceTask> tasks) {
        maintenanceTasks.addAll(tasks);
    }

    public boolean hasScheduledMaintenance(String vehicleId, LocalDateTime dateTime) {
        for (MaintenanceTask task : maintenanceTasks) {
            if (task.getVehicleId().equals(vehicleId) && task.isActiveAt(dateTime)) {
                return true;
            }
        }
        return false;
    }

    public MaintenanceTask getMaintenanceTaskForVehicle(String vehicleId, LocalDateTime dateTime) {
        for (MaintenanceTask task : maintenanceTasks) {
            if (task.getVehicleId().equals(vehicleId) && task.isActiveAt(dateTime)) {
                return task;
            }
        }
        return null;
    }

    public void advanceTime(int minutes) {
        this.currentTime = this.currentTime.plusMinutes(minutes);
        updateEnvironmentState();
    }

    private void updateEnvironmentState() {
        for (Vehicle vehicle : vehicles) {
            if (hasScheduledMaintenance(vehicle.getId(), currentTime)) {
                vehicle.setStatus(VehicleStatus.MAINTENANCE);
                continue;
            }

            List<Incident> activeIncidents = getActiveIncidentsForVehicle(vehicle.getId());
            if (!activeIncidents.isEmpty()) {
                Incident mostRecent = activeIncidents.get(activeIncidents.size() - 1);
                if (!mostRecent.isResolved()) {
                    LocalDateTime availabilityTime = mostRecent.calculateAvailabilityTime();
                    if (currentTime.isBefore(availabilityTime)) {
                        vehicle.setStatus(VehicleStatus.UNAVAILABLE);
                    } else {
                        mostRecent.setResolved();
                        vehicle.setStatus(VehicleStatus.AVAILABLE);
                    }
                }
            }

            if (vehicle.getStatus() != VehicleStatus.MAINTENANCE &&
                    vehicle.getStatus() != VehicleStatus.UNAVAILABLE) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
            }
        }

        for (Depot depot : auxDepots) {
            depot.refillGLP();
        }
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(vehicle -> vehicle.getStatus() == VehicleStatus.AVAILABLE ||
                        vehicle.getStatus() == VehicleStatus.IDLE ||
                        vehicle.getStatus() == VehicleStatus.DRIVING)
                .collect(Collectors.toList());
    }

    public void clearAllOrders() {
        orderQueue.clear();
    }

    /**
     * Clears all active blockages
     */
    public void clearBlockages() {
        activeBlockages.clear();
    }
    
    /**
     * Clears all recorded incidents
     */
    public void clearIncidents() {
        incidentRegistry.clear();
    }
    
    /**
     * Clears all maintenance tasks
     */
    public void clearMaintenanceTasks() {
        maintenanceTasks.clear();
    }
    
    /**
     * Reset depots to their initial state (refill GLP)
     */
    public void resetDepots() {
        // Refill main depot
        mainDepot.refillGLP();
        
        // Refill aux depots
        for (Depot depot : auxDepots) {
            depot.refillGLP();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("🌍 Environment: ")
                .append(currentTime.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        sb.append("\n📋 Vehicles (").append(vehicles.size()).append("):");
        for (Vehicle vehicle : vehicles) {
            sb.append("\n  ").append(vehicle);
        }

        sb.append("\n📋 Depots (").append(auxDepots.size()).append("):");
        for (Depot depot : auxDepots) {
            sb.append("\n  ").append(depot);
        }

        sb.append("\n📋 Orders (").append(orderQueue.size()).append("):");
        long pending = orderQueue.stream().filter(o -> !o.isDelivered() && !o.isOverdue(currentTime)).count();
        long delivered = orderQueue.stream().filter(Order::isDelivered).count();
        long overdue = orderQueue.stream().filter(o -> !o.isDelivered() && o.isOverdue(currentTime)).count();

        for (Order order : orderQueue) {
            if (order.isDelivered()) {
                // Optionally skip delivered orders in detailed list or handle as needed
            } else if (order.isOverdue(currentTime)) {
                sb.append("\n  ").append(order).append(" ⚠️ OVERDUE");
            } else {
                sb.append("\n  ").append(order);
            }
        }

        sb.append("\n  📊 Summary: ").append(pending).append(" pending, ")
                .append(overdue).append(" overdue, ")
                .append(delivered).append(" delivered");

        List<Blockage> currentBlockages = getActiveBlockagesAt(currentTime);
        sb.append("\n📋 Active Blockages (").append(currentBlockages.size()).append("):");
        for (Blockage blockage : currentBlockages) {
            sb.append("\n  ").append(blockage);
        }

        int activeIncidentCount = 0;
        sb.append("\n📋 Active Incidents:");
        for (Vehicle vehicle : vehicles) {
            List<Incident> activeIncidents = getActiveIncidentsForVehicle(vehicle.getId());
            for (Incident incident : activeIncidents) {
                if (!incident.isResolved() && incident.getOccurrenceTime() != null) {
                    sb.append("\n  ").append(incident);
                    activeIncidentCount++;
                }
            }
        }
        sb.append("\n  📊 Total: ").append(activeIncidentCount).append(" active incidents");

        int todayMaintenanceCount = 0;
        sb.append("\n📋 Today's Maintenance Tasks:");
        for (MaintenanceTask task : maintenanceTasks) {
            if (task.isActiveAt(currentTime)) {
                sb.append("\n  ").append(task);
                todayMaintenanceCount++;
            }
        }
        sb.append("\n  📊 Total: ").append(todayMaintenanceCount).append(" maintenance tasks today");

        return sb.toString();
    }

    /**
     * Creates a deep copy of this environment
     * @return A new Environment instance with copies of all internal state
     */
    public Environment clone() {
        // Create new environment with copies of immutable objects
        Environment cloned = new Environment(
            new ArrayList<>(vehicles), 
            mainDepot,  // Depot is assumed to be immutable or has its own clone method
            new ArrayList<>(auxDepots),
            LocalDateTime.of(currentTime.toLocalDate(), currentTime.toLocalTime())
        );
        
        // Copy orders
        cloned.addOrders(new ArrayList<>(orderQueue));
        
        // Copy blockages
        cloned.addBlockages(new ArrayList<>(activeBlockages));
        
        // Copy incidents
        cloned.addIncidents(new ArrayList<>(incidentRegistry));
        
        // Copy maintenance tasks
        cloned.addMaintenanceTasks(new ArrayList<>(maintenanceTasks));
        
        // Copy current solution if exists
        if (currentSolution != null) {
            cloned.setCurrentSolution(new ArrayList<>(currentSolution));
        }
        
        return cloned;
    }
}
