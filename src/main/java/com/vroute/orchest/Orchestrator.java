package com.vroute.orchest;

import com.vroute.models.*;
import com.vroute.solution.DepotStop;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;
import com.vroute.taboo.TabuSearch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.ArrayList;

/**
 * Orchestrator manages the simulation of the environment over time,
 * handling events like order arrivals, blockages, and maintenance tasks.
 */
public class Orchestrator {
    private Environment environment;
    private PriorityQueue<Event> globalEventsQueue;
    private PriorityQueue<Event> simulationEventsQueue;
    private DataReader dataReader;
    private TabuSearch solver;

    private LocalDateTime lastReplanningTime;

    // For reset
    private Environment initialEnvironment;
    private String ordersFilePath;
    private String blockagesFilePath;
    private String maintenanceFilePath;

    // Constants
    private static final int SERVICE_TIME = 15; // Default service time in minutes
    private static final int DEPRATURE_TIME = 15; // Default departure time in minutes

    // Tiempo entre replanificaciones (en minutos)
    private static final long REPLAN_INTERVAL_MINUTES = 60;

    // Factor de demanda mínimo para justificar replanificación
    private static final int DEMAND_FACTOR_THRESHOLD = 5;

    public Orchestrator(Environment initialEnvironment) {
        this.environment = initialEnvironment;
        // Store initial time for reset
        this.initialEnvironment = new Environment(
                new ArrayList<>(),
                null,
                new ArrayList<>(),
                initialEnvironment.getCurrentTime());
        this.globalEventsQueue = new PriorityQueue<>();
        this.simulationEventsQueue = new PriorityQueue<>();
        this.dataReader = new DataReader();
        lastReplanningTime = environment.getCurrentTime();
    }

    /**
     * Resets the simulation to its initial state
     * Must be called after loadEvents to enable proper reset
     * 
     * @return The reset environment instance
     */
    public Environment resetSimulation() {
        // Clear event queues
        globalEventsQueue.clear();
        simulationEventsQueue.clear();

        // Reset environment to initial state by clearing all dynamic data
        // Clear orders
        environment.clearAllOrders();

        // Reset time to original
        environment.setCurrentTime(initialEnvironment.getCurrentTime());

        // Reset vehicle status to AVAILABLE
        for (Vehicle vehicle : environment.getVehicles()) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }

        // Clear active blockages, incidents and maintenance tasks
        environment.clearBlockages();
        environment.clearIncidents();
        environment.clearMaintenanceTasks();

        // Reset any depot resources if needed
        environment.resetDepots();

        // If file paths are stored, reload events
        if (ordersFilePath != null && blockagesFilePath != null && maintenanceFilePath != null) {
            loadEvents(ordersFilePath, blockagesFilePath, maintenanceFilePath);
        }

