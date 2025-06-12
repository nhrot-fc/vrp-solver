package com.vroute.orchest;

import com.vroute.assignation.Assignator;
import com.vroute.assignation.DeliveryInstruction;
import com.vroute.assignation.MetaheuristicAssignator;
import com.vroute.assignation.Solution;
import com.vroute.models.*;
import com.vroute.operation.ActionType;
import com.vroute.operation.Action;
import com.vroute.operation.VehiclePlan;
import com.vroute.operation.VehiclePlanCreator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Orchestrator {
    private static final Logger logger = Logger.getLogger(Orchestrator.class.getName());
    
    private final Environment environment;
    private Map<Vehicle, VehiclePlan> vehiclePlans;
    private LocalDateTime simulationTime;
    private boolean simulationRunning;
    
    private PriorityQueue<Event> eventQueue;
    private List<Event> processedEvents;
    
    private AlgorithmConfig config;
    private SimulationStats stats;
    private Assignator assignator;
    
    private boolean needsReplanning;
    private LocalDateTime lastPlanningTime;

    public Orchestrator(Environment environment) {
        this.environment = environment;
        this.vehiclePlans = new HashMap<>();
        this.simulationTime = environment.getCurrentTime();
        this.simulationRunning = false;
        this.eventQueue = new PriorityQueue<>();
        this.processedEvents = new ArrayList<>();
        this.config = AlgorithmConfig.createDefault();
        this.stats = new SimulationStats();
        this.assignator = new MetaheuristicAssignator(environment);
        this.needsReplanning = true;
        this.lastPlanningTime = null;
    }
    
    public Orchestrator(Environment environment, AlgorithmConfig config) {
        this(environment);
        this.config = config;
    }
    
    public void addEvent(Event event) {
        eventQueue.add(event);
        logger.info("Added event: " + event);
    }
    
    public void addEvents(List<Event> events) {
        eventQueue.addAll(events);
        logger.info("Added " + events.size() + " events");
    }
    
    public Event peekNextEvent() {
        return eventQueue.peek();
    }
    
    public List<Event> getEventsUntil(LocalDateTime time) {
        List<Event> events = new ArrayList<>();
        Event next = eventQueue.peek();
        
        while (next != null && !next.getTime().isAfter(time)) {
            events.add(eventQueue.poll());
            next = eventQueue.peek();
        }
        
        return events;
    }
    
    public void initialize() {
        stats.resetStats();
        vehiclePlans.clear();
        eventQueue.clear();
        processedEvents.clear();
        needsReplanning = true;
        
        stats.startSimulation(environment.getCurrentTime());
        
        scheduleRecurringEvents();
        scheduleKnownEvents();
        
        simulationRunning = true;
    }
    
    private void scheduleRecurringEvents() {
        LocalDateTime currentTime = environment.getCurrentTime();
        LocalDateTime endTime = currentTime.plusDays(config.getSimulationMaxDays());
        
        LocalDateTime checkpointTime = currentTime;
        while (checkpointTime.isBefore(endTime)) {
            checkpointTime = checkpointTime.plusHours(1);
            addEvent(new Event(Event.EventType.PLAN_CHECKPOINT, checkpointTime));
        }
        
        addEvent(new Event(Event.EventType.SIMULATION_END, endTime));
    }
    
    private void scheduleKnownEvents() {
        for (MaintenanceTask task : environment.getMaintenanceTasks()) {
            String vehicleId = task.getVehicleId();
            addEvent(new Event(Event.EventType.MAINTENANCE_START, task.getStartTime(), vehicleId, task));
            addEvent(new Event(Event.EventType.MAINTENANCE_END, task.getEndTime(), vehicleId, task));
        }
        
        for (Blockage blockage : environment.getActiveBlockages()) {
            String blockageId = "blockage-" + blockage.getBlockagePoints().hashCode();
            addEvent(new Event(Event.EventType.BLOCKAGE_START, blockage.getStartTime(), 
                    blockageId, blockage));
            addEvent(new Event(Event.EventType.BLOCKAGE_END, blockage.getEndTime(), 
                    blockageId, blockage));
        }
        
        for (Order order : environment.getPendingOrders()) {
            if (!processedEvents.stream().anyMatch(e -> 
                    e.getType() == Event.EventType.ORDER_ARRIVAL && 
                    e.getEntityId() != null && 
                    e.getEntityId().equals(order.getId()))) {
                
                if (!order.getArriveDate().isBefore(environment.getCurrentTime())) {
                    addEvent(new Event(Event.EventType.ORDER_ARRIVAL, order.getArriveDate(), order.getId(), order));
                } else {
                    addEvent(new Event(Event.EventType.ORDER_ARRIVAL, environment.getCurrentTime(), order.getId(), order));
                }
            }
        }
        
        LocalDateTime refillTime = environment.getCurrentTime().plusDays(1).withHour(6).withMinute(0);
        for (Depot depot : environment.getAuxDepots()) {
            for (int day = 0; day < config.getSimulationMaxDays(); day++) {
                LocalDateTime depotRefillTime = refillTime.plusDays(day);
                addEvent(new Event(Event.EventType.GLP_DEPOT_REFILL, depotRefillTime, depot.getId()));
            }
        }
    }
    
    public boolean runSimulationStep() {
        if (!simulationRunning) {
            return false;
        }
        
        LocalDateTime nextStepTime = simulationTime.plusMinutes(config.getSimulationStepMinutes());
        
        List<Event> events = getEventsUntil(nextStepTime);
        
        for (Event event : events) {
            processEvent(event);
            processedEvents.add(event);
        }
        
        simulationTime = nextStepTime;
        environment.setCurrentTime(simulationTime);
        
        executePlansUntil(simulationTime);
        
        if (needsReplanning && (lastPlanningTime == null || 
                Duration.between(lastPlanningTime, simulationTime).toMinutes() >= config.getAlgorithmJumpValue())) {
            replanRoutes();
        }
        
        updateEnvironmentState();
        
        return simulationRunning;
    }
    
    private void processEvent(Event event) {
        logger.info("Processing event: " + event);
        
        switch (event.getType()) {
            case ORDER_ARRIVAL:
                processOrderArrival(event);
                break;
            case VEHICLE_BREAKDOWN:
                processVehicleBreakdown(event);
                break;
            case BLOCKAGE_START:
                processBlockageStart(event);
                break;
            case BLOCKAGE_END:
                processBlockageEnd(event);
                break;
            case MAINTENANCE_START:
                processMaintenanceStart(event);
                break;
            case MAINTENANCE_END:
                processMaintenanceEnd(event);
                break;
            case GLP_DEPOT_REFILL:
                processDepotRefill(event);
                break;
            case PLAN_CHECKPOINT:
                processPlanCheckpoint(event);
                break;
            case SIMULATION_END:
                processSimulationEnd(event);
                break;
            default:
                logger.warning("Unknown event type: " + event.getType());
        }
    }
    
    private void processOrderArrival(Event event) {
        Order order = event.getData();
        if (order != null) {
            environment.addOrder(order);
            stats.recordNewOrder();
            logger.info("New order received: " + order);
        }
    }
    
    private void processVehicleBreakdown(Event event) {
        String vehicleId = event.getEntityId();
        Incident breakdown = event.getData();
        
        if (vehicleId != null) {
            Vehicle vehicle = findVehicleById(vehicleId);
            if (vehicle != null) {
                vehicle.setStatus(VehicleStatus.UNAVAILABLE);
                environment.addIncident(breakdown);
                stats.recordVehicleBreakdown(vehicleId);
                needsReplanning = true;
                logger.warning("Vehicle breakdown: " + vehicleId);
                
                vehiclePlans.remove(vehicle);
            }
        }
    }
    
    private void processBlockageStart(Event event) {
        Blockage blockage = event.getData();
        if (blockage != null) {
            needsReplanning = true;
            logger.info("Blockage started: " + blockage);
        }
    }
    
    private void processBlockageEnd(Event event) {
        Blockage blockage = event.getData();
        if (blockage != null) {
            stats.recordBlockage(Duration.between(blockage.getStartTime(), blockage.getEndTime()));
            
            needsReplanning = true;
            logger.info("Blockage ended: " + blockage);
        }
    }
    
    private void processMaintenanceStart(Event event) {
        String vehicleId = event.getEntityId();
        MaintenanceTask task = event.getData();
        
        if (vehicleId != null) {
            Vehicle vehicle = findVehicleById(vehicleId);
            if (vehicle != null) {
                vehicle.setStatus(VehicleStatus.MAINTENANCE);
                stats.recordMaintenanceEvent();
                needsReplanning = true;
                
                if (task != null) {
                    logger.info("Maintenance started for vehicle: " + vehicleId + 
                               " until " + task.getEndTime());
                } else {
                    logger.info("Maintenance started for vehicle: " + vehicleId);
                }
                
                vehiclePlans.remove(vehicle);
            }
        }
    }
    
    private void processMaintenanceEnd(Event event) {
        String vehicleId = event.getEntityId();
        
        if (vehicleId != null) {
            Vehicle vehicle = findVehicleById(vehicleId);
            if (vehicle != null && vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                needsReplanning = true;
                logger.info("Maintenance ended for vehicle: " + vehicleId);
            }
        }
    }
    
    private void processDepotRefill(Event event) {
        String depotId = event.getEntityId();
        Integer refillAmount = event.getData();
        
        if (depotId != null && refillAmount != null) {
            Depot depot = findDepotById(depotId);
            if (depot != null) {
                depot.refillGLP();
                logger.info("Depot refilled: " + depotId);
            }
        }
    }
    
    private void processPlanCheckpoint(Event event) {
        verifyPlans();
    }
    
    private void processSimulationEnd(Event event) {
        simulationRunning = false;
        stats.endSimulation(simulationTime);
        logger.info("Simulation ended at " + simulationTime);
    }
    
    private void executePlansUntil(LocalDateTime targetTime) {
        Set<Order> completedOrders = new HashSet<>();
        
        for (Map.Entry<Vehicle, VehiclePlan> entry : new HashMap<>(vehiclePlans).entrySet()) {
            Vehicle vehicle = entry.getKey();
            VehiclePlan plan = entry.getValue();
            
            if (vehicle.getStatus() != VehicleStatus.AVAILABLE && 
                vehicle.getStatus() != VehicleStatus.DRIVING && 
                vehicle.getStatus() != VehicleStatus.SERVING &&
                vehicle.getStatus() != VehicleStatus.REFUELING &&
                vehicle.getStatus() != VehicleStatus.RELOADING) {
                continue;
            }
            
            LocalDateTime currentActionTime = plan.getStartTime();
            List<Action> actions = plan.getActions();
            
            for (int i = 0; i < actions.size(); i++) {
                Action action = actions.get(i);
                LocalDateTime actionEndTime = currentActionTime.plus(action.getDuration());
                
                if (actionEndTime.isAfter(targetTime)) {
                    break;
                }
                
                updateVehicleStateFromAction(vehicle, action, currentActionTime);
                
                recordActionStatistics(vehicle, action);
                
                if (action.getType() == ActionType.SERVE && action.getOrder() != null) {
                    int glpDeliveredM3 = (int) Math.round(action.getGlpChangeM3());
                    action.getOrder().recordDelivery(glpDeliveredM3, vehicle.getId(), actionEndTime);
                    completedOrders.add(action.getOrder());
                    
                    stats.recordDeliveredOrder(action.getOrder().isOverdue(actionEndTime));
                    stats.recordVehicleDelivery(vehicle.getId());
                }
                
                currentActionTime = actionEndTime;
            }
            
            vehicle.setStatus(plan.getStatusAt(targetTime));
        }
        
        for (Order order : completedOrders) {
            logger.info("Order delivered: " + order.getId());
        }
    }
    
    private void updateVehicleStateFromAction(Vehicle vehicle, Action action, LocalDateTime actionTime) {
        switch (action.getType()) {
            case DRIVE:
                vehicle.setCurrentPosition(action.getDestination());
                vehicle.consumeFuel(action.getFuelChangeGal());
                break;
                
            case REFUEL:
                vehicle.refuel();
                break;
                
            case RELOAD:
                int glpAmount = (int) Math.round(action.getGlpChangeM3());
                vehicle.refill(glpAmount);
                break;
                
            case SERVE:
                int glpToDispense = (int) Math.round(action.getGlpChangeM3());
                vehicle.dispenseGlp(glpToDispense);
                break;
                
            case MAINTENANCE:
                break;

            default:
                break;
        }
    }
    
    private void recordActionStatistics(Vehicle vehicle, Action action) {
        switch (action.getType()) {
            case DRIVE:
                double distance = VehiclePlanCreator.calculatePathDistance(action.getPath());
                double fuel = action.getFuelChangeGal();
                Duration drivingTime = action.getDuration();
                
                stats.recordVehicleOperation(vehicle.getId(), distance, fuel, drivingTime);
                break;
                
            case WAIT:
                stats.recordVehicleIdle(vehicle.getId(), action.getDuration());
                break;
                
            default:
                stats.recordVehicleOperation(vehicle.getId(), 0, 0, action.getDuration());
                break;
        }
    }
    
    private void verifyPlans() {
        boolean planIssuesFound = false;
        
        for (Map.Entry<Vehicle, VehiclePlan> entry : vehiclePlans.entrySet()) {
            Vehicle vehicle = entry.getKey();
            
            if (vehicle.getStatus() == VehicleStatus.UNAVAILABLE ||
                vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
                planIssuesFound = true;
                vehiclePlans.remove(vehicle);
                continue;
            }
        }
        
        needsReplanning = needsReplanning || planIssuesFound;
    }
    
    private void updateEnvironmentState() {
        for (Vehicle vehicle : environment.getVehicles()) {
            if (shouldGenerateRandomBreakdown(vehicle)) {
                Shift currentShift = Shift.getShiftForTime(simulationTime.toLocalTime());
                Incident breakdown = new Incident(vehicle.getId(), IncidentType.TI2, currentShift);
                breakdown.setOccurrenceTime(simulationTime);
                breakdown.setLocation(vehicle.getCurrentPosition());
                addEvent(new Event(Event.EventType.VEHICLE_BREAKDOWN, simulationTime, vehicle.getId(), breakdown));
            }
        }
    }
    
    private boolean shouldGenerateRandomBreakdown(Vehicle vehicle) {
        if (vehicle.getStatus() == VehicleStatus.DRIVING) {
            return Math.random() < 0.001;
        }
        return false;
    }
    
    private void replanRoutes() {
        long startTime = System.currentTimeMillis();
        
        try {
            Solution solution = assignator.solve(environment);
            
            convertSolutionToPlans(solution);
            
            long planningTime = System.currentTimeMillis() - startTime;
            stats.recordReplan(planningTime);
            lastPlanningTime = simulationTime;
            needsReplanning = false;
            
            logger.info("Replanning completed in " + planningTime + "ms");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during replanning", e);
        }
    }
    
    private void convertSolutionToPlans(Solution solution) {
        vehiclePlans.entrySet().removeIf(entry -> 
            entry.getKey().getStatus() == VehicleStatus.AVAILABLE);
        
        Map<Vehicle, List<DeliveryInstruction>> assignments = solution.getVehicleOrderAssignments();
        
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : assignments.entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<DeliveryInstruction> instructions = entry.getValue();
            
            if (!instructions.isEmpty()) {
                VehiclePlan plan = VehiclePlanCreator.createPlan(environment, vehicle, instructions);
                
                if (plan != null) {
                    vehiclePlans.put(vehicle, plan);
                } else {
                    logger.log(Level.WARNING, "Failed to create plan for vehicle " + vehicle.getId());
                }
            }
        }
    }
    
    private Vehicle findVehicleById(String vehicleId) {
        return environment.getVehicles().stream()
                .filter(v -> v.getId().equals(vehicleId))
                .findFirst()
                .orElse(null);
    }
    
    private Depot findDepotById(String depotId) {
        if (environment.getMainDepot().getId().equals(depotId)) {
            return environment.getMainDepot();
        }
        
        return environment.getAuxDepots().stream()
                .filter(d -> d.getId().equals(depotId))
                .findFirst()
                .orElse(null);
    }
    
    public List<Order> getPendingOrders() {
        return environment.getPendingOrders();
    }
    
    public List<Order> getOverdueOrders() {
        return environment.getOverdueOrders();
    }
    
    public SimulationStats getStats() {
        return stats;
    }
    
    public Map<Vehicle, VehiclePlan> getVehiclePlans() {
        return new HashMap<>(vehiclePlans);
    }
    
    public void setConfig(AlgorithmConfig config) {
        this.config = config;
    }
    
    public AlgorithmConfig getConfig() {
        return config;
    }
    
    public void setAssignator(Assignator assignator) {
        this.assignator = assignator;
    }
    
    public SimulationStats runFullSimulation() {
        initialize();
        
        while (simulationRunning) {
            runSimulationStep();
        }
        
        return stats;
    }
    
    public void updateVisualization() {
    }
}
