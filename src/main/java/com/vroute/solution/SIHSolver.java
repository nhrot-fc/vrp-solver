package com.vroute.solution;

import com.vroute.models.*;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sequential Insertion Heuristic (SIH) implementation for the V-Route problem.
 * This heuristic builds routes one by one, inserting orders based on a cost
 * function
 * that considers distance, time constraints, and vehicle limitations.
 */
public class SIHSolver {

    private static final double ALPHA = 0.6; // Weight for distance increase
    private static final double BETA = 0.3; // Weight for time delay increase
    private static final double GAMMA = 0.1; // Weight for waiting time increase

    private final Environment environment;
    private final List<Vehicle> availableVehicles;
    private final List<Depot> depots;
    private final LocalDateTime currentDateTime;

    public SIHSolver(Environment environment, List<Vehicle> availableVehicles, List<Depot> depots,
            LocalDateTime currentDateTime) {
        this.environment = environment;
        this.availableVehicles = new ArrayList<>(availableVehicles);
        this.depots = depots;
        this.currentDateTime = currentDateTime;
    }

    public Solution solve(Map<String, Order> orders) {
        if (orders.isEmpty()) {
            return new Solution(orders, Collections.emptyList());
        }

        Map<String, Order> remainingOrders = new HashMap<>(orders);
        List<Route> routes = new ArrayList<>();
        List<Vehicle> vehicles = new ArrayList<>(availableVehicles);
        List<Order> sortedOrders = sortOrdersByCriticality(remainingOrders.values());
        while (!sortedOrders.isEmpty() && !vehicles.isEmpty()) {
            Vehicle vehicle = vehicles.remove(0);
            Route route = constructRoute(vehicle, sortedOrders, remainingOrders);

            if (route != null) {
                routes.add(route);
                sortedOrders = sortedOrders.stream()
                        .filter(order -> remainingOrders.containsKey(order.getId()))
                        .collect(Collectors.toList());
            }
        }

        return new Solution(orders, routes);
    }

    private Route constructRoute(Vehicle vehicle, List<Order> sortedOrders, Map<String, Order> remainingOrders) {
        String routeId = "R" + UUID.randomUUID().toString().substring(0, 8);

        Vehicle simulatedVehicle = vehicle.clone();
        Position startPosition = simulatedVehicle.getCurrentPosition();
        Depot startDepot = findDepotByPosition(startPosition);

        if (startDepot == null) {
            return null;
        }

        List<RouteStop> stops = new ArrayList<>();
        LocalDateTime currentTime = currentDateTime;

        Order seedOrder = findSeedOrder(sortedOrders, simulatedVehicle, startDepot, currentTime);
        if (seedOrder == null) {
            return null;
        }

        PathResult pathResult = PathFinder.findPath(
                environment,
                startPosition,
                seedOrder.getPosition(),
                currentTime);

        if (!pathResult.isPathFound()) {
            return null;
        }

        double distanceToSeed = pathResult.getTotalDistance();
        LocalDateTime arrivalTimeAtSeed = pathResult.getArrivalTime();

        if (arrivalTimeAtSeed.isBefore(seedOrder.getArriveTime())) {
            arrivalTimeAtSeed = seedOrder.getArriveTime();
        }

        double fuelConsumed = simulatedVehicle.calculateFuelNeeded(distanceToSeed);
        if (fuelConsumed > simulatedVehicle.getCurrentFuelGal()) {
            return null;
        }
        simulatedVehicle.consumeFuel(distanceToSeed);

        int glpDelivery = seedOrder.getRemainingGlpM3();
        if (glpDelivery > simulatedVehicle.getCurrentGlpM3()) {
            return null;
        }
        simulatedVehicle.dispenseGlp(glpDelivery);

        OrderStop seedStop = new OrderStop(
                seedOrder.getId(),
                seedOrder.getPosition(),
                arrivalTimeAtSeed,
                glpDelivery);

        stops.add(seedStop);

        remainingOrders.remove(seedOrder.getId());

        currentTime = arrivalTimeAtSeed.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);