        System.out.println("Simulation has been reset to initial state");
        return environment;
    }

    /**
     * Init global events queue with initial state
     */
    public void loadEvents(String ordersFilePath, String blockagesFilePath, String maintenanceFilePath) {
        // Store file paths for potential reset
        this.ordersFilePath = ordersFilePath;
        this.blockagesFilePath = blockagesFilePath;
        this.maintenanceFilePath = maintenanceFilePath;

        LocalDateTime startTime = environment.getCurrentTime();

        // Load orders for next 24 hours
        List<Order> orders = dataReader.loadOrders(ordersFilePath, startTime, 24, 0);
        for (Order order : orders) {
            Event event = new Event(EventType.ORDER_ARRIVAL, order.getArriveTime(), order.getId(), order);
            globalEventsQueue.add(event);
        }

        // Load blockages for next 24 hours
        List<Blockage> blockages = dataReader.loadBlockages(blockagesFilePath, startTime, 24, 0);
        for (Blockage blockage : blockages) {
            // Schedule start of blockage
            Event startEvent = new Event(EventType.BLOCKAGE_START, blockage.getStartTime(),
                    "blockage", blockage);
            globalEventsQueue.add(startEvent);

            // Schedule end of blockage
            Event endEvent = new Event(EventType.BLOCKAGE_END, blockage.getEndTime(),
                    "blockage", blockage);
            globalEventsQueue.add(endEvent);
        }

        // Load maintenance tasks for next 30 days
        List<Maintenance> tasks = dataReader.loadMaintenanceSchedule(maintenanceFilePath, startTime, 30, 0);
        for (Maintenance task : tasks) {
            // Schedule start of maintenance
            Event startEvent = new Event(EventType.MAINTENANCE_START, task.getStartTime(),
                    task.getVehicleId(), task);
            globalEventsQueue.add(startEvent);

            // Schedule end of maintenance
            Event endEvent = new Event(EventType.MAINTENANCE_END, task.getEndTime(),
                    task.getVehicleId(), task);
            globalEventsQueue.add(endEvent);
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
        while ((!globalEventsQueue.isEmpty() && !globalEventsQueue.peek().getTime().isAfter(targetTime)) ||
                (!simulationEventsQueue.isEmpty() && !simulationEventsQueue.peek().getTime().isAfter(targetTime))) {
            Event event = null;

            // Compare events from both queues and take the earliest one
            if (!globalEventsQueue.isEmpty() && !simulationEventsQueue.isEmpty()) {
                Event nextSimulationEvent = simulationEventsQueue.peek();
                Event nextGlobalEvent = globalEventsQueue.peek();
                if (nextGlobalEvent.getTime().isAfter(nextSimulationEvent.getTime())) {
                    event = simulationEventsQueue.poll();
                } else {
                    event = globalEventsQueue.poll();
                }
            } else if (!globalEventsQueue.isEmpty()) {
                event = globalEventsQueue.poll();
            } else if (!simulationEventsQueue.isEmpty()) {
                event = simulationEventsQueue.poll();
            }

            if (event == null) {
                break;
            }
            handleEvent(event);
        }

        if (needsReplanning()) {
            Solution solution = solver.solve(environment);
            processSolution(solution);
            lastReplanningTime = environment.getCurrentTime();
        }

        // Update environment time
        environment.advanceTime(minutes);
    }

    /**
     * Helper method for debug output
     * 
     * @param message The debug message to print
     */
    private void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[DEBUG] " + message);
        }
    }

    /**
     * Process a single event based on its type
     */
    private void handleEvent(Event event) {
        debug("Handling event: " + event);

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
                Maintenance task = event.getData();
                if (!environment.getActiveMaintenance().contains(task)) {
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
                for (Depot depot : environment.getAuxDepots()) {
                    if (depot.getId().equals(event.getEntityId())) {
                        depot.refillGLP();
                        break;
                    }
                }
                break;

            case SIMULATION_END:
                // Implementation for simulation end
                break;

            case ORDER_DELIVERED:
                OrderStop orderStop = event.getData();
                for (Order or : environment.getPendingOrders()) {
                    if (or.getId().equals(orderStop.getOrder().getId())) {
                        or.recordDelivery(orderStop.getGlpDelivery(), event.getEntityId(), event.getTime());
                        environment.removeDeliveredOrders();
                        break;
                    }
                }
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(event.getEntityId())) {
                        vehicle.dispenseGlp(orderStop.getGlpDelivery());
                        vehicle.setStatus(VehicleStatus.SERVING);
                        break;
                    }
                }
                break;

            case GLP_DEPOT_UPDATED:
                DepotStop depotStop = event.getData();
                for (Depot depot : environment.getAuxDepots()) {
                    if (depot.getId().equals(depotStop.getDepot().getId())) {
                        depot.serveGLP(depotStop.getGlpRecharge());
                        break;
                    }
                }
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(event.getEntityId())) {
                        vehicle.dispenseGlp(depotStop.getGlpRecharge());
                        break;
                    }
                }
                break;

            case VEHICLE_DEPARTURE:
                String departingVehicleId = event.getEntityId();
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(departingVehicleId)) {
                        vehicle.setStatus(VehicleStatus.DRIVING);
                        break;
                    }
                }
                break;

            case VEHICLE_ARRIVES_MAIN_DEPOT:
                String arrivingVehicleId = event.getEntityId();
                for (Vehicle vehicle : environment.getVehicles()) {
                    if (vehicle.getId().equals(arrivingVehicleId)) {
                        vehicle.setStatus(VehicleStatus.IDLE);
                        break;
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getType());
        }
    }

    /**
     * Determina si es necesaria una replanificación basada en:
     * 1. Ha transcurrido una hora desde la última replanificación
     * 2. El factor de demanda (nuevas órdenes) supera el umbral definido
     *
     * @return true si se debe ejecutar una replanificación, false en caso
     *         contrario.
     */
    private boolean needsReplanning() {
        LocalDateTime currentTime = environment.getCurrentTime();

        // Calculamos minutos desde la última replanificación
        long minutesSinceLastReplan = java.time.Duration.between(lastReplanningTime, currentTime).toMinutes();
        debug("Minutos desde última replanificación: " + minutesSinceLastReplan);

        // Verificamos si ha pasado el tiempo de intervalo
        if (minutesSinceLastReplan >= REPLAN_INTERVAL_MINUTES) {
            // Contamos nuevas órdenes desde la última replanificación
            List<Order> pendingOrders = environment.getPendingOrders();
            long newOrdersCount = pendingOrders.stream()
                    .filter(order -> order.getArriveTime().isAfter(lastReplanningTime))
                    .count();

            debug("Órdenes nuevas: " + newOrdersCount + "/" + DEMAND_FACTOR_THRESHOLD);

            // Replanificamos si hay suficiente demanda
            if (newOrdersCount >= DEMAND_FACTOR_THRESHOLD) {
                System.out.println("Replanificando: " + newOrdersCount + " nuevas órdenes desde última planificación");
                return true;
            }
        }

        return false;
    }

    /**
     * Process a solution to generate simulation events
     * 
     * @param solution The solution to process
     */
    private void processSolution(Solution solution) {
        // Clear existing simulation events
        simulationEventsQueue.clear();
        debug("Procesando solución con " + solution.getRoutes().size() + " rutas");

        // Process each route in the solution
        for (Route route : solution.getRoutes()) {
            String vehicleId = route.getVehicle().getId();

            // Skip routes for vehicles in maintenance or unavailable
            boolean vehicleAvailable = environment.getVehicles().stream()
                    .filter(v -> v.getId().equals(vehicleId))
                    .anyMatch(v -> v.getStatus() != VehicleStatus.MAINTENANCE
                            && v.getStatus() != VehicleStatus.UNAVAILABLE);

            if (!vehicleAvailable) {
                continue;
            }

            for (RouteStop stop : route.getStops()) {
                // Process main depots stops
                if (stop instanceof DepotStop) {
                    DepotStop depotStop = (DepotStop) stop;
                    if (depotStop.getDepot().getId().equals(environment.getMainDepot().getId())) {
                        // Create arrival to main depot and departure events
                        LocalDateTime arrivalTime = route.getStartTime();
                        simulationEventsQueue.add(
                                new Event(EventType.VEHICLE_ARRIVES_MAIN_DEPOT, arrivalTime, vehicleId, depotStop));

                        // Schedule departure after service time
                        LocalDateTime departureTime = arrivalTime.plusMinutes(DEPRATURE_TIME);
                        simulationEventsQueue
                                .add(new Event(EventType.VEHICLE_DEPARTURE, departureTime, vehicleId, vehicleId));
                    } else {
                        // It is a stop for an auxiliary depot
                        // Create GLP depot update event
                        simulationEventsQueue.add(new Event(EventType.GLP_DEPOT_UPDATED, route.getStartTime(),
                                vehicleId, depotStop));
                    }

                } else if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    // Create order delivery event
                    simulationEventsQueue.add(
                            new Event(EventType.ORDER_DELIVERED, route.getStartTime(), vehicleId, orderStop));

                    // Schedule vehicle departure after service time
                    LocalDateTime departureTime = route.getStartTime().plusMinutes(SERVICE_TIME);
                    simulationEventsQueue
                            .add(new Event(EventType.VEHICLE_DEPARTURE, departureTime, vehicleId, vehicleId));
                }
            }
        }
    }

    /**
     * Gets the current environment
     */
    public Environment getEnvironment() {
        return environment;
    }
}
