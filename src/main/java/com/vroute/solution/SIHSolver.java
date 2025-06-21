package com.vroute.solution;

import com.vroute.models.*;
import com.vroute.pathfinding.PathFinder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sequential Insertion Heuristic (SIH) implementation for the V-Route problem.
 * This heuristic builds routes one by one, inserting orders based on a cost function
 * that considers distance, time constraints, and vehicle limitations.
 */
public class SIHSolver {
    
    // Constants for the cost function weights
    private static final double ALPHA = 0.6;  // Weight for distance increase
    private static final double BETA = 0.3;   // Weight for time delay increase
    private static final double GAMMA = 0.1;  // Weight for waiting time increase
    
    // Time constants in minutes
    private static final int GLP_SERVE_DURATION_MINUTES = 15;
    
    // Average vehicle speed in km/h
    private static final double VEHICLE_AVG_SPEED = 50.0;
    
    private final Environment environment;
    private final List<Vehicle> availableVehicles;
    private final List<Depot> depots;
    private final LocalDateTime currentDateTime;
    
    /**
     * Creates a new SIH solver.
     * 
     * @param environment The environment containing the problem data
     * @param availableVehicles List of available vehicles
     * @param depots List of available depots
     * @param currentDateTime Current date and time to start the planning
     */
    public SIHSolver(Environment environment, List<Vehicle> availableVehicles, List<Depot> depots, LocalDateTime currentDateTime) {
        this.environment = environment;
        this.availableVehicles = new ArrayList<>(availableVehicles);
        this.depots = depots;
        this.currentDateTime = currentDateTime;
    }
    
    /**
     * Solves the vehicle routing problem using the SIH algorithm.
     * 
     * @param orders Map of orders to be delivered
     * @return A Solution object containing the generated routes
     */
    public Solution solve(Map<String, Order> orders) {
        if (orders.isEmpty()) {
            return new Solution(orders, Collections.emptyList());
        }
        
        // Make a copy of the orders to work with
        Map<String, Order> remainingOrders = new HashMap<>(orders);
        
        // Final list of routes
        List<Route> routes = new ArrayList<>();
        
        // Available vehicles (we'll remove vehicles as they get assigned)
        List<Vehicle> vehicles = new ArrayList<>(availableVehicles);
        
        // Sort orders by criticality (inverse of time window duration)
        List<Order> sortedOrders = sortOrdersByCriticality(remainingOrders.values());
        
        // Main loop: construct routes while there are unassigned orders and available vehicles
        while (!sortedOrders.isEmpty() && !vehicles.isEmpty()) {
            // Select next vehicle
            Vehicle vehicle = vehicles.remove(0);
            
            // Try to create a route with this vehicle
            Route route = constructRoute(vehicle, sortedOrders, remainingOrders);
            
            if (route != null) {
                routes.add(route);
                
                // Update sortedOrders (remove assigned orders)
                sortedOrders = sortedOrders.stream()
                    .filter(order -> remainingOrders.containsKey(order.getId()))
                    .collect(Collectors.toList());
            }
        }
        
        return new Solution(orders, routes);
    }
    
