package com.vroute.operation;

import com.vroute.assignation.DeliveryInstruction;
import com.vroute.exceptions.InsufficientFuelException;
import com.vroute.exceptions.NoPathFoundException;
import com.vroute.models.*;
import com.vroute.pathfinding.PathFinder;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Rules for Vehicle Plan Generation (derived from README):
 *
 * Operational Constraints:
 * 1. Speed: Constant 50 Km/h (README 2.3).
 * 2. Map: Grid-based, 1 Km node spacing, no diagonals/curves (README 2.1).
 * 3. Hours: 24/7 operations (README 2.3).
 *
 * Fuel Management (Constants.VEHICLE_FUEL_CAPACITY_GAL = 25 Gallons):
 * 4. Capacity: 25 Gallons for all trucks (README 3.1).
 * 5. Consumption: (Distance * (TareWeight + GLPWeight)) /
 * Constants.CONSUMPTION_FACTOR (180) (README 3.2).
 * - Must have fuel for next leg.
 * - If not, refuel at nearest `canRefuel` Depot (fills to max).
 *
 * GLP Management:
 * 6. Capacity: Vehicle-type specific (README 3.1).
 * 7. Supply: Must have enough GLP for current order.
 * - If not, refill at a Depot (Main Plant or Intermediate Tank).
 * - Main Plant (Constants.MAIN_PLANT_ID): Unlimited GLP.
 * - Intermediate Tanks: Limited capacity and refill schedules (README 2.2).
 * - Refilling: Fills vehicle to capacity (or Depot's available GLP if less).
 *
 * Time & Duration:
 * 8. Delivery Window: Orders have a `dueDate` (README 4.2).
 * 9. At Customer: 15 min for GLP discharge (README 3.4).
 * 10. At Depot (Plant): 15 min for "Routine Maintenance to Exit" (README 3.4).
 * 11. Refueling (Fuel): 10 min (assumed). // Constants.REFUEL_DURATION_MINUTES
 * 12. Refilling (GLP): 10 min (assumed). //
 * Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES
 *
 * Routing & Order Fulfillment:
 * 13. End of Plan: Typically at a plant, implied by "Routine Maintenance to
 * Exit".
 * 14. Limits: Do not exceed GLP or fuel capacities.
 *
 * Maintenance (Preventive):
 * 15. Scheduled: If on route when maintenance starts, return to plant (README
 * 3.3).
 *
 * Environment Factors:
 * 16. Blockages: Handled by PathFinder (README 5.1).
 *
 * Planning Methodology:
 * 20. Immutability: All operations use cloned instances.
 */
public class VehiclePlanCreator {

    private final Environment environment;
    private final Depot mainDepot;

    public VehiclePlanCreator(Environment environment) {
        this.environment = environment;
        this.mainDepot = environment.getMainDepot();
    }

    private LocalDateTime vehicleRefuel(Vehicle vehicle, Depot fuelDepot, LocalDateTime currentTime,
            List<VehicleAction> actions) {
        VehicleAction refuelAction = VehicleActionFactory.createRefuelingAction(fuelDepot, vehicle);
        actions.add(refuelAction);
        vehicle.refuel();
        return currentTime.plus(refuelAction.getDuration());
    }

    private LocalDateTime vehicleRefill(Vehicle vehicle, Depot glpDepot, int glpAmountM3, LocalDateTime currentTime,
            List<VehicleAction> actions) {
        VehicleAction refillAction = VehicleActionFactory.createRefillingAction(glpDepot, glpAmountM3);
        actions.add(refillAction);
        vehicle.refill(glpAmountM3);
        return currentTime.plus(refillAction.getDuration());
    }

    private double calculatePathDistance(List<Position> path) {
        if (path == null || path.size() <= 1) {
            return 0.0;
        }
        return (path.size() - 1) * Constants.NODE_DISTANCE;
    }

    private LocalDateTime driveToLocation(Vehicle vehicle, Position destination, LocalDateTime currentTime,
            List<VehicleAction> actions) throws NoPathFoundException, InsufficientFuelException {
        if (vehicle.getCurrentPosition().equals(destination)) {
            return currentTime;
        }

        List<Position> path = PathFinder.findPath(environment, vehicle.getCurrentPosition(), destination, currentTime);
        if (path.isEmpty()) {
            throw new NoPathFoundException("No path found from " + vehicle.getCurrentPosition() + " to " + destination);
        }

        double distanceKm = calculatePathDistance(path);
        double fuelConsumedGal = vehicle.calculateFuelNeeded(distanceKm);

        if (fuelConsumedGal > vehicle.getCurrentFuelGal()) {
            throw new InsufficientFuelException("Not enough fuel to reach destination. Need " +
                    fuelConsumedGal + " gal, but only have " + vehicle.getCurrentFuelGal() + " gal.");
        }

        Duration duration = Duration.ofMinutes((int) (distanceKm / Constants.VEHICLE_AVG_SPEED * 60.0));

        VehicleAction drivingAction = VehicleActionFactory.createDrivingAction(path, duration, fuelConsumedGal);
        actions.add(drivingAction);

        vehicle.setCurrentPosition(destination);
        vehicle.consumeFuel(distanceKm);

        return currentTime.plus(duration);
    }

    private boolean canReach(Vehicle vehicle, Position destination, LocalDateTime currentTime) {
        if (vehicle.getCurrentPosition().equals(destination)) {
            return true;
        }

        List<Position> path = PathFinder.findPath(environment, vehicle.getCurrentPosition(), destination, currentTime);
        if (path.isEmpty()) {
            return false;
        }

        double distanceKm = calculatePathDistance(path);
        double fuelConsumedGal = vehicle.calculateFuelNeeded(distanceKm);

        return vehicle.getCurrentFuelGal() > fuelConsumedGal + Constants.EPSILON;
    }

    private LocalDateTime processFuelSupply(Vehicle vehicle, LocalDateTime currentTime, List<VehicleAction> actions) {
        try {
            Depot fuelDepot = findNearestFuelDepot(environment.getAuxDepots(), vehicle.getCurrentPosition());
            if (!canReach(vehicle, fuelDepot.getPosition(), currentTime)) {
                return null;
            }

            LocalDateTime updatedTime = driveToLocation(vehicle, fuelDepot.getPosition(), currentTime, actions);
            updatedTime = vehicleRefuel(vehicle, fuelDepot, updatedTime, actions);

            return updatedTime;
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to refuel: " + e.getMessage());
            return null;
        }
    }

    private LocalDateTime processGlpSupply(Vehicle vehicle, int glpRequired,
            LocalDateTime currentTime,
            List<VehicleAction> actions) {
        try {
            Depot glpDepot = findNearestGLPDepot(environment.getAuxDepots(), vehicle.getCurrentPosition(), glpRequired);

            if (canReach(vehicle, glpDepot.getPosition(), currentTime)) {
                currentTime = driveToLocation(vehicle, glpDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefill(vehicle, glpDepot, glpRequired, currentTime, actions);
            } else {
                Depot fuelDepot = findNearestFuelDepot(environment.getAuxDepots(), vehicle.getCurrentPosition());
                if (!canReach(vehicle, fuelDepot.getPosition(), currentTime)) {
                    return null;
                }

                currentTime = driveToLocation(vehicle, fuelDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefuel(vehicle, fuelDepot, currentTime, actions);

                if (!canReach(vehicle, glpDepot.getPosition(), currentTime)) {
                    return null;
                }

                currentTime = driveToLocation(vehicle, glpDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefill(vehicle, glpDepot, glpRequired, currentTime, actions);
            }

            if (glpDepot.canRefuel() && vehicle.getCurrentFuelGal() <= vehicle.getFuelCapacityGal() * 0.3) {
                currentTime = vehicleRefuel(vehicle, glpDepot, currentTime, actions);
            }

            return currentTime;
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to complete GLP supply: " + e.getMessage());
            return null;
        }
    }

    public VehiclePlan createPlan(Vehicle vehicle, List<DeliveryInstruction> instructions,
            LocalDateTime planStartTime) {
        try {
            Vehicle currentVehicle = vehicle.clone();
            LocalDateTime currentTime = planStartTime;
            List<VehicleAction> actions = new ArrayList<>();

            for (int i = 0; i < instructions.size(); i++) {
                DeliveryInstruction inst = instructions.get(i);
                Order order = inst.getOriginalOrder().clone();
                Position orderPos = order.getPosition();

                // int glpNeeded = 0;
                // for (int j = i; j < instructions.size(); j++) {
                // DeliveryInstruction nextInst = instructions.get(j);
                // if (vehicle.getCurrentGlpM3() + glpNeeded + nextInst.getGlpAmountToDeliver()
                // <= currentVehicle
                // .getGlpCapacityM3()) {
                // glpNeeded += nextInst.getGlpAmountToDeliver();
                // } else {
                // break;
                // }
                // }

                int glpNeeded = currentVehicle.getGlpCapacityM3() - currentVehicle.getCurrentGlpM3();

                if (currentVehicle.getCurrentGlpM3() < glpNeeded) {
                    LocalDateTime updatedTime = processGlpSupply(currentVehicle, glpNeeded, currentTime, actions);
                    if (updatedTime == null) {
                        return null;
                    }
                    currentTime = updatedTime;
                }

                if (!canReach(currentVehicle, orderPos, currentTime)) {
                    LocalDateTime updatedTime = processFuelSupply(currentVehicle, currentTime, actions);
                    if (updatedTime == null) {
                        return null;
                    }
                    currentTime = updatedTime;

                    if (!canReach(currentVehicle, orderPos, currentTime)) {
                        return null;
                    }
                }

                currentTime = driveToLocation(currentVehicle, orderPos, currentTime, actions);

                VehicleAction servingAction = VehicleActionFactory.createServingAction(
                        orderPos,
                        order,
                        inst.getGlpAmountToDeliver());
                actions.add(servingAction);

                currentVehicle.dispenseGlp(inst.getGlpAmountToDeliver());
                currentTime = currentTime.plus(servingAction.getDuration());
            }

            if (!instructions.isEmpty()) {
                Position mainDepotPos = this.mainDepot.getPosition();

                if (!canReach(currentVehicle, mainDepotPos, currentTime)) {
                    LocalDateTime updatedTime = processFuelSupply(currentVehicle, currentTime, actions);
                    if (updatedTime == null) {
                        return null;
                    }
                    currentTime = updatedTime;

                    if (!canReach(currentVehicle, mainDepotPos, currentTime)) {
                        return null;
                    }
                }

                currentTime = driveToLocation(currentVehicle, mainDepotPos, currentTime, actions);
                VehicleAction maintenanceAction = VehicleActionFactory.createMaintenanceAction(
                        mainDepotPos,
                        Duration.ofMinutes(Constants.ROUTINE_MAINTENANCE_MINUTES));
                actions.add(maintenanceAction);

                currentTime = currentTime.plus(maintenanceAction.getDuration());
            }

            return new VehiclePlan(currentVehicle, actions, planStartTime);
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to create plan: " + e.getMessage());
            return null;
        }
    }

    private Depot findNearestGLPDepot(List<Depot> depots, Position currentPosition, int glpNeeded) {
        final double effectiveGlpNeeded = Math.max(glpNeeded, Constants.EPSILON);
        return depots.stream()
                .filter(depot -> depot.getCurrentGlpM3() >= effectiveGlpNeeded)
                .min((d1, d2) -> Double.compare(currentPosition.distanceTo(d1.getPosition()),
                        currentPosition.distanceTo(d2.getPosition())))
                .orElse(this.mainDepot);
    }

    private Depot findNearestFuelDepot(List<Depot> depots, Position currentPosition) {
        return depots.stream()
                .filter(Depot::canRefuel)
                .min((d1, d2) -> Double.compare(currentPosition.distanceTo(d1.getPosition()),
                        currentPosition.distanceTo(d2.getPosition())))
                .orElse(this.mainDepot);
    }
}