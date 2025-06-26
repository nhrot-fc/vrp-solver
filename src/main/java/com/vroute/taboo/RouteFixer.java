package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private static class TravelResult {
        private final List<DepotStop> visitedDepots;
        private final Vehicle updatedVehicle;
        private final Position destination;
        private final LocalDateTime eta;

        public TravelResult(List<DepotStop> visitedDepots, Vehicle updatedVehicle, Position destination,
                LocalDateTime eta) {
            this.visitedDepots = visitedDepots;
            this.updatedVehicle = updatedVehicle;
            this.destination = destination;
            this.eta = eta;
        }

        public List<DepotStop> getVisitedDepots() {
            return visitedDepots;
        }

        public Vehicle getUpdatedVehicle() {
            return updatedVehicle;
        }

        public Position getDestination() {
            return destination;
        }

        public LocalDateTime getEta() {
            return eta;
        }
    }

    public static TravelResult driveTo(Environment env, Vehicle vehicle, Position destination,
            LocalDateTime currentTime) {
        // Preparar resultado
        List<DepotStop> visitedDepots = new ArrayList<>();
        Vehicle updatedVehicle = vehicle.clone();
        Position currentPosition = updatedVehicle.getCurrentPosition();
        LocalDateTime eta = currentTime;

        // Si ya está en el destino, retornar inmediatamente
        if (currentPosition.equals(destination)) {
            return new TravelResult(visitedDepots, updatedVehicle, destination, eta);
        }

        // Iteramos hasta que lleguemos al destino o determinemos que no es posible
        while (!currentPosition.equals(destination)) {
            // Calcular ruta directa al destino
            PathResult pathResult = PathFinder.findPath(env, currentPosition, destination, eta);
            if (pathResult == null) {
                debug("No se encontró ruta desde " + currentPosition + " hasta " + destination);
                return null; // No hay ruta posible
            }

            // Verificar si hay suficiente combustible para el viaje directo
            int distance = pathResult.getDistance();
            double fuelNeeded = updatedVehicle.calculateFuelNeeded(distance);

            if (updatedVehicle.getCurrentFuelGal() >= fuelNeeded) {
                // Hay suficiente combustible para ir directamente al destino
                updatedVehicle.consumeFuel(distance);
                updatedVehicle.setCurrentPosition(destination);

                // Actualizar la hora de llegada
                eta = pathResult.getArrivalTimes().get(pathResult.getArrivalTimes().size() - 1);
                currentPosition = destination;

                break; // Terminamos el bucle, ya llegamos al destino
            }
            // No hay suficiente combustible, necesitamos repostar

            // Buscar depósito de combustible más cercano
            Depot fuelDepot = findFuelSource(env, currentPosition, eta);
            if (fuelDepot == null) {
                debug("No se encontró depósito de combustible alcanzable");
                return null; // No hay depósito de combustible alcanzable
            }

            // Calcular ruta al depósito
            PathResult depotPathResult = PathFinder.findPath(env, currentPosition, fuelDepot.getPosition(), eta);
            if (depotPathResult == null) {
                debug("No se encontró ruta al depósito de combustible");
                return null; // No hay ruta al depósito
            }

            // Verificar si podemos llegar al depósito con el combustible actual
            int depotDistance = depotPathResult.getDistance();
            double fuelNeededToDepot = updatedVehicle.calculateFuelNeeded(depotDistance);

            if (updatedVehicle.getCurrentFuelGal() < fuelNeededToDepot) {
                debug("No hay suficiente combustible para llegar al depósito más cercano");
                return null; // No podemos ni llegar al depósito
            }

            // Viajamos al depósito
            updatedVehicle.consumeFuel(depotDistance);
            updatedVehicle.setCurrentPosition(fuelDepot.getPosition());

            // Actualizar tiempo y posición
            eta = depotPathResult.getArrivalTimes().get(depotPathResult.getArrivalTimes().size() - 1);
            currentPosition = fuelDepot.getPosition();

            // Repostar en el depósito
            updatedVehicle.refuel();

            // Registrar depósito visitado
            visitedDepots.add(new DepotStop(fuelDepot, eta, 0)); // El 0 indica que solo repostamos combustible

        }

        return new TravelResult(visitedDepots, updatedVehicle, destination, eta);
    }

    public static Route fixRoute(Environment env, List<OrderStop> orderStops, Vehicle vehicle,
            LocalDateTime startTime) {
        // Trabajar con una copia del vehículo para simulación
        Vehicle vehicleClone = vehicle.clone();
        Position currentPosition = vehicleClone.getCurrentPosition();
        LocalDateTime currentTime = startTime;
        List<RouteStop> routeStops = new ArrayList<>();

        // Procesar cada pedido de la ruta
        for (OrderStop orderStop : orderStops) {

            if (orderStop.getGlpDelivery() > vehicleClone.getGlpCapacityM3()) {
                debug("La orden " + orderStop.getEntityID() + " requiere más GLP que el vehículo puede transportar");
                return null;
            }

            // Paso 1: Verificar si necesitamos recargar GLP
            if (vehicleClone.getCurrentGlpM3() < orderStop.getGlpDelivery()) {
                // Calcular cuánto GLP necesitamos recargar
                int glpToRefill = vehicleClone.getGlpCapacityM3() - vehicleClone.getCurrentGlpM3();

                // Buscar un depósito con suficiente GLP
                Depot depot = findGLPSource(env, glpToRefill, currentPosition, currentTime);
                if (depot == null) {
                    debug("No se encontró depósito con suficiente GLP");
                    return null;
                }

                // Viajar al depósito (incluye paradas de combustible si son necesarias)
                TravelResult travelToDepot = driveTo(env, vehicleClone, depot.getPosition(), currentTime);
                if (travelToDepot == null) {
                    debug("No se pudo calcular ruta al depósito");
                    return null;
                }

                // Actualizar estado después del viaje al depósito
                currentTime = travelToDepot.getEta();
                currentPosition = travelToDepot.getDestination();
                vehicleClone = travelToDepot.getUpdatedVehicle();

                // Añadir paradas intermedias (si hubo repostaje de combustible)
                routeStops.addAll(travelToDepot.getVisitedDepots());

                // Recargar GLP
                vehicleClone.refill(glpToRefill);
                if (depot.canRefuel()) {
                    vehicleClone.refuel();
                }

                // Registrar la parada en el depósito de GLP
                routeStops.add(new DepotStop(depot, currentTime, glpToRefill));
            }

            // Paso 2: Viajar al punto de la orden
            TravelResult travelToOrder = driveTo(env, vehicleClone, orderStop.getPosition(), currentTime);
            if (travelToOrder == null) {
                debug("No se pudo calcular ruta a la orden");
                return null;
            }

            // Actualizar estado después del viaje
            currentTime = travelToOrder.getEta();
            currentPosition = travelToOrder.getDestination();
            vehicleClone = travelToOrder.getUpdatedVehicle();

            // Añadir paradas intermedias (si hubo repostaje)
            routeStops.addAll(travelToOrder.getVisitedDepots());

            // Dispensar GLP
            vehicleClone.dispenseGlp(orderStop.getGlpDelivery());

            // Registrar la entrega de la orden
            routeStops.add(new OrderStop(orderStop.getEntityID(), orderStop.getPosition(),
                    currentTime, orderStop.getGlpDelivery()));
        }

        return new Route(vehicleClone, routeStops, startTime);
    }

    public static Depot findGLPSource(Environment env, int glpRequest, Position position, LocalDateTime currentTime) {
        List<Depot> depots = new ArrayList<>(env.getDepots());
        depots.removeIf(depot -> depot.getCurrentGlpM3() < glpRequest);
        Depot nearestDepot = findNearestDepot(env, depots, position, currentTime);
        return nearestDepot;
    }

    public static Depot findFuelSource(Environment env, Position position, LocalDateTime currentTime) {
        List<Depot> depots = new ArrayList<>(env.getDepots());
        depots.removeIf(depot -> !depot.canRefuel());
        Depot nearestDepot = findNearestDepot(env, depots, position, currentTime);
        return nearestDepot;
    }

    public static Depot findNearestDepot(Environment env, List<Depot> depots, Position position,
            LocalDateTime currentTime) {
        if (depots.isEmpty()) {
            return null;
        }

        Depot bestDepot = null;
        double bestDistance = Double.MAX_VALUE;

        for (Depot depot : depots) {
            PathResult pathResult = PathFinder.findPath(env, position, depot.getPosition(), currentTime);

            if (pathResult != null) {
                if (pathResult.getDistance() < bestDistance) {
                    bestDistance = pathResult.getDistance();
                    bestDepot = depot;
                }
            }
        }

        return bestDepot;
    }

    private static void debug(String message) {
        if (Constants.DEBUG) {
            System.out.println("[RouteFixer] " + message);
        }
    }
}