    /**
     * Constructs a single route for a vehicle.
     * 
     * @param vehicle The vehicle to use
     * @param sortedOrders List of orders sorted by priority
     * @param remainingOrders Map of orders that still need to be assigned
     * @return A Route object, or null if no route could be created
     */
    private Route constructRoute(Vehicle vehicle, List<Order> sortedOrders, Map<String, Order> remainingOrders) {
        // Create a unique ID for the route
        String routeId = "R" + UUID.randomUUID().toString().substring(0, 8);
        
        // Clone vehicle for simulation
        Vehicle simulatedVehicle = vehicle.clone();
        
        // Start and end at the vehicle's current position (assumed to be at a depot)
        Position startPosition = simulatedVehicle.getCurrentPosition();
        
        // Find the corresponding depot
        Depot startDepot = findDepotByPosition(startPosition);
        if (startDepot == null) {
            // Vehicle is not at a depot, can't start route
            return null;
        }
        
        // List of stops for this route
        List<RouteStop> stops = new ArrayList<>();
        
        // Start time of the route
        LocalDateTime currentTime = currentDateTime;
        
        // Try to find a seed customer for the route
        Order seedOrder = findSeedOrder(sortedOrders, simulatedVehicle, startDepot, currentTime);
        if (seedOrder == null) {
            // No feasible seed order found
            return null;
        }
        
        // Calculate the path to the seed order
        List<Position> pathToSeed = PathFinder.findPath(
            environment, 
            startPosition, 
            seedOrder.getPosition(), 
            currentTime
        );
        
        if (pathToSeed.isEmpty()) {
            // No path to seed order
            return null;
        }
        
        // Calculate distance and travel time to seed
        double distanceToSeed = calculateDistance(pathToSeed);
        LocalDateTime arrivalTimeAtSeed = calculateArrivalTime(currentTime, distanceToSeed);
        
        // Check if we need to wait for the order's time window
        if (arrivalTimeAtSeed.isBefore(seedOrder.getArriveTime())) {
            arrivalTimeAtSeed = seedOrder.getArriveTime();
        }
        
        // Update vehicle state
        double fuelConsumed = simulatedVehicle.calculateFuelNeeded(distanceToSeed);
        if (fuelConsumed > simulatedVehicle.getCurrentFuelGal()) {
            // Not enough fuel to reach seed
            return null;
        }
        simulatedVehicle.consumeFuel(distanceToSeed);
        
        // Create the order stop for the seed
        int glpDelivery = seedOrder.getRemainingGlpM3();
        if (glpDelivery > simulatedVehicle.getCurrentGlpM3()) {
            // Not enough GLP to fulfill order
            return null;
        }
        simulatedVehicle.dispenseGlp(glpDelivery);
        
        OrderStop seedStop = new OrderStop(
            seedOrder.getId(),
            seedOrder.getPosition(),
            arrivalTimeAtSeed,
            glpDelivery
        );
        
        stops.add(seedStop);
        
        // Remove seed order from remaining orders
        remainingOrders.remove(seedOrder.getId());
        
        // Update current position and time
        Position currentPosition = seedOrder.getPosition();
        currentTime = arrivalTimeAtSeed.plusMinutes(GLP_SERVE_DURATION_MINUTES);
        
        // Main insertion loop
        boolean moreInsertionsPossible = true;
        
        while (moreInsertionsPossible) {
            InsertionResult bestInsertion = findBestInsertion(
                simulatedVehicle,
                stops,
                remainingOrders,
                sortedOrders,
                currentTime
            );
            
            if (bestInsertion != null) {
                // Insert the order at the best position
                stops.add(bestInsertion.insertionPosition, bestInsertion.orderStop);
                
                if (bestInsertion.refuelStop != null) {
                    stops.add(bestInsertion.refuelPosition, bestInsertion.refuelStop);
                }
                
                // Remove from remaining orders
                remainingOrders.remove(bestInsertion.orderId);
                
                // Update vehicle state
                simulatedVehicle.dispenseGlp(bestInsertion.glpDelivery);
                
                if (bestInsertion.refuelStop != null) {
                    simulatedVehicle.refuel();
                    simulatedVehicle.refill(bestInsertion.refuelStop.getGlpRecharge());
                }
            } else {
                moreInsertionsPossible = false;
            }
        }
        
        // If we couldn't add any orders beyond the seed, return null
        if (stops.size() <= 1) {
            remainingOrders.put(seedOrder.getId(), seedOrder);
            return null;
        }
        
        // Try to add a final stop back to a depot
        DepotStop finalDepotStop = findFinalDepotStop(
            simulatedVehicle,
            currentPosition,
            currentTime
        );
        
        if (finalDepotStop != null) {
            stops.add(finalDepotStop);
        }
        
        return new Route(routeId, vehicle, stops);
    }
    
    /**
     * Finds the best order to use as the seed for a new route.
     * 
     * @param sortedOrders List of orders sorted by priority
     * @param vehicle The vehicle to use
     * @param startDepot The starting depot
     * @param currentTime Current time
     * @return A seed order, or null if none is feasible
     */
    private Order findSeedOrder(List<Order> sortedOrders, Vehicle vehicle, Depot startDepot, LocalDateTime currentTime) {
        for (Order order : sortedOrders) {
            // Calculate distance and check if the order is reachable
            List<Position> path = PathFinder.findPath(
                environment,
                startDepot.getPosition(),
                order.getPosition(),
                currentTime
            );
            
            if (path.isEmpty()) {
                continue; // No path to this order
            }
            
            double distance = calculateDistance(path);
            
            // Check if vehicle has enough fuel to reach the order and return to the depot
            double fuelNeeded = vehicle.calculateFuelNeeded(distance);
            double returnDistance = order.getPosition().distanceTo(startDepot.getPosition());
            double returnFuel = vehicle.calculateFuelNeeded(returnDistance);
            
            if (fuelNeeded + returnFuel > vehicle.getCurrentFuelGal()) {
                continue; // Not enough fuel
            }
            
            // Check if vehicle has enough GLP to serve the order
            if (order.getRemainingGlpM3() > vehicle.getCurrentGlpM3()) {
                continue; // Not enough GLP
            }
            
            // Calculate arrival time
            LocalDateTime arrivalTime = calculateArrivalTime(currentTime, distance);
            
            // Check if arrival time is before due time
            if (arrivalTime.isAfter(order.getDueTime())) {
                continue; // Too late
            }
            
            return order; // Found a feasible seed order
        }
        
        return null; // No feasible seed found
    }
    