        boolean moreInsertionsPossible = true;

        while (moreInsertionsPossible) {
            InsertionResult bestInsertion = findBestInsertion(
                    simulatedVehicle,
                    stops,
                    remainingOrders,
                    sortedOrders,
                    currentTime);

            if (bestInsertion != null) {
                stops.add(bestInsertion.insertionPosition, bestInsertion.orderStop);

                if (bestInsertion.refuelStop != null) {
                    stops.add(bestInsertion.refuelPosition, bestInsertion.refuelStop);
                }

                remainingOrders.remove(bestInsertion.orderId);
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

        // Try to add a final stop back to the main depot
        DepotStop finalDepotStop = findFinalDepotStop(
                simulatedVehicle,
                stops.get(stops.size() - 1).getPosition(),
                stops.get(stops.size() - 1).getArrivalTime().plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES));

        // If we can't return to the main depot, the route is invalid
        if (finalDepotStop == null) {
            // Put all orders back in the remaining orders
            for (RouteStop stop : stops) {
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    Order order = findOrderById(orderStop.getEntityID(), sortedOrders);
                    if (order != null) {
                        remainingOrders.put(order.getId(), order);
                    }
                }
            }
            return null;
        }

        stops.add(finalDepotStop);
        return new Route(routeId, vehicle, stops);
    }

    private Order findSeedOrder(List<Order> sortedOrders, Vehicle vehicle, Depot startDepot,
            LocalDateTime currentTime) {
        Depot mainDepot = environment.getMainDepot();

        for (Order order : sortedOrders) {
            // Calculate path to the order
            PathResult pathToOrder = PathFinder.findPath(
                    environment,
                    startDepot.getPosition(),
                    order.getPosition(),
                    currentTime);

            if (!pathToOrder.isPathFound()) {
                continue; // No path to this order
            }

            double distanceToOrder = pathToOrder.getTotalDistance();

            // Check if vehicle has enough fuel to reach the order
            double fuelToOrder = vehicle.calculateFuelNeeded(distanceToOrder);
            if (fuelToOrder > vehicle.getCurrentFuelGal()) {
                continue; // Not enough fuel
            }

            // Check if vehicle has enough GLP to serve the order
            if (order.getRemainingGlpM3() > vehicle.getCurrentGlpM3()) {
                continue; // Not enough GLP
            }

            // Calculate arrival time at the order
            LocalDateTime arrivalAtOrder = pathToOrder.getArrivalTime();
            if (arrivalAtOrder.isAfter(order.getDueTime())) {
                continue; // Too late
            }

            // Check if the vehicle can return to the main depot after serving this order
            LocalDateTime departureFromOrder = arrivalAtOrder.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
            PathResult pathToMainDepot = PathFinder.findPath(
                    environment,
                    order.getPosition(),
                    mainDepot.getPosition(),
                    departureFromOrder);

            if (!pathToMainDepot.isPathFound()) {
                continue; // No path back to main depot
            }

            // Calculate remaining fuel after reaching the order
            double remainingFuel = vehicle.getCurrentFuelGal() - fuelToOrder;

            // Check if there's enough fuel to return to main depot
            double fuelToMainDepot = vehicle.calculateFuelNeeded(pathToMainDepot.getTotalDistance());
            if (fuelToMainDepot > remainingFuel) {
                continue; // Not enough fuel to return to main depot
            }

            return order; // Found a feasible seed order
        }

        return null; // No feasible seed found
    }

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
                        remainingOrders);

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
                                order.getPosition());

                        if (bestDepot != null) {
                            PathResult refuelPath = PathFinder.findPath(
                                    environment,
                                    currentPosition,
                                    bestDepot.getPosition(),
                                    currentTime);

                            LocalDateTime refuelArrivalTime = refuelPath.isPathFound()
                                    ? refuelPath.getArrivalTime()
                                    : currentTime;

                            refuelStop = new DepotStop(
                                    bestDepot,
                                    refuelArrivalTime,
                                    vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3());
                        }
                    }

                    // Create the order stop
                    OrderStop orderStop = new OrderStop(
                            order.getId(),
                            order.getPosition(),
                            cost.arrivalTime,
                            order.getRemainingGlpM3());

                    bestInsertion = new InsertionResult(
                            order.getId(),
                            orderStop,
                            i,
                            order.getRemainingGlpM3(),
                            refuelStop,
                            refuelPosition);
                }
            }
        }

        return bestInsertion;
    }

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

        // Calculate path from previous position to order
        PathResult pathToPrev = PathFinder.findPath(
                environment,
                prevPosition,
                order.getPosition(),
                currentTime);

        if (!pathToPrev.isPathFound()) {
            return null; // No path to the order
        }

        double distanceToPrev = pathToPrev.getTotalDistance();
        LocalDateTime arrivalTime = pathToPrev.getArrivalTime();

        // Calculate path from order to next position if exists
        double distanceToNext = 0;
        double directDistance = 0;

        if (nextPosition != null) {
            PathResult pathToNext = PathFinder.findPath(
                    environment,
                    order.getPosition(),
                    nextPosition,
                    arrivalTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES));

            if (!pathToNext.isPathFound()) {
                return null; // No path to the next position
            }

            distanceToNext = pathToNext.getTotalDistance();

            // Calculate direct path from prev to next
            PathResult directPath = PathFinder.findPath(
                    environment,
                    prevPosition,
                    nextPosition,
                    currentTime);

            if (directPath.isPathFound()) {
                directDistance = directPath.getTotalDistance();
            } else {
                return null; // Cannot calculate detour cost
            }
        }

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
        LocalDateTime departureTime = arrivalTime.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
        long totalDelay = 0;

        if (nextPosition != null) {
            // Calculate how the insertion affects the timing of subsequent stops
            LocalDateTime newNextArrival = nextPosition != null
                    ? PathFinder.findPath(environment, order.getPosition(), nextPosition, departureTime)
                            .getArrivalTime()
                    : departureTime;

            LocalDateTime oldNextArrival = stops.get(insertPosition).getArrivalTime();

            if (newNextArrival.isAfter(oldNextArrival)) {
                Duration delay = Duration.between(oldNextArrival, newNextArrival);
                totalDelay = delay.toMinutes();

                // Check if the delay causes any time window violations for subsequent stops
                LocalDateTime currentArrival = newNextArrival;
                Position lastPos = nextPosition;

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
                        Position nextPos = stops.get(i + 1).getPosition();
                        PathResult nextPath = PathFinder.findPath(
                                environment,
                                lastPos,
                                nextPos,
                                currentArrival.plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES));

                        if (!nextPath.isPathFound()) {
                            return null; // No path to next stop with the delay
                        }

                        currentArrival = nextPath.getArrivalTime();
                        lastPos = nextPos;
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

    private DepotStop findFinalDepotStop(
            Vehicle vehicle,
            Position currentPosition,
            LocalDateTime currentTime) {

        Depot mainDepot = environment.getMainDepot();

        PathResult pathResult = PathFinder.findPath(
                environment,
                currentPosition,
                mainDepot.getPosition(),
                currentTime);

        if (!pathResult.isPathFound()) {
            return null; // No path to main depot
        }

        double distance = pathResult.getTotalDistance();
        LocalDateTime arrivalTime = pathResult.getArrivalTime();

        // Check if vehicle has enough fuel to reach the main depot
        double fuelNeeded = vehicle.calculateFuelNeeded(distance);
        if (fuelNeeded > vehicle.getCurrentFuelGal()) {
            return null; // Not enough fuel to reach main depot
        }

        return new DepotStop(
                mainDepot,
                arrivalTime,
                0 // No GLP recharge needed at the end of the route
        );
    }

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

    private Depot findDepotByPosition(Position position) {
        for (Depot depot : depots) {
            if (depot.getPosition().equals(position)) {
                return depot;
            }
        }
        return null;
    }

    private Order findOrderById(String orderId, List<Order> orders) {
        for (Order order : orders) {
            if (order.getId().equals(orderId)) {
                return order;
            }
        }
        return null;
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
