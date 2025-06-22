package com.vroute.solution;

import com.vroute.models.*;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sequential Insertion Heuristic (SIH) implementation for the V-Route problem.
 * This heuristic builds routes one by one, inserting orders based on a cost
 * function
 * that considers distance, time constraints, and vehicle limitations.
 */
public class SIHSolver implements Solver {

    private static final double ALPHA = 0.6; // Weight for distance increase
    private static final double BETA = 0.3; // Weight for time delay increase
    private static final double GAMMA = 0.1; // Weight for waiting time increase

    // Constantes temporales para el SIHSolver
    private static final double AVERAGE_SPEED_KM_H = Constants.VEHICLE_AVG_SPEED;
    private static final int SERVICE_TIME_MINUTES = Constants.GLP_SERVE_DURATION_MINUTES;

    @Override
    public Solution solve(Environment environment) {
        Map<String, Order> pendingOrders = new HashMap<>();
        for (Order order : environment.getPendingOrders()) {
            pendingOrders.put(order.getId(), order);
        }

        if (pendingOrders.isEmpty()) {
            return new Solution(pendingOrders, Collections.emptyList());
        }

        // Save evaluator configuration
        boolean prevAllowPartial = Evaluator.isAllowPartialGlpDelivery();
        Evaluator.setAllowPartialGlpDelivery(true);

        try {
            // First attempt: Try to create a solution with the standard algorithm
            Solution solution = createSolutionWithStandardAlgorithm(environment, pendingOrders);

            // Check if we have any valid routes
            if (!solution.getRoutes().isEmpty()) {
                return solution;
            }

            // Second attempt: Try with a simpler approach for feasibility
            return createFallbackSolution(environment, pendingOrders);
        } finally {
            // Restore evaluator configuration
            Evaluator.setAllowPartialGlpDelivery(prevAllowPartial);
        }
    }

    /**
     * Creates a solution using the standard SIH algorithm
     */
    private Solution createSolutionWithStandardAlgorithm(Environment environment, Map<String, Order> pendingOrders) {
        Map<String, Order> remainingOrders = new HashMap<>(pendingOrders);
        List<Route> routes = new ArrayList<>();
        List<Vehicle> vehicles = new ArrayList<>(environment.getAvailableVehicles());
        List<Order> sortedOrders = sortOrdersByCriticality(remainingOrders.values());

        while (!sortedOrders.isEmpty() && !vehicles.isEmpty()) {
            Vehicle vehicle = vehicles.remove(0);
            Route route = constructRoute(environment, vehicle, sortedOrders, remainingOrders);

            if (route != null) {
                routes.add(route);
                sortedOrders = sortedOrders.stream()
                        .filter(order -> remainingOrders.containsKey(order.getId()))
                        .collect(Collectors.toList());
            }
        }

        // Validate all routes with the Evaluator
        List<Route> validRoutes = new ArrayList<>();
        for (Route route : routes) {
            if (Evaluator.isRouteValid(route, pendingOrders)) {
                validRoutes.add(route);
            }
        }

        return new Solution(pendingOrders, validRoutes);
    }