    /**
     * Finds the best insertion of an order into the current route.
     * 
     * @param vehicle The vehicle
     * @param stops Current stops in the route
     * @param remainingOrders Map of orders that still need to be assigned
     * @param sortedOrders List of orders sorted by priority
     * @param currentTime Current time
     * @return An InsertionResult object, or null if no feasible insertion found
     */
    private InsertionResult findBestInsertion(
            Vehicle vehicle,
            List<RouteStop> stops,
            Map<String, Order> remainingOrders,
            List<Order> sortedOrders,
            LocalDateTime currentTime) {
        
        InsertionResult bestInsertion = null;
        double bestCost = Double.POSITIVE_INFINITY;
        
        // Current position is the last stop position
        Position currentPosition = stops.get(stops.size() - 1).getPosition();
        
        // Try to insert each unassigned order
        for (Order order : sortedOrders) {
            if (!remainingOrders.containsKey(order.getId())) {
                continue; // Order already assigned
            }
            
            // Check if vehicle has enough GLP to serve the order
            if (order.getRemainingGlpM3() > vehicle.getCurrentGlpM3()) {
                // Check if we can refuel at a depot
                boolean canRefuel = false;
                for (Depot depot : depots) {
                    if (depot.canRefuel() && depot.getCurrentGlpM3() >= order.getRemainingGlpM3()) {
                        canRefuel = true;
                        break;
                    }
                }
                
                if (!canRefuel) {
                    continue; // Can't fulfill this order even with refueling
                }
            }
            
            // Try to insert at each position
            for (int i = 0; i <= stops.size(); i++) {
                // Calculate cost of inserting the order at position i
                InsertionCost cost = calculateInsertionCost(
                    vehicle, 
                    stops, 
                    i, 
                    order,
                    currentPosition,
                    currentTime,
                    remainingOrders
                );
                
                if (cost != null && cost.totalCost < bestCost) {
                    // This is the best insertion so far
                    bestCost = cost.totalCost;
                    
                    // Check if we need to refuel
                    DepotStop refuelStop = null;
                    int refuelPosition = i;
                    
                    if (cost.needsRefuel) {
                        // Find best depot for refueling
                        Depot bestDepot = findBestRefuelDepot(
                            currentPosition, 
                            order.getPosition()
                        );
                        
                        if (bestDepot != null) {
                            LocalDateTime refuelArrivalTime = calculateArrivalTime(
                                currentTime, 
                                currentPosition.distanceTo(bestDepot.getPosition())
                            );
                            
                            refuelStop = new DepotStop(
                                bestDepot,
                                refuelArrivalTime,
                                vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3()
                            );
                        }
                    }
                    
                    // Create the order stop
                    OrderStop orderStop = new OrderStop(
                        order.getId(),
                        order.getPosition(),
                        cost.arrivalTime,
                        order.getRemainingGlpM3()
                    );
                    
                    bestInsertion = new InsertionResult(
                        order.getId(),
                        orderStop,
                        i,
                        order.getRemainingGlpM3(),
                        refuelStop,
                        refuelPosition
                    );
                }
            }
        }
        
        return bestInsertion;
    }
    
