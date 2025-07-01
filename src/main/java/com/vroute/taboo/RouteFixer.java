package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.pathfinding.PathFinder;
import com.vroute.pathfinding.PathResult;
import com.vroute.solution.DepotStop;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;

public class RouteFixer {
    private static void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[RouteFixer] " + message);
        }
    }

    public static Route fixRoute(Environment env, List<OrderStop> orderStops, Vehicle vehicle, LocalDateTime starTime) {
        List<RouteStop> finalRoute = new ArrayList<>();
       
        Vehicle currentVehicle = vehicle.clone();
        LocalDateTime currentTime = starTime;
        
        for(OrderStop os: orderStops) {
            if(os.getGlpDelivery() > vehicle.getGlpCapacityM3()) {
                debug(String.format("%s is impossible to serve: glp needed %d, vehicle capacity %d", os.getOrder().getId(), os.getGlpDelivery(), vehicle.getGlpCapacityM3()));
                return null;
            }
            // Case: Vehicle has not enough GLP to attend OrderStop
            // vehicle.currentPosition -> depot
            if (currentVehicle.getCurrentGlpM3() < os.getGlpDelivery()) {
                // Find nearest depot with enough GLP
                int glpNeeded = currentVehicle.getGlpCapacityM3() - currentVehicle.getCurrentGlpM3();
                Depot glpDepot = findNearestGLPSource(env, currentVehicle.getCurrentPosition(), glpNeeded, currentTime);
                
                if (glpDepot == null) {
                    debug("No depot with enough GLP found");
                    return null;
                }
                
                // Travel to the depot
                TravelResult travelToDepot = driveTo(currentVehicle.getCurrentPosition(), 
                                                   new DepotStop(glpDepot, glpNeeded), 
                                                   currentTime, 
                                                   env, 
                                                   currentVehicle);
                
                if (travelToDepot == null) {
                    debug("Impossible to travel to depot");
                    return null;
                }
                
                // Update vehicle state after reaching depot
                currentVehicle = travelToDepot.getUpdatedVehicle();
                currentTime = travelToDepot.getEta().plusMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
                
                // Refill GLP at depot
                currentVehicle.refill(glpNeeded);
                
                // Add depot stop to route
                finalRoute.addAll(travelToDepot.getVisitedStops());
                
                // Now travel from depot to order
                TravelResult travelToOrder = driveTo(currentVehicle.getCurrentPosition(), 
                                                   os, 
                                                   currentTime, 
                                                   env, 
                                                   currentVehicle);
                
                if (travelToOrder == null) {
                    debug("Impossible to travel from depot to order");
                    return null;
                }
                
                // Update vehicle state after serving order
                currentVehicle = travelToOrder.getUpdatedVehicle();
                currentVehicle.setCurrentGlpM3(currentVehicle.getCurrentGlpM3() - os.getGlpDelivery());
                currentTime = travelToOrder.getEta().plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
                
                // Add order stop to route
                OrderStop newOs = new OrderStop(os.getOrder(), os.getOrder().getPosition(), os.getGlpDelivery());
                newOs.setPath(travelToOrder.getVisitedStops().getLast().getPath());
                finalRoute.add(newOs);
            }
            // Vehicle has enough GLP to attend OrderStop
            // vehicle.currentPsition -> os
            else if (currentVehicle.getCurrentGlpM3() >= os.getGlpDelivery()) {
                // drive to order stop. serve. add orderstop
                TravelResult travel = driveTo(currentVehicle.getCurrentPosition(), os, currentTime, env, currentVehicle);
                if (travel == null) {
                    debug("Impossible to travel");
                    return null;
                }
                currentVehicle = travel.getUpdatedVehicle();
                currentVehicle.setCurrentGlpM3(currentVehicle.getCurrentGlpM3() - os.getGlpDelivery());
                currentTime = travel.getEta().plusMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
                OrderStop newOs = new OrderStop(os.getOrder(), os.getOrder().getPosition(), os.getGlpDelivery());
                newOs.setPath(travel.getVisitedStops().getLast().getPath());
                finalRoute.add(newOs);
            }
        }

        // Add main depot travel
        DepotStop depotStop = new DepotStop(env.getMainDepot(), 0);
        TravelResult travelToDepot = driveTo(currentVehicle.getCurrentPosition(), depotStop, currentTime, env, currentVehicle);
        if (travelToDepot == null) {
            debug("Impossible to travel to depot");
            return null;
        }
        currentVehicle = travelToDepot.getUpdatedVehicle();
        currentTime = travelToDepot.getEta();
        finalRoute.addAll(travelToDepot.getVisitedStops());
        return new Route(vehicle, finalRoute, starTime);
    }
    private static class TravelResult {
        private final List<RouteStop> visitedStops;
        private final Vehicle updatedVehicle;
        private final LocalDateTime eta;

        public TravelResult(List<RouteStop> visitedStops, Vehicle updatedVehicle, LocalDateTime eta) {
            this.visitedStops = visitedStops;
            this.updatedVehicle = updatedVehicle;
            this.eta = eta;
        }

        public List<RouteStop> getVisitedStops() {
            return visitedStops;
        }

        public Vehicle getUpdatedVehicle() {
            return updatedVehicle;
        }

        public LocalDateTime getEta() {
            return eta;
        }
    }

    private static TravelResult driveTo(Position from, RouteStop to, LocalDateTime starTime, Environment env, Vehicle vehicle) {
        if(from.equals(to.getPosition())) {
            return new TravelResult(List.of(to), vehicle, starTime);
        }

        PathResult path = PathFinder.findPath(env, from, to.getPosition(), starTime);
        if (path == null) {
            debug(String.format("No path found from %s to %s", from, to.getPosition()));
            return null;
        }
        int distanceKm = path.getDistance();
        double fuelNeeded = vehicle.calculateFuelNeeded(distanceKm);
        // Case 1: Enough fuel to reach destination
        if ((vehicle.getCurrentFuelGal() - fuelNeeded > 0) || (Math.abs(vehicle.getCurrentFuelGal() - fuelNeeded) < 0.001)) {
            Vehicle currentVehicle = vehicle.clone();
            currentVehicle.consumeFuelFromDistance(distanceKm);
            currentVehicle.setCurrentPosition(to.getPosition());
            RouteStop destination = to.clone();
            destination.setPath(path.getPath());

            LocalDateTime eta = path.getArrivalTimes().getLast();
            return new TravelResult(List.of(destination), currentVehicle, eta);
        }

        // Case 2: Not enough fuel need intermediate depot to refuel
        Depot fuelDepot = findNearestFuelSource(env, from, starTime);
        if (fuelDepot == null) {
            debug("No fuel depot found");
            return null;
        }

        PathResult fuelPath = PathFinder.findPath(env, from, fuelDepot.getPosition(), starTime);
        if (fuelPath == null) {
            debug(String.format("No path found from %s to fuel depot at %s", from, fuelDepot.getPosition()));
            return null;
        }

        distanceKm = fuelPath.getDistance();
        fuelNeeded = vehicle.calculateFuelNeeded(distanceKm);
        
        // Case 2.1: Cannot reach fuel depot
        if (fuelNeeded > vehicle.getCurrentFuelGal()) {
            debug(String.format("Not enough fuel to reach depot: needed %f, have %f", fuelNeeded, vehicle.getCurrentFuelGal()));
            return null;
        }
        
        // Case 2.2: Can reach [from -> depot]
        Vehicle vehicleAtDepot = vehicle.clone();
        vehicleAtDepot.consumeFuelFromDistance(distanceKm);
        vehicleAtDepot.setCurrentPosition(fuelDepot.getPosition());
        vehicleAtDepot.refuel();
        
        // Create depot stop
        DepotStop depotStop = new DepotStop(fuelDepot, 0);
        depotStop.setPath(fuelPath.getPath());
        
        LocalDateTime arrivalAtDepot = fuelPath.getArrivalTimes().getLast();
        LocalDateTime departureFromDepot = arrivalAtDepot.plusMinutes(Constants.REFUEL_DURATION_MINUTES);
        
        // After refuel verify if original destination is reachable
        PathResult pathFromDepot = PathFinder.findPath(env, fuelDepot.getPosition(), to.getPosition(), departureFromDepot);
        if (pathFromDepot == null) {
            debug(String.format("No path found from depot at %s to destination %s", fuelDepot.getPosition(), to.getPosition()));
            return null;
        }
        
        int distanceFromDepotKm = pathFromDepot.getDistance();
        double fuelNeededFromDepot = vehicleAtDepot.calculateFuelNeeded(distanceFromDepotKm);
        
        // Check if we have enough fuel after refueling
        if (fuelNeededFromDepot > vehicleAtDepot.getCurrentFuelGal()) {
            debug(String.format("Not enough fuel after refueling to reach destination: needed %f, have %f", 
                    fuelNeededFromDepot, vehicleAtDepot.getCurrentFuelGal()));
            return null;
        }
        
        // Can reach [depot -> original destination]
        vehicleAtDepot.consumeFuelFromDistance(distanceFromDepotKm);
        vehicleAtDepot.setCurrentPosition(to.getPosition());
        
        RouteStop destination = to.clone();
        destination.setPath(pathFromDepot.getPath());
        
        LocalDateTime finalEta = pathFromDepot.getArrivalTimes().getLast();
        
        // Return the complete travel result with both stops
        List<RouteStop> stops = new ArrayList<>();
        stops.add(depotStop);
        stops.add(destination);
        
        return new TravelResult(stops, vehicleAtDepot, finalEta);
    }

    private static Depot findNearestFuelSource(Environment env, Position pos, LocalDateTime time) {
        List<Depot> fuelDepots = env.getDepots().stream()
                .filter(Depot::canRefuel)
                .collect(Collectors.toList());
        
        return findNearestDepot(env, pos, fuelDepots, time);
    }
    
    private static Depot findNearestGLPSource(Environment env, Position pos, int glpDemand, LocalDateTime time) {
        List<Depot> glpDepots = env.getDepots().stream()
                .filter(d -> d.getCurrentGlpM3() >= glpDemand)
                .collect(Collectors.toList());
        
        return findNearestDepot(env, pos, glpDepots, time);
    }
    
    private static Depot findNearestDepot(Environment env, Position pos, List<Depot> candidates, LocalDateTime time) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        
        Depot nearest = null;
        PathResult shortestPath = null;
        
        for (Depot depot : candidates) {
            PathResult path = PathFinder.findPath(env, pos, depot.getPosition(), time);
            if (path == null) {
                continue;
            }
            if (shortestPath == null || path.getDistance() < shortestPath.getDistance()) {
                shortestPath = path;
                nearest = depot;
            }
        }

        if (nearest == null) return env.getMainDepot();

        return nearest;
    }
}
