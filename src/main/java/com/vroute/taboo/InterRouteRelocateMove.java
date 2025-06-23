package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.List;

import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A move that relocates a stop from one route to another
 */
public class InterRouteRelocateMove implements TabuMove {
    private final int fromRouteIndex;
    private final int fromStopIndex;
    private final int toRouteIndex;
    private final int toStopIndex;
    private Solution solution;
    
    /**
     * Create an inter-route relocation move
     * @param fromRouteIndex Index of the source route
     * @param fromStopIndex Index of the stop to relocate
     * @param toRouteIndex Index of the target route
     * @param toStopIndex Index where to insert the stop in the target route
     */
    public InterRouteRelocateMove(int fromRouteIndex, int fromStopIndex, int toRouteIndex, int toStopIndex) {
        this.fromRouteIndex = fromRouteIndex;
        this.fromStopIndex = fromStopIndex;
        this.toRouteIndex = toRouteIndex;
        this.toStopIndex = toStopIndex;
    }
    
    @Override
    public boolean apply(Solution solution) {
        this.solution = solution;
        
        // Get routes to modify
        List<Route> routes = solution.getRoutes();
        if (fromRouteIndex >= routes.size() || toRouteIndex >= routes.size() || fromRouteIndex == toRouteIndex) {
            return false;
        }
        
        Route fromRoute = routes.get(fromRouteIndex);
        Route toRoute = routes.get(toRouteIndex);
        
        List<RouteStop> fromStops = fromRoute.getStops();
        List<RouteStop> toStops = toRoute.getStops();
        
        // Validate indices
        if (fromStopIndex < 0 || fromStopIndex >= fromStops.size() ||
            toStopIndex < 0 || toStopIndex > toStops.size()) {
            return false;
        }
        
        // Only allow moving OrderStops
        if (!(fromStops.get(fromStopIndex) instanceof OrderStop)) {
            return false;
        }
        
        // Extract the stop to move
        OrderStop stopToMove = (OrderStop) fromStops.remove(fromStopIndex);
        
        // Check if the target vehicle can handle the order
        Vehicle targetVehicle = toRoute.getVehicle();
        if (!canVehicleHandleStop(targetVehicle, stopToMove)) {
            // Put the stop back and return failure
            fromStops.add(fromStopIndex, stopToMove);
            return false;
        }
        
        // Insert at the target position
        toStops.add(toStopIndex, stopToMove);
        
        // Validate and recalculate both routes
        boolean fromValid = recalculateArrivalTimes(fromRoute);
        boolean toValid = recalculateArrivalTimes(toRoute);
        
        if (!fromValid || !toValid) {
            // If either route is invalid, revert the move
            toStops.remove(toStopIndex);
            fromStops.add(fromStopIndex, stopToMove);
            // Restore original arrival times
            recalculateArrivalTimes(fromRoute);
            recalculateArrivalTimes(toRoute);
            return false;
        }
        
        return true;
    }
    
    private boolean canVehicleHandleStop(Vehicle vehicle, OrderStop stop) {
        // Check if the vehicle has enough capacity for this stop
        // This is a simplified check - in a real system you would consider
        // the actual GLP remaining in the vehicle after previous stops
        
        int glpRequest = stop.getGlpDelivery();
        return glpRequest <= vehicle.getGlpCapacityM3();
    }
    
    private boolean recalculateArrivalTimes(Route route) {
        List<RouteStop> stops = route.getStops();
        if (stops.isEmpty()) {
            return true;
        }
        
        // Starting from the first stop, recalculate arrival times for all stops
        LocalDateTime currentTime = stops.get(0).getArrivalTime();
        
        for (int i = 1; i < stops.size(); i++) {
            RouteStop current = stops.get(i);
            RouteStop previous = stops.get(i - 1);
            
            // Calculate travel time from previous stop to current stop
            Position prevPos = previous.getPosition();
            Position currPos = current.getPosition();
            double distance = prevPos.distanceTo(currPos);
            
            // Assuming a constant speed of 50 km/h for simplicity
            double travelTimeHours = distance / 50.0;
            int travelTimeMinutes = (int) Math.ceil(travelTimeHours * 60);
            
            // Update arrival time
            currentTime = currentTime.plusMinutes(travelTimeMinutes);
            
            // Check if the stop is an OrderStop, and if so, validate delivery time
            if (current instanceof OrderStop) {
                OrderStop orderStop = (OrderStop) current;
                String orderId = orderStop.getEntityID();
                if (solution.getOrders().containsKey(orderId)) {
                    LocalDateTime dueTime = solution.getOrders().get(orderId).getDueTime();
                    if (currentTime.isAfter(dueTime)) {
                        // Delivery would be late, move is invalid
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    @Override
    public String getTabuKey() {
        return "inter_relocate_" + fromRouteIndex + "_" + fromStopIndex + "_" + toRouteIndex + "_" + toStopIndex;
    }
} 