    /**
     * Calculates the cost of inserting an order at a specific position in the route.
     * 
     * @param vehicle The vehicle
     * @param stops Current stops in the route
     * @param insertPosition Position to insert the new stop
     * @param order Order to insert
     * @param currentPosition Current vehicle position
     * @param currentTime Current time
     * @return An InsertionCost object, or null if the insertion is not feasible
     */
    private InsertionCost calculateInsertionCost(
            Vehicle vehicle,
            List<RouteStop> stops,
            int insertPosition,
            Order order,
            Position currentPosition,
            LocalDateTime currentTime,
            Map<String, Order> remainingOrders) {
        
        // Get positions before and after insertion
        Position prevPosition = insertPosition > 0 
            ? stops.get(insertPosition - 1).getPosition() 
            : currentPosition;
        
        Position nextPosition = insertPosition < stops.size() 
            ? stops.get(insertPosition).getPosition() 
            : null;
        
        // Calculate distances
        double distanceToPrev = prevPosition.distanceTo(order.getPosition());
        double distanceToNext = nextPosition != null 
            ? order.getPosition().distanceTo(nextPosition) 
            : 0;
        double directDistance = nextPosition != null 
            ? prevPosition.distanceTo(nextPosition) 
            : 0;
        
        // Calculate extra distance
        double extraDistance = distanceToPrev + distanceToNext - directDistance;
        
        // Check if vehicle has enough fuel
        double fuelNeeded = vehicle.calculateFuelNeeded(distanceToPrev);
        boolean needsRefuel = false;
        
        if (fuelNeeded > vehicle.getCurrentFuelGal()) {
            // Vehicle needs to refuel
            needsRefuel = true;
            
            // Check if there's a depot nearby that can refuel
            Depot refuelDepot = findBestRefuelDepot(prevPosition, order.getPosition());
            if (refuelDepot == null) {
                return null; // No feasible refuel depot
            }
        }
        
        // Calculate arrival time at the order
        LocalDateTime arrivalTime;
        if (insertPosition > 0) {
            LocalDateTime prevArrivalTime = stops.get(insertPosition - 1).getArrivalTime();
            // Add service time for previous stop
            LocalDateTime departureTime = prevArrivalTime.plusMinutes(GLP_SERVE_DURATION_MINUTES);
            arrivalTime = calculateArrivalTime(departureTime, distanceToPrev);
        } else {
            arrivalTime = calculateArrivalTime(currentTime, distanceToPrev);
        }
        
        // Check if we arrive within the time window
        if (arrivalTime.isAfter(order.getDueTime())) {
            return null; // Too late
        }
        
        // Calculate waiting time if we arrive before the start of the time window
        long waitingMinutes = 0;
        if (arrivalTime.isBefore(order.getArriveTime())) {
            Duration waitingTime = Duration.between(arrivalTime, order.getArriveTime());
            waitingMinutes = waitingTime.toMinutes();
            arrivalTime = order.getArriveTime();
        }
        
        // Calculate delay for subsequent stops
        LocalDateTime departureTime = arrivalTime.plusMinutes(GLP_SERVE_DURATION_MINUTES);
        long totalDelay = 0;
        
        if (nextPosition != null) {
            // Calculate how the insertion affects the timing of subsequent stops
            LocalDateTime newNextArrival = calculateArrivalTime(departureTime, distanceToNext);
            LocalDateTime oldNextArrival = stops.get(insertPosition).getArrivalTime();
            
            if (newNextArrival.isAfter(oldNextArrival)) {
                Duration delay = Duration.between(oldNextArrival, newNextArrival);
                totalDelay = delay.toMinutes();
                
                // Check if the delay causes any time window violations for subsequent stops
                LocalDateTime currentArrival = newNextArrival;
                for (int i = insertPosition; i < stops.size(); i++) {
                    RouteStop stop = stops.get(i);
                    
                    if (stop instanceof OrderStop) {
                        String orderId = ((OrderStop) stop).getEntityID();
                        Order nextOrder = remainingOrders.get(orderId);
                        if (nextOrder != null && currentArrival.isAfter(nextOrder.getDueTime())) {
                            return null; // Time window violation
                        }
                    }
                    
                    // Calculate arrival time for next stop
                    if (i < stops.size() - 1) {
                        double distanceToNextStop = stop.getPosition().distanceTo(stops.get(i + 1).getPosition());
                        currentArrival = calculateArrivalTime(
                            currentArrival.plusMinutes(GLP_SERVE_DURATION_MINUTES),
                            distanceToNextStop
                        );
                    }
                }
            }
        }
        
        // Calculate cost components
        double distanceCost = ALPHA * extraDistance;
        double delayCost = BETA * totalDelay;
        double waitingCost = GAMMA * waitingMinutes;
        
        double totalCost = distanceCost + delayCost + waitingCost;
        
        return new InsertionCost(totalCost, arrivalTime, needsRefuel);
    }
    
    /**
     * Finds the best depot for refueling between two positions.
     * 
     * @param fromPosition Starting position
     * @param toPosition Destination position
     * @return The best depot, or null if none is feasible
     */
    private Depot findBestRefuelDepot(Position fromPosition, Position toPosition) {
        Depot bestDepot = null;
        double bestDeviation = Double.POSITIVE_INFINITY;
        
        for (Depot depot : depots) {
            if (!depot.canRefuel()) {
                continue;
            }
            
            double directDistance = fromPosition.distanceTo(toPosition);
            double detourDistance = fromPosition.distanceTo(depot.getPosition()) + 
                                   depot.getPosition().distanceTo(toPosition);
            
            double deviation = detourDistance - directDistance;
            
            if (deviation < bestDeviation) {
                bestDepot = depot;
                bestDeviation = deviation;
            }
        }
        
        return bestDepot;
    }
    
