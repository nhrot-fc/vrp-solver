package com.vroute.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Environment {
    private final List<Vehicle> vehicles;
    private final Depot mainDepot;
    private final List<Depot> auxDepots;

    private LocalDateTime currentTime;
    private final List<Order> pendingOrders;
    private final List<Blockage> blockages;
    private final List<Incident> activeIncidents;
    private final List<Maintenance> activeMaintenance;

    public Environment(List<Vehicle> vehicles, Depot mainDepot, List<Depot> auxDepots,
            LocalDateTime referenceDateTime) {
        this.currentTime = referenceDateTime;
        this.vehicles = new ArrayList<>(vehicles);
        this.mainDepot = mainDepot;
        this.auxDepots = new ArrayList<>(auxDepots);
        this.pendingOrders = new ArrayList<>();
        this.blockages = new ArrayList<>();
        this.activeIncidents = new ArrayList<>();
        this.activeMaintenance = new ArrayList<>();
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<Depot> getAuxDepots() {
        return Collections.unmodifiableList(auxDepots);
    }

    public List<Depot> getDepots() {
        List<Depot> depots = new ArrayList<>(auxDepots);
        depots.add(mainDepot);
        return Collections.unmodifiableList(depots);
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public List<Blockage> getBlockages() {
        return Collections.unmodifiableList(blockages);
    }

    public List<Incident> getActiveIncidents() {
        return Collections.unmodifiableList(activeIncidents);
    }

    public List<Maintenance> getActiveMaintenance() {
        return Collections.unmodifiableList(activeMaintenance);
    }

    public Depot getMainDepot() {
        return mainDepot;
    }

    public void setCurrentTime(LocalDateTime newTime) {
        this.currentTime = newTime;
    }

    public void addOrder(Order order) {
        pendingOrders.add(order);
    }

    public void addOrders(List<Order> orders) {
        pendingOrders.addAll(orders);
    }

    public int removeDeliveredOrders() {
        int initialSize = pendingOrders.size();
        pendingOrders.removeIf(Order::isDelivered);
        return initialSize - pendingOrders.size();
    }

    public List<Order> getPendingOrders() {
        return pendingOrders.stream()
                .filter(order -> !order.isDelivered())
                .collect(Collectors.toList());
    }

    public List<Order> getOverdueOrders() {
        return pendingOrders.stream()
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
        blockages.add(blockage);
    }

    public void addBlockages(List<Blockage> blockages) {
        blockages.addAll(blockages);
    }

    public List<Blockage> getActiveBlockagesAt(LocalDateTime dateTime) {
        return blockages.stream()
                .filter(b -> b.isActiveAt(dateTime))
                .collect(Collectors.toList());
    }

    public void addIncident(Incident incident) {
        activeIncidents.add(incident);
    }

    public void addIncidents(List<Incident> incidents) {
        activeIncidents.addAll(incidents);
    }

    public List<Incident> getActiveIncidentsForVehicle(String vehicleId) {
        return activeIncidents.stream()
                .filter(incident -> incident.getVehicleId().equals(vehicleId) &&
                        !incident.isResolved() &&
                        incident.getOccurrenceTime() != null &&
                        !currentTime.isBefore(incident.getOccurrenceTime()))
                .collect(Collectors.toList());
    }

    public void addMaintenanceTask(Maintenance task) {
        activeMaintenance.add(task);
    }

    public void addMaintenanceTasks(List<Maintenance> tasks) {
        activeMaintenance.addAll(tasks);
    }

    public boolean hasScheduledMaintenance(String vehicleId, LocalDateTime dateTime) {
        for (Maintenance task : activeMaintenance) {
            if (task.getVehicleId().equals(vehicleId) && task.isActiveAt(dateTime)) {
                return true;
            }
        }
        return false;
    }

    public Maintenance getMaintenanceTaskForVehicle(String vehicleId, LocalDateTime dateTime) {
        for (Maintenance task : activeMaintenance) {
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
        pendingOrders.clear();
    }

    /**
     * Clears all active blockages
     */
    public void clearBlockages() {
        blockages.clear();
    }

    /**
     * Clears all recorded incidents
     */
    public void clearIncidents() {
        activeIncidents.clear();
    }

    /**
     * Clears all maintenance tasks
     */
    public void clearMaintenanceTasks() {
        activeMaintenance.clear();
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
        long delivered = pendingOrders.stream().filter(Order::isDelivered).count();
        long overdue = pendingOrders.stream().filter(o -> !o.isDelivered() && o.isOverdue(currentTime)).count();

        List<Blockage> currentBlockages = getActiveBlockagesAt(currentTime);

        int activeIncidentCount = 0;
        for (Vehicle vehicle : vehicles) {
            List<Incident> activeIncidents = getActiveIncidentsForVehicle(vehicle.getId());
            for (Incident incident : activeIncidents) {
                if (!incident.isResolved() && incident.getOccurrenceTime() != null) {
                    activeIncidentCount++;
                }
            }
        }

        int todayMaintenanceCount = 0;
        for (Maintenance task : activeMaintenance) {
            if (task.isActiveAt(currentTime)) {
                todayMaintenanceCount++;
            }
        }

        return String.format("🌍 %s 🚛 %d 🏭 %d 📦 %d(%d⚠️/%d✅) 🚧 %d ⚙️ %d 🔧 %d",
                currentTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
                vehicles.size(),
                auxDepots.size() + 1,
                pendingOrders.size(),
                overdue,
                delivered,
                currentBlockages.size(),
                activeIncidentCount,
                todayMaintenanceCount);
    }

    /**
     * Creates a deep copy of this environment
     * 
     * @return A new Environment instance with copies of all internal state
     */
    public Environment clone() {
        List<Vehicle> clonedVehicles = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            clonedVehicles.add(vehicle.clone());
        }

        Depot clonedMainDepot = mainDepot.clone();
        List<Depot> clonedAuxDepots = new ArrayList<>();
        for (Depot depot : auxDepots) {
            clonedAuxDepots.add(depot.clone());
        }

        Environment cloned = new Environment(clonedVehicles, clonedMainDepot, clonedAuxDepots, currentTime);
        List<Order> clonedOrders = new ArrayList<>();
        for (Order order : pendingOrders) {
            clonedOrders.add(order.clone());
        }
        cloned.addOrders(clonedOrders);
        List<Blockage> clonedBlockages = new ArrayList<>();
        for (Blockage blockage : blockages) {
            clonedBlockages.add(blockage.clone());
        }
        cloned.addBlockages(clonedBlockages);
        List<Incident> clonedIncidents = new ArrayList<>();
        for (Incident incident : activeIncidents) {
            clonedIncidents.add(incident.clone());
        }
        cloned.addIncidents(clonedIncidents);
        cloned.addMaintenanceTasks(activeMaintenance);
        return cloned;
    }
}
