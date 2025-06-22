package com.vroute.orchest;

import com.vroute.models.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Orchestrator manages the simulation of the environment over time,
 * handling events like order arrivals, blockages, and maintenance tasks.
 */
public class Orchestrator {
    private Environment environment;
    private PriorityQueue<Event> eventQueue;
    private DataReader dataReader;
    private List<Consumer<Event>> eventListeners;

    public Orchestrator(Environment initialEnvironment) {
        this.environment = initialEnvironment;
        this.eventQueue = new PriorityQueue<>();
        this.dataReader = new DataReader();
        this.eventListeners = new ArrayList<>();
    }

    /**
     * Loads and schedules events from data files
     */
    public void loadEvents(String ordersFilePath, String blockagesFilePath, String maintenanceFilePath) {
        LocalDateTime startTime = environment.getCurrentTime();
        
        // Load orders for next 24 hours
        List<Order> orders = dataReader.loadOrders(ordersFilePath, startTime, 24, 0);
        for (Order order : orders) {
            Event event = new Event(EventType.ORDER_ARRIVAL, order.getArriveTime(), order.getId(), order);
            eventQueue.add(event);
        }
        
        // Load blockages for next 24 hours
        List<Blockage> blockages = dataReader.loadBlockages(blockagesFilePath, startTime, 24, 0);
        for (Blockage blockage : blockages) {
            // Schedule start of blockage
            Event startEvent = new Event(EventType.BLOCKAGE_START, blockage.getStartTime(), 
                                        "blockage", blockage);
            eventQueue.add(startEvent);
            
            // Schedule end of blockage
            Event endEvent = new Event(EventType.BLOCKAGE_END, blockage.getEndTime(), 
                                      "blockage", blockage);
            eventQueue.add(endEvent);
        }
        
        // Load maintenance tasks for next 30 days
        List<MaintenanceTask> tasks = dataReader.loadMaintenanceSchedule(maintenanceFilePath, startTime, 30, 0);
        for (MaintenanceTask task : tasks) {
            // Schedule start of maintenance
            Event startEvent = new Event(EventType.MAINTENANCE_START, task.getStartTime(), 
                                        task.getVehicleId(), task);
            eventQueue.add(startEvent);
            
            // Schedule end of maintenance
            Event endEvent = new Event(EventType.MAINTENANCE_END, task.getEndTime(), 
                                      task.getVehicleId(), task);
            eventQueue.add(endEvent);
        }
        environment.addBlockages(blockages);
        environment.addMaintenanceTasks(tasks);
    }
    
    /**
     * Advances the simulation by the specified number of minutes
     */
    public void advanceTime(int minutes) {
        LocalDateTime targetTime = environment.getCurrentTime().plusMinutes(minutes);
        
        // Process all events until the target time
        while (!eventQueue.isEmpty() && !eventQueue.peek().getTime().isAfter(targetTime)) {
            Event event = eventQueue.poll();
            handleEvent(event);
        }
        
        // Update environment time
        environment.advanceTime(minutes);
    }
    
    /**
     * Process a single event based on its type
     */
    private void handleEvent(Event event) {
        System.out.println("Handling event: " + event);
        switch (event.getType()) {
            case ORDER_ARRIVAL:
                Order order = event.getData();
                environment.addOrder(order);
                break;
                
            case BLOCKAGE_START:
                Blockage blockage = event.getData();
                environment.addBlockage(blockage);
                break;
                
            case BLOCKAGE_END:
                // Nothing to do, just keeping for notification
                break;
                
            case MAINTENANCE_START:
                MaintenanceTask task = event.getData();
                if (!environment.getMaintenanceTasks().contains(task)) {
                    environment.addMaintenanceTask(task);
                }
                // Find and update vehicle status
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(event.getEntityId())) {
                        vehicle.setStatus(VehicleStatus.MAINTENANCE);
                        break;
                    }
                }
                break;
                
            case MAINTENANCE_END:
                // Find and update vehicle status
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(event.getEntityId())) {
                        vehicle.setStatus(VehicleStatus.AVAILABLE);
                        break;
                    }
                }
                break;
                
            case VEHICLE_BREAKDOWN:
                Incident incident = event.getData();
                environment.addIncident(incident);
                // Find and update vehicle status
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(event.getEntityId())) {
                        vehicle.setStatus(VehicleStatus.UNAVAILABLE);
                        break;
                    }
                }
                break;
                
            case GLP_DEPOT_REFILL:
                // Implementation for depot refill
                break;
                
            case SIMULATION_END:
                // Implementation for simulation end
                break;
        }
        
        // Notify listeners
        notifyListeners(event);
    }

    /**
     * Adds an event listener
     */
    public void addEventListener(Consumer<Event> listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Notifies all listeners of an event
     */
    private void notifyListeners(Event event) {
        for (Consumer<Event> listener : eventListeners) {
            listener.accept(event);
        }
    }
    
    /**
     * Gets the current environment
     */
    public Environment getEnvironment() {
        return environment;
    }
}