    /**
     * Finds a suitable final depot stop for the route.
     * 
     * @param vehicle The vehicle
     * @param currentPosition Current position
     * @param currentTime Current time
     * @return A DepotStop, or null if no feasible depot found
     */
    private DepotStop findFinalDepotStop(
            Vehicle vehicle,
            Position currentPosition,
            LocalDateTime currentTime) {
        
        Depot bestDepot = null;
        double shortestDistance = Double.POSITIVE_INFINITY;
        
        for (Depot depot : depots) {
            double distance = currentPosition.distanceTo(depot.getPosition());
            
            // Check if vehicle has enough fuel to reach this depot
            double fuelNeeded = vehicle.calculateFuelNeeded(distance);
            if (fuelNeeded > vehicle.getCurrentFuelGal()) {
                continue;
            }
            
            if (distance < shortestDistance) {
                bestDepot = depot;
                shortestDistance = distance;
            }
        }
        
        if (bestDepot != null) {
            LocalDateTime arrivalTime = calculateArrivalTime(currentTime, shortestDistance);
            
            return new DepotStop(
                bestDepot,
                arrivalTime,
                0  // No GLP recharge needed at the end of the route
            );
        }
        
        return null;
    }
    
    /**
     * Sorts orders by criticality (inverse of time window duration).
     * 
     * @param orders Collection of orders to sort
     * @return Sorted list of orders
     */
    private List<Order> sortOrdersByCriticality(Collection<Order> orders) {
        return orders.stream()
            .sorted((o1, o2) -> {
                // Calculate criticality as inverse of time window duration
                Duration window1 = Duration.between(o1.getArriveTime(), o1.getDueTime());
                Duration window2 = Duration.between(o2.getArriveTime(), o2.getDueTime());
                
                // Orders with shorter time windows have higher priority (higher criticality)
                return window1.compareTo(window2);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Finds a depot at a specific position.
     * 
     * @param position The position to look for
     * @return The depot at the position, or null if none exists
     */
    private Depot findDepotByPosition(Position position) {
        for (Depot depot : depots) {
            if (depot.getPosition().equals(position)) {
                return depot;
            }
        }
        return null;
    }
    
    /**
     * Calculates the distance of a path.
     * 
     * @param path List of positions representing the path
     * @return The total distance of the path
     */
    private double calculateDistance(List<Position> path) {
        double distance = 0.0;
        
        for (int i = 0; i < path.size() - 1; i++) {
            distance += path.get(i).distanceTo(path.get(i + 1));
        }
        
        return distance;
    }
    
    /**
     * Calculates the arrival time based on distance and speed.
     * 
     * @param startTime Starting time
     * @param distanceKm Distance in kilometers
     * @return The arrival time
     */
    private LocalDateTime calculateArrivalTime(LocalDateTime startTime, double distanceKm) {
        double timeInHours = distanceKm / VEHICLE_AVG_SPEED;
        long timeInSeconds = (long) (timeInHours * 3600);
        return startTime.plusSeconds(timeInSeconds);
    }
    
    /**
     * Helper class to store the result of a route insertion.
     */
    private static class InsertionResult {
        final String orderId;
        final OrderStop orderStop;
        final int insertionPosition;
        final int glpDelivery;
        final DepotStop refuelStop;
        final int refuelPosition;
        
        InsertionResult(
                String orderId,
                OrderStop orderStop,
                int insertionPosition,
                int glpDelivery,
                DepotStop refuelStop,
                int refuelPosition) {
            this.orderId = orderId;
            this.orderStop = orderStop;
            this.insertionPosition = insertionPosition;
            this.glpDelivery = glpDelivery;
            this.refuelStop = refuelStop;
            this.refuelPosition = refuelPosition;
        }
    }
    
    /**
     * Helper class to store insertion cost information.
     */
    private static class InsertionCost {
        final double totalCost;
        final LocalDateTime arrivalTime;
        final boolean needsRefuel;
        
        InsertionCost(double totalCost, LocalDateTime arrivalTime, boolean needsRefuel) {
            this.totalCost = totalCost;
            this.arrivalTime = arrivalTime;
            this.needsRefuel = needsRefuel;
        }
    }
}
