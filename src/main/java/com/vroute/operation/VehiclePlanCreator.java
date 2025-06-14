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

    private VehiclePlanCreator() {
        // Utility class with static methods only - prevent instantiation
    }

    public static LocalDateTime vehicleRefuel(Vehicle vehicle, Depot fuelDepot, LocalDateTime currentTime,
            List<Action> actions) {
        Action refuelAction = ActionFactory.createRefuelingAction(fuelDepot, vehicle, currentTime);
        actions.add(refuelAction);
        vehicle.refuel();
        return currentTime.plus(refuelAction.getDuration());
    }

    public static LocalDateTime vehicleRefill(Vehicle vehicle, Depot glpDepot, int glpAmountM3,
            LocalDateTime currentTime,
            List<Action> actions) {
        Action refillAction = ActionFactory.createRefillingAction(glpDepot, glpAmountM3, currentTime);
        actions.add(refillAction);
        vehicle.refill(glpAmountM3);
        return currentTime.plus(refillAction.getDuration());
    }

    public static double calculatePathDistance(List<Position> path) {
        if (path == null || path.size() <= 1) {
            return 0.0;
        }
        return (path.size() - 1) * Constants.NODE_DISTANCE;
    }

    public static LocalDateTime driveToLocation(Environment environment, Vehicle vehicle, Position destination,
            LocalDateTime currentTime,
            List<Action> actions) throws NoPathFoundException, InsufficientFuelException {
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

        Action drivingAction = ActionFactory.createDrivingAction(path, fuelConsumedGal, currentTime, currentTime.plus(duration));
        actions.add(drivingAction);

        vehicle.setCurrentPosition(destination);
        vehicle.consumeFuel(distanceKm);

        return currentTime.plus(duration);
    }

    public static boolean canReach(Environment environment, Vehicle vehicle, Position destination,
            LocalDateTime currentTime) {
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

    public static LocalDateTime processFuelSupply(Environment environment, Vehicle vehicle, LocalDateTime currentTime,
            List<Action> actions) {
        try {
            Depot fuelDepot = findNearestFuelDepot(environment.getAuxDepots(), vehicle.getCurrentPosition(),
                    environment.getMainDepot());
            if (!canReach(environment, vehicle, fuelDepot.getPosition(), currentTime)) {
                return null;
            }

            LocalDateTime updatedTime = driveToLocation(environment, vehicle, fuelDepot.getPosition(), currentTime,
                    actions);
            updatedTime = vehicleRefuel(vehicle, fuelDepot, updatedTime, actions);

            return updatedTime;
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to refuel: " + e.getMessage());
            return null;
        }
    }

    public static LocalDateTime processGlpSupply(Environment environment, Vehicle vehicle, int glpRequired,
            LocalDateTime currentTime, List<Action> actions) {
        try {
            Depot glpDepot = findNearestGLPDepot(environment.getAuxDepots(), vehicle.getCurrentPosition(), glpRequired,
                    environment.getMainDepot());

            if (canReach(environment, vehicle, glpDepot.getPosition(), currentTime)) {
                currentTime = driveToLocation(environment, vehicle, glpDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefill(vehicle, glpDepot, glpRequired, currentTime, actions);
            } else {
                Depot fuelDepot = findNearestFuelDepot(environment.getAuxDepots(), vehicle.getCurrentPosition(),
                        environment.getMainDepot());
                if (!canReach(environment, vehicle, fuelDepot.getPosition(), currentTime)) {
                    return null;
                }

                currentTime = driveToLocation(environment, vehicle, fuelDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefuel(vehicle, fuelDepot, currentTime, actions);

                if (!canReach(environment, vehicle, glpDepot.getPosition(), currentTime)) {
                    return null;
                }

                currentTime = driveToLocation(environment, vehicle, glpDepot.getPosition(), currentTime, actions);
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

    public static VehiclePlan createPlan(Environment environment, Vehicle vehicle,
            List<DeliveryInstruction> instructions) {
        try {
            Vehicle currentVehicle = vehicle.clone();
            LocalDateTime planStartTime = environment.getCurrentTime();
            LocalDateTime currentTime = planStartTime;
            List<Action> actions = new ArrayList<>();
            Depot mainDepot = environment.getMainDepot();

            for (int i = 0; i < instructions.size(); i++) {
                DeliveryInstruction inst = instructions.get(i);
                Order order = inst.getOriginalOrder().clone();
                Position orderPos = order.getPosition();
                int glpNeeded = currentVehicle.getGlpCapacityM3() - currentVehicle.getCurrentGlpM3();

                if (currentVehicle.getCurrentGlpM3() < glpNeeded) {
                    LocalDateTime updatedTime = processGlpSupply(environment, currentVehicle, glpNeeded, currentTime,
                            actions);
                    if (updatedTime == null) {
                        return null;
                    }
                    currentTime = updatedTime;
                }

                if (!canReach(environment, currentVehicle, orderPos, currentTime)) {
                    LocalDateTime updatedTime = processFuelSupply(environment, currentVehicle, currentTime, actions);
                    if (updatedTime == null) {
                        return null;
                    }
                    currentTime = updatedTime;

                    if (!canReach(environment, currentVehicle, orderPos, currentTime)) {
                        return null;
                    }
                }

                currentTime = driveToLocation(environment, currentVehicle, orderPos, currentTime, actions);

                Action servingAction = ActionFactory.createServingAction(
                        orderPos,
                        order,
                        inst.getGlpAmountToDeliver(),
                        currentTime);
                actions.add(servingAction);

                currentVehicle.dispenseGlp(inst.getGlpAmountToDeliver());
                currentTime = currentTime.plus(servingAction.getDuration());
            }

            if (!instructions.isEmpty()) {
                Position mainDepotPos = mainDepot.getPosition();

                if (!canReach(environment, currentVehicle, mainDepotPos, currentTime)) {
                    LocalDateTime updatedTime = processFuelSupply(environment, currentVehicle, currentTime, actions);
                    if (updatedTime == null) {
                        return null;
                    }
                    currentTime = updatedTime;

                    if (!canReach(environment, currentVehicle, mainDepotPos, currentTime)) {
                        return null;
                    }
                }

                currentTime = driveToLocation(environment, currentVehicle, mainDepotPos, currentTime, actions);
                Action maintenanceAction = ActionFactory.createMaintenanceAction(
                        mainDepotPos,
                        Duration.ofMinutes(Constants.ROUTINE_MAINTENANCE_MINUTES),
                        currentTime);
                actions.add(maintenanceAction);

                currentTime = currentTime.plus(maintenanceAction.getDuration());
            }

            return new VehiclePlan(currentVehicle, actions, planStartTime);
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to create plan: " + e.getMessage());
            return null;
        }
    }

    public static Depot findNearestGLPDepot(List<Depot> depots, Position currentPosition, int glpNeeded,
            Depot mainDepot) {
        final double effectiveGlpNeeded = Math.max(glpNeeded, Constants.EPSILON);
        return depots.stream()
                .filter(depot -> depot.getCurrentGlpM3() >= effectiveGlpNeeded)
                .min((d1, d2) -> Double.compare(currentPosition.distanceTo(d1.getPosition()),
                        currentPosition.distanceTo(d2.getPosition())))
                .orElse(mainDepot);
    }

    public static Depot findNearestFuelDepot(List<Depot> depots, Position currentPosition, Depot mainDepot) {
        return depots.stream()
                .filter(Depot::canRefuel)
                .min((d1, d2) -> Double.compare(currentPosition.distanceTo(d1.getPosition()),
                        currentPosition.distanceTo(d2.getPosition())))
                .orElse(mainDepot);
    }
}