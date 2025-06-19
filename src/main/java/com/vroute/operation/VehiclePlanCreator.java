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
 * Simplified Rules for Vehicle Plan Generation:
 * 
 * Start:
 * - If vehicle is at main depot → Execute routine maintenance
 * - If delivery instruction location is blocked → Skip instruction
 * - If not enough GLP for delivery → Refill at nearest GLP depot
 * 
 * Core:
 * - Drive to order location
 * - Serve order
 * 
 * End:
 * - Return to main depot
 * 
 * Driving Logic:
 * - Calculate path
 * - If no path → Exception
 * - If not enough fuel → Go to main depot for refueling
 * - If not enough fuel to return to main depot or no path → Exception
 * 
 * Constraints:
 * - GLP can be refilled at any depot (main or auxiliary)
 * - Fuel can ONLY be refilled at the main depot
 * - Vehicle speed: Constant 50 Km/h
 * - Fuel consumption: (Distance * (TareWeight + GLPWeight)) / Constants.CONSUMPTION_FACTOR
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
        // Make sure we don't exceed the vehicle's GLP capacity
        int actualGlpAmount = Math.min(glpAmountM3, vehicle.getGlpCapacityM3() - vehicle.getCurrentGlpM3());
        
        // Don't add an action if there's no GLP to add
        if (actualGlpAmount <= 0) {
            return currentTime;
        }
        
        Action refillAction = ActionFactory.createRefillingAction(glpDepot, actualGlpAmount, currentTime);
        actions.add(refillAction);
        vehicle.refill(actualGlpAmount);
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

        Action drivingAction = ActionFactory.createDrivingAction(path, fuelConsumedGal, currentTime,
                currentTime.plus(duration));
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
            // Fuel can ONLY be obtained at the main depot
            Depot mainDepot = environment.getMainDepot();
            if (!canReach(environment, vehicle, mainDepot.getPosition(), currentTime)) {
                return null;
            }

            LocalDateTime updatedTime = driveToLocation(environment, vehicle, mainDepot.getPosition(), currentTime, actions);
            updatedTime = vehicleRefuel(vehicle, mainDepot, updatedTime, actions);

            return updatedTime;
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to refuel: " + e.getMessage());
            return null;
        }
    }

    public static LocalDateTime processGlpSupply(Environment environment, Vehicle vehicle, int glpRequired,
            LocalDateTime currentTime, List<Action> actions) {
        try {
            // Find closest depot with sufficient GLP
            Depot glpDepot = findNearestGLPDepot(environment.getAuxDepots(), vehicle.getCurrentPosition(), glpRequired,
                    environment.getMainDepot());

            if (canReach(environment, vehicle, glpDepot.getPosition(), currentTime)) {
                currentTime = driveToLocation(environment, vehicle, glpDepot.getPosition(), currentTime, actions);
                currentTime = vehicleRefill(vehicle, glpDepot, glpRequired, currentTime, actions);
            } else {
                // Need to refuel first at the main depot
                Depot fuelDepot = environment.getMainDepot();
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

            // If at main depot refuel
            if (glpDepot == environment.getMainDepot()) {
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

            // START: If at main depot, perform routine maintenance
            if (currentVehicle.getCurrentPosition().equals(mainDepot.getPosition())) {
                Action maintenanceAction = ActionFactory.createMaintenanceAction(
                        mainDepot.getPosition(),
                        Duration.ofMinutes(Constants.ROUTINE_MAINTENANCE_MINUTES),
                        currentTime);
                actions.add(maintenanceAction);
                currentTime = currentTime.plus(maintenanceAction.getDuration());
            }

            // Process each delivery instruction
            for (DeliveryInstruction instruction : instructions) {
                Order order = instruction.getOriginalOrder().clone();
                Position orderPosition = order.getPosition();
                
                // Check if location is blocked (can't reach customer)
                if (!canReach(environment, currentVehicle, orderPosition, currentTime)) {
                    // Try to refuel first if it might be a fuel issue
                    if (currentVehicle.getCurrentFuelGal() < currentVehicle.getFuelCapacityGal() * 0.3) {
                        LocalDateTime updatedTime = processFuelSupply(environment, currentVehicle, currentTime, actions);
                        
                        // If refueling failed or still can't reach, skip this order
                        if (updatedTime == null || !canReach(environment, currentVehicle, orderPosition, updatedTime)) {
                            continue; // Skip this instruction
                        }
                        currentTime = updatedTime;
                    } else {
                        // Location is blocked for other reasons
                        continue; // Skip this instruction
                    }
                }
                
                // Check if need GLP refill
                if (currentVehicle.getCurrentGlpM3() < instruction.getGlpAmountToDeliver()) {
                    int glpNeeded = currentVehicle.getGlpCapacityM3() - currentVehicle.getCurrentGlpM3();
                    // Go to nearest GLP depot to refill
                    LocalDateTime updatedTime = processGlpSupply(environment, currentVehicle, glpNeeded, currentTime, actions);
                    if (updatedTime == null) {
                        continue; // Skip if refill failed
                    }
                    currentTime = updatedTime;
                }
                
                try {
                    // Drive to customer location
                    currentTime = driveToLocation(environment, currentVehicle, orderPosition, currentTime, actions);
                    
                    // Serve the order
                    Action servingAction = ActionFactory.createServingAction(
                            orderPosition,
                            order,
                            instruction.getGlpAmountToDeliver(),
                            currentTime);
                    actions.add(servingAction);
                    
                    currentVehicle.dispenseGlp(instruction.getGlpAmountToDeliver());
                    currentTime = currentTime.plus(servingAction.getDuration());
                } catch (NoPathFoundException e) {
                    // Path blocked, skip this instruction
                    continue;
                } catch (InsufficientFuelException e) {
                    // Not enough fuel (shouldn't happen due to previous checks)
                    continue;
                }
            }

            // END: Return to main depot if not already there
            if (!currentVehicle.getCurrentPosition().equals(mainDepot.getPosition())) {
                // Check if can reach main depot
                if (!canReach(environment, currentVehicle, mainDepot.getPosition(), currentTime)) {
                    // Try to refuel first
                    LocalDateTime updatedTime = processFuelSupply(environment, currentVehicle, currentTime, actions);
                    if (updatedTime == null) {
                        // Can't reach main depot even after refuel attempt
                        return null;
                    }
                    currentTime = updatedTime;
                }
                
                try {
                    // Drive to main depot
                    currentTime = driveToLocation(environment, currentVehicle, mainDepot.getPosition(), currentTime, actions);
                    
                    // Perform maintenance at main depot
                    Action maintenanceAction = ActionFactory.createMaintenanceAction(
                            mainDepot.getPosition(),
                            Duration.ofMinutes(Constants.ROUTINE_MAINTENANCE_MINUTES),
                            currentTime);
                    actions.add(maintenanceAction);
                    currentTime = currentTime.plus(maintenanceAction.getDuration());
                } catch (NoPathFoundException | InsufficientFuelException e) {
                    return null;
                }
            }

            return new VehiclePlan(currentVehicle, actions, planStartTime);
        } catch (Exception e) {
            System.err.println("Failed to create plan: " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a simple plan to send a vehicle to the main depot.
     * Used when a vehicle has no assigned deliveries.
     */
    public static VehiclePlan createPlanToMainDepot(Environment environment, Vehicle vehicle) {
        try {
            Vehicle currentVehicle = vehicle.clone();
            LocalDateTime planStartTime = environment.getCurrentTime();
            List<Action> actions = new ArrayList<>();
            Depot mainDepot = environment.getMainDepot();

            // Only create plan if not already at main depot
            if (!currentVehicle.getCurrentPosition().equals(mainDepot.getPosition())) {
                LocalDateTime currentTime = planStartTime;

                // Check if need to refuel first
                if (!canReach(environment, currentVehicle, mainDepot.getPosition(), currentTime)) {
                    // Can't reach main depot with current fuel
                    return null;
                }

                // Drive to main depot
                currentTime = driveToLocation(environment, currentVehicle, mainDepot.getPosition(), currentTime, actions);
                
                // Perform maintenance at main depot
                Action maintenanceAction = ActionFactory.createMaintenanceAction(
                        mainDepot.getPosition(),
                        Duration.ofMinutes(Constants.ROUTINE_MAINTENANCE_MINUTES),
                        currentTime);
                actions.add(maintenanceAction);
                
                return new VehiclePlan(currentVehicle, actions, planStartTime);
            }
            
            return null; // No plan needed if already at main depot
        } catch (NoPathFoundException | InsufficientFuelException e) {
            System.err.println("Failed to create plan to main depot: " + e.getMessage());
            return null;
        }
    }

    public static Depot findNearestGLPDepot(List<Depot> depots, Position currentPosition, int glpNeeded,
            Depot mainDepot) {
        List<Depot> availableDepots = new ArrayList<>(depots);
        availableDepots.add(mainDepot);
        return availableDepots.stream()
                .filter(depot -> depot.getCurrentGlpM3() >= glpNeeded)
                .min((d1, d2) -> Double.compare(currentPosition.distanceTo(d1.getPosition()),
                        currentPosition.distanceTo(d2.getPosition())))
                .orElse(mainDepot);
    }
}