    /**
     * Creates a fallback solution when the standard algorithm fails
     * This is a simpler approach focused on feasibility
     */
    private Solution createFallbackSolution(Environment environment, Map<String, Order> pendingOrders) {
        List<Route> routes = new ArrayList<>();
        List<Vehicle> vehicles = new ArrayList<>(environment.getAvailableVehicles());

        // Sort orders by due time (earliest first)
        List<Order> sortedOrders = new ArrayList<>(pendingOrders.values());
        sortedOrders.sort(Comparator.comparing(Order::getDueTime));

        // For each vehicle, try to create a simple route
        for (Vehicle vehicle : vehicles) {
            if (sortedOrders.isEmpty()) {
                break;
            }

            // Clone the vehicle to work with
            Vehicle vehicleClone = vehicle.clone();
            List<RouteStop> stops = new ArrayList<>();
            LocalDateTime currentTime = environment.getCurrentTime();
            final Position initialPosition = vehicleClone.getCurrentPosition();
            Position currentPosition = initialPosition;

            // First, check if we need to refuel at a depot
            if (vehicleClone.getCurrentGlpM3() < vehicleClone.getGlpCapacityM3() / 2) {
                Depot depot = findNearestDepot(currentPosition, environment.getDepots());
                if (depot != null) {
                    // Calculate travel time to depot
                    double distanceToDepot = currentPosition.distanceTo(depot.getPosition());
                    double travelTimeHours = distanceToDepot / AVERAGE_SPEED_KM_H;
                    LocalDateTime arrivalTime = currentTime.plusMinutes((long) (travelTimeHours * 60));

                    // Calculate GLP to recharge
                    int glpToRecharge = Math.min(
                            vehicleClone.getGlpCapacityM3() - vehicleClone.getCurrentGlpM3(),
                            depot.getCurrentGlpM3());

                    // Add depot stop
                    DepotStop depotStop = new DepotStop(depot, arrivalTime, glpToRecharge);
                    stops.add(depotStop);

                    // Update vehicle state
                    vehicleClone.refill(glpToRecharge);
                    vehicleClone.setCurrentFuelGal(vehicleClone.getFuelCapacityGal()); // Full refuel
                    currentPosition = depot.getPosition();
                    currentTime = arrivalTime.plusMinutes(SERVICE_TIME_MINUTES);
                }
            }

            // Create a list of orders this vehicle can handle
            List<Order> feasibleOrders = new ArrayList<>();
            for (Order order : sortedOrders) {
                // Calculate if we can reach this order
                double distanceToOrder = currentPosition.distanceTo(order.getPosition());
                double fuelNeeded = vehicleClone.calculateFuelNeeded(distanceToOrder);

                if (fuelNeeded <= vehicleClone.getCurrentFuelGal()) {
                    feasibleOrders.add(order);

                    // Limit to 3 orders per vehicle for simplicity
                    if (feasibleOrders.size() >= 3) {
                        break;
                    }
                }
            }

            // If we found feasible orders, create stops for them
            if (!feasibleOrders.isEmpty()) {
                // Sort by proximity to current position
                final Position sortPosition = currentPosition; // Create a final copy for the lambda
                feasibleOrders.sort((o1, o2) -> Double.compare(
                        sortPosition.distanceTo(o1.getPosition()),
                        sortPosition.distanceTo(o2.getPosition())));

                // Add order stops
                for (Order order : feasibleOrders) {
                    // Calculate travel time
                    double distanceToOrder = currentPosition.distanceTo(order.getPosition());
                    double travelTimeHours = distanceToOrder / AVERAGE_SPEED_KM_H;
                    LocalDateTime arrivalTime = currentTime.plusMinutes((long) (travelTimeHours * 60));

                    // Calculate GLP to deliver (partial delivery if needed)
                    int glpToDeliver = Math.min(vehicleClone.getCurrentGlpM3(), order.getRemainingGlpM3());

                    // Add order stop
                    OrderStop orderStop = new OrderStop(order.getId(), order.getPosition(), arrivalTime, glpToDeliver);
                    stops.add(orderStop);

                    // Update vehicle state
                    vehicleClone.dispenseGlp(glpToDeliver);
                    vehicleClone.consumeFuel(vehicleClone.calculateFuelNeeded(distanceToOrder));
                    currentPosition = order.getPosition();
                    currentTime = arrivalTime.plusMinutes(SERVICE_TIME_MINUTES);

                    // Remove this order from the sorted list
                    sortedOrders.remove(order);

                    // Check if we need to refuel at a depot
                    if (vehicleClone.getCurrentGlpM3() < 5 && !sortedOrders.isEmpty()) {
                        Depot depot = findNearestDepot(currentPosition, environment.getDepots());
                        if (depot != null) {
                            // Calculate travel time to depot
                            double distanceToDepot = currentPosition.distanceTo(depot.getPosition());
                            double depotTravelTimeHours = distanceToDepot / AVERAGE_SPEED_KM_H;
                            LocalDateTime depotArrivalTime = currentTime
                                    .plusMinutes((long) (depotTravelTimeHours * 60));

                            // Calculate GLP to recharge
                            int glpToRecharge = Math.min(
                                    vehicleClone.getGlpCapacityM3() - vehicleClone.getCurrentGlpM3(),
                                    depot.getCurrentGlpM3());

                            // Add depot stop
                            DepotStop depotStop = new DepotStop(depot, depotArrivalTime, glpToRecharge);
                            stops.add(depotStop);

                            // Update vehicle state
                            vehicleClone.refill(glpToRecharge);
                            vehicleClone.setCurrentFuelGal(vehicleClone.getFuelCapacityGal()); // Full refuel
                            currentPosition = depot.getPosition();
                            currentTime = depotArrivalTime.plusMinutes(SERVICE_TIME_MINUTES);
                        }
                    }
                }

                // Create route if we have stops
                if (!stops.isEmpty()) {
                    Route route = new Route(vehicle.getId(), vehicle, stops);

                    // Only add if the route is valid according to the Evaluator
                    if (Evaluator.isRouteValid(route, pendingOrders)) {
                        routes.add(route);
                    }
                }
            }
        }

        return new Solution(pendingOrders, routes);
    }

