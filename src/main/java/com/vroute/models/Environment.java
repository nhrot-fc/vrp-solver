package com.vroute.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Environment {
    private final List<Vehicle> vehicles;
    private final Depot mainDepot;
    private final List<Depot> auxDepots;

    private LocalDateTime currentTime;
    private final List<Order> pendingOrders;
    private final List<ServeRecord> serveRecords;
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
        this.serveRecords = new ArrayList<>();
        this.blockages = new ArrayList<>();
        this.activeIncidents = new ArrayList<>();
        this.activeMaintenance = new ArrayList<>();
    }

    // Getters
    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Vehicle> getAvailableVehicles() {
        return vehicles.stream().filter(v -> v.isActive()).collect(Collectors.toList());
    }

    public Depot getMainDepot() {
        return mainDepot;
    }

    public List<Depot> getAuxDepots() {
        return auxDepots;
    }

    public List<Depot> getDepots() {
        List<Depot> depots = new ArrayList<>();
        depots.add(mainDepot);
        depots.addAll(auxDepots);
        return depots;
    }

    public LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public List<Order> getPendingOrders() {
        return pendingOrders;
    }

    public List<ServeRecord> getServeRecords() {
        return serveRecords;
    }

    public List<Incident> getActiveIncidents() {
        return activeIncidents;
    }

    public List<Maintenance> getActiveMaintenance() {
        return activeMaintenance;
    }

    public List<Blockage> getActiveBlockagesAt(LocalDateTime time) {
        return blockages.stream().filter(b -> b.isActiveAt(time)).collect(Collectors.toList());
    }

    public Vehicle getVehicleById(String id) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getId().equals(id)) {
                return vehicle;
            }
        }
        return null;
    }
    
    public Order getOrderById(String id) {
        for (Order order : pendingOrders) {
            if (order.getId().equals(id)) {
                return order;
            }
        }
        return null;
    }
    
    public Depot getDepotById(String id) {
        for (Depot depot : getDepots()) {
            if (depot.getId().equals(id)) {
                return depot;
            }
        }
        return null;
    }

    // Operations
    public void addOrder(Order order) {
        pendingOrders.add(order);
    }

    public void addOrders(List<Order> orders) {
        pendingOrders.addAll(orders);
    }

    public void addBlockage(Blockage blockage) {
        blockages.add(blockage);
    }

    public void addBlockages(List<Blockage> blockages) {
        this.blockages.addAll(blockages);
    }

    public void registerServe(Vehicle vehicle, Order order, int glpDelivered, LocalDateTime serveTime) {
        serveRecords.add(new ServeRecord(vehicle.getId(), order.getId(), glpDelivered, serveTime));
    }

    public void registerIncident(Incident incident) {
        activeIncidents.add(incident);
    }

    public void registerMaintenance(Maintenance maintenance) {
        activeMaintenance.add(maintenance);
    }
    
    public void refillDepots() {
        for (Depot depot : auxDepots) {
            depot.refillGLP();
        }
    }

    // Update
    public void updateTime(Duration duration) {
        // Update current time and process operations
        currentTime = currentTime.plus(duration);

        // Clear blockages
        blockages.removeIf(b -> b.isCompleted(currentTime));

        // Clear incidents and update vehicles
        for (Incident incident : activeIncidents) {
            if (incident.isResolved(currentTime)) {
                activeIncidents.remove(incident);
                Vehicle vehicle = vehicles.stream().filter(v -> v.getId().equals(incident.getVehicleId()))
                        .findFirst().orElse(null);
                if (vehicle != null) {
                    vehicle.setStatus(VehicleStatus.AVAILABLE);
                }
            }
        }
        
        // Clear maintenance and update vehicles
        for (Maintenance maintenance : activeMaintenance) {
            if (maintenance.isCompleted(currentTime)) {
                activeMaintenance.remove(maintenance);
                Vehicle vehicle = vehicles.stream().filter(v -> v.getId().equals(maintenance.getVehicleId()))
                        .findFirst().orElse(null);
                if (vehicle != null) {
                    vehicle.setStatus(VehicleStatus.AVAILABLE);
                }
                // Create a new maintenance for the vehicle
                registerMaintenance(maintenance.createNextTask());
            }
        }

        // Clear delivered pending orders
        pendingOrders.removeIf(o -> o.isDelivered());
    }

    @Override
    public String toString() {
        long delivered = pendingOrders.stream().filter(Order::isDelivered).count();
        long overdue = pendingOrders.stream().filter(o -> !o.isDelivered() && o.isOverdue(currentTime)).count();

        List<Blockage> currentBlockages = getActiveBlockagesAt(currentTime);

        return String.format("🌍 %s 🚛 %d 🏭 %d 📦 %d(%d⚠️/%d✅) 🚧 %d ⚙️ %d 🔧 %d",
                currentTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
                vehicles.size(),
                auxDepots.size() + 1,
                pendingOrders.size(),
                overdue,
                delivered,
                currentBlockages.size(),
                activeIncidents.size(),
                activeMaintenance.size());
    }

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

        for (Maintenance maintenance : activeMaintenance) {
            cloned.registerMaintenance(maintenance);
        }

        for (Incident incident : clonedIncidents) {
            cloned.registerIncident(incident);
        }

        return cloned;
    }
}
