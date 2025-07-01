package com.vroute.orchest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import com.vroute.models.*;
import com.vroute.solution.Route;
import com.vroute.solution.Solution;
import com.vroute.solution.Solver;
import com.vroute.taboo.TabuSearch;
import com.vroute.solution.RouteStop;
import com.vroute.solution.OrderStop;
import com.vroute.solution.DepotStop;

/**
 * Orchestrator manages the simulation of the environment over time,
 * handling events like order arrivals, blockages, and maintenance tasks.
 */
public class Orchestrator {
    private final Environment environment;
    private final Solver solver;
    private final PriorityQueue<Event> eventQueue;
    private final Random random;

    private boolean running;
    private boolean replanFlag;
    private Solution currentSolution;

    public Orchestrator(Environment environment) {
        this.environment = environment;
        this.solver = new TabuSearch();
        this.eventQueue = new PriorityQueue<>();
        this.random = new Random();
        this.running = false;
        this.replanFlag = true;
        this.currentSolution = null;
    }

    public void addEvent(Event event) {
        eventQueue.add(event);
    }

    public void addEvents(List<Event> events) {
        eventQueue.addAll(events);
    }

    public void start() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public Solution getCurrentSolution() {
        return this.currentSolution;
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public void advanceTime(Duration duration) {
        if (!running)
            return;
        if (replanFlag) {
            currentSolution = replan();
        }

        // Process any events that should occur during this time period
        LocalDateTime currentTime = environment.getCurrentTime();
        LocalDateTime futureTime = currentTime.plus(duration);
        
        // Process events scheduled before or at futureTime
        while (!eventQueue.isEmpty() && !eventQueue.peek().getTime().isAfter(futureTime)) {
            Event event = eventQueue.poll();
            processEvent(event);
        }
        
        // Randomly generate incidents for vehicles in transit if they're active
        if (currentSolution != null) {
            checkForRandomIncidents(currentTime, duration);
        }
        
        // Process solution to move vehicles along routes
        processSolution(duration);
        
        // Update environment time
        environment.updateTime(duration);
    }

    private void checkForRandomIncidents(LocalDateTime currentTime, Duration duration) {
        // Only consider active vehicles that are currently on a route
        for (Route route : currentSolution.getRoutes()) {
            Vehicle vehicle = environment.getVehicleById(route.getVehicle().getId());
            
            if (vehicle != null && vehicle.isActive()) {
                // Calculate incident probability based on vehicle type and route length
                double incidentProbability = calculateIncidentProbability(vehicle, route);
                
                // Scale probability by the duration of this time step
                double scaledProbability = incidentProbability * (duration.toMinutes() / 60.0);
                
                // Random check if an incident occurs
                if (random.nextDouble() < scaledProbability) {
                    // Generate a random incident
                    IncidentType incidentType = getRandomIncidentType();
                    Shift currentShift = determineCurrentShift(currentTime);
                    Incident incident = new Incident(vehicle.getId(), incidentType, currentShift, currentTime);
                    
                    // Create and queue an immediate incident event
                    Event incidentEvent = new Event(EventType.INCIDENT, currentTime, incident);
                    processEvent(incidentEvent);
                }
            }
        }
    }
    
    private double calculateIncidentProbability(Vehicle vehicle, Route route) {
        // Base probability from Constants
        double baseProb = Constants.INCIDENT_ROUTE_OCCURRENCE_MIN_PERCENTAGE;
        
        // Adjust based on vehicle type (older types more prone to incidents)
        switch (vehicle.getType()) {
            case TA: baseProb *= 1.5; break;  // Oldest type
            case TB: baseProb *= 1.2; break;
            case TC: baseProb *= 1.0; break;
            case TD: baseProb *= 0.8; break;  // Newest type
        }
        
        // Adjust based on route length
        int stopCount = route.getStops().size();
        if (stopCount > 10) baseProb *= 1.5;
        else if (stopCount > 5) baseProb *= 1.2;
        
        // Cap at max probability
        return Math.min(baseProb, Constants.INCIDENT_ROUTE_OCCURRENCE_MAX_PERCENTAGE);
    }
    
    private IncidentType getRandomIncidentType() {
        // Different probability for each incident type
        double value = random.nextDouble();
        if (value < 0.6) {
            return IncidentType.TI1; // 60% chance of minor incident
        } else if (value < 0.9) {
            return IncidentType.TI2; // 30% chance of moderate incident
        } else {
            return IncidentType.TI3; // 10% chance of major incident
        }
    }
    
    private Shift determineCurrentShift(LocalDateTime time) {
        int hour = time.getHour();
        if (hour >= 6 && hour < 14) {
            return Shift.T1;  // Morning shift (6am-2pm)
        } else if (hour >= 14 && hour < 22) {
            return Shift.T2;  // Afternoon shift (2pm-10pm)
        } else {
            return Shift.T3;  // Night shift (10pm-6am)
        }
    }

    public Solution replan() {
        replanFlag = false;
        return solver.solve(environment);
    }

    public void processSolution(Duration duration) {
        if (currentSolution == null)
            return;

        LocalDateTime currentTime = environment.getCurrentTime();
        LocalDateTime futureTime = currentTime.plus(duration);

        for (Route route : currentSolution.getRoutes()) {
            Vehicle vehicle = environment.getVehicleById(route.getVehicle().getId());
            if (vehicle == null || !vehicle.isActive())
                continue;

            // Skip routes that haven't started yet
            if (route.getStartTime().isAfter(currentTime))
                continue;

            // Process this route until we reach futureTime or complete all stops
            LocalDateTime routeTime = route.getStartTime();
            if (routeTime.isBefore(currentTime)) {
                routeTime = currentTime;
            }

            Position currentPosition = vehicle.getCurrentPosition();
            List<RouteStop> remainingStops = route.getStops();

            while (!remainingStops.isEmpty() && !routeTime.isAfter(futureTime)) {
                RouteStop currentStop = remainingStops.get(0);
                List<Position> path = currentStop.getPath();

                if (path == null || path.isEmpty()) {
                    // We're already at the stop, process it
                    processStop(vehicle, currentStop, routeTime);
                    remainingStops.remove(0);
                    continue;
                }

                // Find our position in the path
                int currentPosIndex = -1;
                for (int i = 0; i < path.size(); i++) {
                    if (path.get(i).equals(currentPosition)) {
                        currentPosIndex = i;
                        break;
                    }
                }

                // If we can't find our position, start from the beginning
                if (currentPosIndex == -1)
                    currentPosIndex = 0;

                // Process movement along the path
                boolean reachedStop = false;
                for (int i = currentPosIndex + 1; i < path.size(); i++) {
                    Position nextPosition = path.get(i);

                    // Calculate time and fuel to move to next position
                    double distanceKm = currentPosition.distanceTo(nextPosition);
                    double fuelNeeded = vehicle.calculateFuelNeeded(distanceKm);
                    int timeMinutes = (int) Math.ceil(distanceKm * 60 / Constants.VEHICLE_AVG_SPEED);

                    // Check if we have enough fuel
                    if (vehicle.getCurrentFuelGal() < fuelNeeded) {
                        // Not enough fuel, vehicle is stranded
                        vehicle.setStatus(VehicleStatus.UNAVAILABLE);
                        break;
                    }

                    // Check if moving to this position would exceed our time limit
                    LocalDateTime nextTime = routeTime.plusMinutes(timeMinutes);
                    if (nextTime.isAfter(futureTime)) {
                        // We don't have enough time to reach the next position
                        break;
                    }

                    // Move to next position
                    vehicle.consumeFuelFromDistance(distanceKm);
                    vehicle.setCurrentPosition(nextPosition);
                    currentPosition = nextPosition;
                    routeTime = nextTime;

                    // If we've reached the stop position
                    if (i == path.size() - 1) {
                        processStop(vehicle, currentStop, routeTime);
                        remainingStops.remove(0);
                        reachedStop = true;
                        break;
                    }
                }

                if (!reachedStop) {
                    // We didn't reach the stop within the time limit
                    break;
                }
            }
        }
    }

    private void processStop(Vehicle vehicle, RouteStop stop, LocalDateTime stopTime) {
        if (stop instanceof OrderStop) {
            OrderStop orderStop = (OrderStop) stop;
            Order order = environment.getOrderById(orderStop.getOrder().getId());

            if (order != null && !order.isDelivered()) {
                int glpToDeliver = Math.min(orderStop.getGlpDelivery(), order.getRemainingGlpM3());
                glpToDeliver = Math.min(glpToDeliver, vehicle.getCurrentGlpM3());

                if (glpToDeliver > 0) {
                    vehicle.serveOrder(order, glpToDeliver, stopTime);
                }
            }
        } else if (stop instanceof DepotStop) {
            DepotStop depotStop = (DepotStop) stop;
            Depot depot = environment.getDepotById(depotStop.getDepot().getId());

            if (depot != null) {
                // Refill GLP
                int glpToRecharge = Math.min(depotStop.getGlpRecharge(),
                        vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3());
                glpToRecharge = Math.min(glpToRecharge, depot.getCurrentGlpM3());

                if (glpToRecharge > 0) {
                    depot.serveGLP(glpToRecharge);
                    vehicle.refill(glpToRecharge);
                }

                // Refuel if possible
                if (depot.canRefuel()) {
                    vehicle.refuel();
                }
            }
        }
    }

    public void processEvent(Event event) {
        System.out.println(event);
        switch (event.getType()) {
            case INCIDENT:
                environment.registerIncident((Incident) event.getData());
                replanFlag = true;
                break;
            case MAINTENANCE:
                environment.registerMaintenance((Maintenance) event.getData());
                replanFlag = true;
                break;
            case BLOCKAGE_START:
                break;
            case BLOCKAGE_END:
                break;
            case GLP_DEPOT_REFILL:
                environment.refillDepots();
                replanFlag = true; 
                break;
            case ORDER_ARRIVAL:
                environment.addOrder((Order) event.getData());
                replanFlag = true;
                break;
            default:
                break;
        }
    }
}