    private Route constructRoute(Environment environment, Vehicle vehicle, List<Order> sortedOrders,
            Map<String, Order> remainingOrders) {
        String routeId = "R" + UUID.randomUUID().toString().substring(0, 8);

        Vehicle simulatedVehicle = vehicle.clone();
        Position startPosition = simulatedVehicle.getCurrentPosition();
        Depot startDepot = findDepotByPosition(environment, startPosition);

        if (startDepot == null) {
            return null;
        }

        List<RouteStop> stops = new ArrayList<>();
        LocalDateTime currentTime = environment.getCurrentTime();

        Order seedOrder = findSeedOrder(environment, sortedOrders, simulatedVehicle, startDepot, currentTime);
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
                    environment,
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
                environment,
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

    private Order findSeedOrder(Environment environment, List<Order> sortedOrders, Vehicle vehicle, Depot startDepot,
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
            Environment environment,
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
            boolean needsRefuel = false;
            if (order.getRemainingGlpM3() > vehicle.getCurrentGlpM3()) {
                // Check if we can refuel at a depot
                boolean canRefuel = false;
                Depot bestRefuelDepot = null;
                for (Depot depot : environment.getDepots()) {
                    if (depot.canRefuel() && depot.getCurrentGlpM3() >= order.getRemainingGlpM3()) {
                        canRefuel = true;
                        if (bestRefuelDepot == null ||
                                currentPosition.distanceTo(depot.getPosition()) < currentPosition
                                        .distanceTo(bestRefuelDepot.getPosition())) {
                            bestRefuelDepot = depot;
                        }
                    }
                }

                if (canRefuel) {
                    needsRefuel = true;
                } else if (!Evaluator.isAllowPartialGlpDelivery()) {
                    // If we can't refuel and partial delivery is not allowed, skip this order
                    continue;
                }
            }

            // Try to insert at each position
            for (int i = 0; i <= stops.size(); i++) {
                // Calculate cost of inserting the order at position i
                InsertionCost cost = calculateInsertionCost(
                        environment,
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

                    if (cost.needsRefuel || needsRefuel) {
                        // Find best depot for refueling
                        Depot bestDepot = findBestRefuelDepot(
                                environment,
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

                            // Calculate how much GLP to recharge (don't exceed vehicle capacity)
                            int glpToRecharge = Math.min(
                                    vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3(),
                                    bestDepot.getCurrentGlpM3());

                            refuelStop = new DepotStop(
                                    bestDepot,
                                    refuelArrivalTime,
                                    glpToRecharge);
                        }
                    }

                    // Create the order stop
                    // Determine how much GLP we can deliver (partial or full)
                    int glpDelivery = order.getRemainingGlpM3();
                    if (glpDelivery > vehicle.getCurrentGlpM3() && Evaluator.isAllowPartialGlpDelivery()) {
                        glpDelivery = vehicle.getCurrentGlpM3();
                    }

                    OrderStop orderStop = new OrderStop(
                            order.getId(),
                            order.getPosition(),
                            cost.arrivalTime,
                            glpDelivery);

                    bestInsertion = new InsertionResult(
                            order.getId(),
                            orderStop,
                            i,
                            glpDelivery,
                            refuelStop,
                            refuelPosition);
                }
            }
        }

        return bestInsertion;
    }

    private InsertionCost calculateInsertionCost(
            Environment environment,
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
            Depot refuelDepot = findBestRefuelDepot(environment, prevPosition, order.getPosition());
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

    private Depot findBestRefuelDepot(Environment environment, Position fromPosition, Position toPosition) {
        Depot bestDepot = null;
        double bestDeviation = Double.POSITIVE_INFINITY;

        List<Depot> depots = environment.getDepots();

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
            Environment environment,
            Vehicle vehicle,
            Position currentPosition,
            LocalDateTime currentTime) {

        Depot mainDepot = environment.getMainDepot();
        if (mainDepot == null) {
            // If there's no main depot, try to find any depot
            List<Depot> depots = environment.getDepots();
            if (!depots.isEmpty()) {
                mainDepot = depots.get(0);
            } else {
                return null; // No depots available
            }
        }

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

    private Depot findDepotByPosition(Environment environment, Position position) {
        List<Depot> depots = environment.getDepots();

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

    /**
     * Genera una solución inicial simple para los problemas de ruteo
     * Esta función crea rutas basadas en la asignación greedy de órdenes a
     * vehículos
     * 
     * @param orders    Lista de órdenes a ser atendidas
     * @param vehicles  Lista de vehículos disponibles
     * @param depots    Lista de depósitos disponibles
     * @param startTime Hora de inicio de la simulación
     * @param random    Generador de números aleatorios
     * @return Una solución factible (o lo más cercana posible)
     */
    public static Solution generateInitialSolution(
            Map<String, Order> orders,
            List<Vehicle> vehicles,
            List<Depot> depots,
            LocalDateTime startTime,
            Random random) {

        // Clonamos los pedidos y vehículos para no modificar los originales
        Map<String, Order> clonedOrders = new HashMap<>();
        for (Order order : orders.values()) {
            clonedOrders.put(order.getId(), order.clone());
        }

        List<Vehicle> clonedVehicles = new ArrayList<>();
        for (Vehicle v : vehicles) {
            clonedVehicles.add(v.clone());
        }

        // Guardar configuración del evaluador
        boolean prevAllowPartial = Evaluator.isAllowPartialGlpDelivery();
        Evaluator.setAllowPartialGlpDelivery(true);

        try {
            // Intentar crear solución con el algoritmo estándar
            Solution solution = generateSolutionWithStandardAlgorithm(clonedOrders, clonedVehicles, depots, startTime,
                    random);

            // Si no hay rutas válidas, intentar con el algoritmo de fallback
            if (solution.getRoutes().isEmpty() && !clonedOrders.isEmpty() && !clonedVehicles.isEmpty()) {
                solution = generateFallbackSolution(clonedOrders, clonedVehicles, depots, startTime);
            }

            return solution;
        } finally {
            // Restaurar la configuración del evaluador
            Evaluator.setAllowPartialGlpDelivery(prevAllowPartial);
        }
    }

    /**
     * Genera una solución utilizando el algoritmo estándar
     */
    private static Solution generateSolutionWithStandardAlgorithm(
            Map<String, Order> orders,
            List<Vehicle> vehicles,
            List<Depot> depots,
            LocalDateTime startTime,
            Random random) {

        // Creamos un mapa para almacenar las rutas por vehículo
        Map<String, Route> routesByVehicle = new HashMap<>();

        // Creamos una lista de todos los pedidos para procesarlos
        List<Order> ordersList = new ArrayList<>(orders.values());

        // Ordenamos los pedidos por proximidad a la fecha límite
        Collections.sort(ordersList, (o1, o2) -> o1.getDueTime().compareTo(o2.getDueTime()));

        // Asignar órdenes a vehículos
        for (Order order : ordersList) {
            Vehicle bestVehicle = null;
            double minDistance = Double.MAX_VALUE;

            // Buscar el vehículo más cercano al pedido y con suficiente capacidad
            for (Vehicle vehicle : vehicles) {
                if (vehicle.getCurrentGlpM3() >= order.getRemainingGlpM3()) {
                    double distance = vehicle.getCurrentPosition().distanceTo(order.getPosition());
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestVehicle = vehicle;
                    }
                }
            }

            // Si no encontramos vehículo, intentar con uno cualquiera y después ir al depot
            if (bestVehicle == null && !vehicles.isEmpty()) {
                bestVehicle = vehicles.get(random.nextInt(vehicles.size()));
            } else if (bestVehicle == null) {
                // No hay vehículos disponibles
                continue;
            }

            // Obtener o crear la ruta para este vehículo
            Route route = routesByVehicle.getOrDefault(bestVehicle.getId(),
                    new Route(bestVehicle.getId(), bestVehicle, new ArrayList<>()));

            // Si el vehículo no tiene suficiente GLP, agregar una parada en un depósito
            if (bestVehicle.getCurrentGlpM3() < order.getRemainingGlpM3()) {
                // Buscar el depósito más cercano
                Depot nearestDepot = findNearestDepot(bestVehicle.getCurrentPosition(), depots);

                if (nearestDepot != null) {
                    // Calcular tiempo para llegar al depósito
                    double distanceToDepot = bestVehicle.getCurrentPosition().distanceTo(nearestDepot.getPosition());
                    long minutesToDepot = (long) (distanceToDepot / AVERAGE_SPEED_KM_H * 60);
                    LocalDateTime arrivalToDepot = startTime.plus(minutesToDepot, ChronoUnit.MINUTES);

                    // Calcular la recarga de GLP necesaria (sin exceder la capacidad)
                    int rechargeAmount = Math.min(
                            bestVehicle.getGlpCapacityM3() - bestVehicle.getCurrentGlpM3(),
                            nearestDepot.getCurrentGlpM3());

                    // Asegurar que no sea negativo
                    rechargeAmount = Math.max(0, rechargeAmount);

                    // Agregar la parada en el depósito
                    route.getStops().add(new DepotStop(nearestDepot, arrivalToDepot, rechargeAmount));

                    // Actualizar posición y GLP del vehículo
                    bestVehicle.setCurrentPosition(nearestDepot.getPosition());
                    bestVehicle.refill(rechargeAmount);

                    // Actualizar el tiempo de inicio para la siguiente parada
                    startTime = arrivalToDepot.plus(SERVICE_TIME_MINUTES, ChronoUnit.MINUTES);
                }
            }

            // Calcular tiempo para llegar al pedido
            double distanceToOrder = bestVehicle.getCurrentPosition().distanceTo(order.getPosition());
            long minutesToOrder = (long) (distanceToOrder / AVERAGE_SPEED_KM_H * 60);
            LocalDateTime arrivalToOrder = startTime.plus(minutesToOrder, ChronoUnit.MINUTES);

            // Determinar cuánto GLP entregar (completo o parcial)
            int glpToDeliver = Math.min(bestVehicle.getCurrentGlpM3(), order.getRemainingGlpM3());

            // Crear la parada para el pedido
            OrderStop orderStop = new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    arrivalToOrder,
                    glpToDeliver);

            // Agregar parada a la ruta
            route.getStops().add(orderStop);

            // Actualizar GLP del vehículo y posición
            bestVehicle.dispenseGlp(orderStop.getGlpDelivery());
            bestVehicle.setCurrentPosition(order.getPosition());

            // Registrar la entrega al pedido
            order.recordDelivery(orderStop.getGlpDelivery(), bestVehicle.getId(), arrivalToOrder);

            // Actualizar el tiempo para la siguiente parada
            startTime = arrivalToOrder.plus(SERVICE_TIME_MINUTES, ChronoUnit.MINUTES);

            // Guardar la ruta actualizada
            routesByVehicle.put(bestVehicle.getId(), route);
        }

        // Convertir el mapa de rutas a una lista
        List<Route> routes = new ArrayList<>(routesByVehicle.values());

        // Validar las rutas con el evaluador
        List<Route> validRoutes = new ArrayList<>();
        for (Route route : routes) {
            if (Evaluator.isRouteValid(route, orders)) {
                validRoutes.add(route);
            }
        }

        return new Solution(orders, validRoutes);
    }

    /**
     * Genera una solución de respaldo cuando el algoritmo estándar falla
     */
    private static Solution generateFallbackSolution(
            Map<String, Order> orders,
            List<Vehicle> vehicles,
            List<Depot> depots,
            LocalDateTime startTime) {

        List<Route> routes = new ArrayList<>();

        // Intentar crear una ruta simple para cada vehículo
        for (Vehicle vehicle : vehicles) {
            List<RouteStop> stops = new ArrayList<>();

            // Encontrar el depósito principal o el más cercano
            Depot mainDepot = null;
            for (Depot depot : depots) {
                if (depot.isMainDepot()) {
                    mainDepot = depot;
                    break;
                }
            }

            if (mainDepot == null && !depots.isEmpty()) {
                mainDepot = depots.get(0);
            }

            if (mainDepot != null) {
                // Calcular tiempo para llegar al depósito
                double distanceToDepot = vehicle.getCurrentPosition().distanceTo(mainDepot.getPosition());
                long minutesToDepot = (long) (distanceToDepot / AVERAGE_SPEED_KM_H * 60);
                LocalDateTime arrivalToDepot = startTime.plus(minutesToDepot, ChronoUnit.MINUTES);

                // Calcular la recarga de GLP (llenar al máximo)
                int rechargeAmount = Math.min(
                        vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3(),
                        mainDepot.getCurrentGlpM3());

                // Agregar la parada en el depósito
                DepotStop depotStop = new DepotStop(mainDepot, arrivalToDepot, rechargeAmount);
                stops.add(depotStop);

                // Buscar un pedido cercano al depósito
                Order closestOrder = null;
                double minDistance = Double.MAX_VALUE;

                for (Order order : orders.values()) {
                    double distance = mainDepot.getPosition().distanceTo(order.getPosition());
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestOrder = order;
                    }
                }

                if (closestOrder != null) {
                    // Calcular tiempo para llegar al pedido
                    double distanceToOrder = mainDepot.getPosition().distanceTo(closestOrder.getPosition());
                    long minutesToOrder = (long) (distanceToOrder / AVERAGE_SPEED_KM_H * 60);
                    LocalDateTime arrivalToOrder = arrivalToDepot.plus(minutesToOrder + SERVICE_TIME_MINUTES,
                            ChronoUnit.MINUTES);

                    // Determinar cuánto GLP entregar (después de recargar)
                    int glpToDeliver = Math.min(
                            vehicle.getCurrentGlpM3() + rechargeAmount,
                            closestOrder.getRemainingGlpM3());

                    // Crear la parada para el pedido
                    OrderStop orderStop = new OrderStop(
                            closestOrder.getId(),
                            closestOrder.getPosition(),
                            arrivalToOrder,
                            glpToDeliver);

                    // Agregar parada a la ruta
                    stops.add(orderStop);
                }

                if (stops.size() > 1) { // Solo crear la ruta si tiene al menos un depot y un pedido
                    Route route = new Route(vehicle.getId(), vehicle, stops);

                    // Solo agregar si la ruta es válida
                    if (Evaluator.isRouteValid(route, orders)) {
                        routes.add(route);
                    }
                }
            }
        }

        return new Solution(orders, routes);
    }

    private static Depot findNearestDepot(Position position, List<Depot> depots) {
        Depot nearest = depots.get(0);
        double minDistance = position.distanceTo(nearest.getPosition());

        for (int i = 1; i < depots.size(); i++) {
            Depot depot = depots.get(i);
            double distance = position.distanceTo(depot.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = depot;
            }
        }

        return nearest;
    }
}
