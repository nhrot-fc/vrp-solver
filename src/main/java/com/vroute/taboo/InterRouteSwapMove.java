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
 * A move that swaps two stops between different routes
 */
public class InterRouteSwapMove implements TabuMove {
    private final int firstRouteIndex;
    private final int firstStopIndex;
    private final int secondRouteIndex;
    private final int secondStopIndex;
    private Solution solution;
    
    /**
     * Create an inter-route swap move
     * @param firstRouteIndex Index of the first route
     * @param firstStopIndex Index of the stop in the first route to swap
     * @param secondRouteIndex Index of the second route
     * @param secondStopIndex Index of the stop in the second route to swap
     */
    public InterRouteSwapMove(int firstRouteIndex, int firstStopIndex, int secondRouteIndex, int secondStopIndex) {
        this.firstRouteIndex = firstRouteIndex;
        this.firstStopIndex = firstStopIndex;
        this.secondRouteIndex = secondRouteIndex;
        this.secondStopIndex = secondStopIndex;
    }
    
    @Override
    public boolean apply(Solution solution) {
        this.solution = solution;
        
        // Get routes to modify
        List<Route> routes = solution.getRoutes();
        if (firstRouteIndex >= routes.size() || secondRouteIndex >= routes.size() || 
            firstRouteIndex == secondRouteIndex) {
            return false;
        }
        
        Route firstRoute = routes.get(firstRouteIndex);
        Route secondRoute = routes.get(secondRouteIndex);
        
        List<RouteStop> firstStops = firstRoute.getStops();
        List<RouteStop> secondStops = secondRoute.getStops();
        
        // Validate indices
        if (firstStopIndex < 0 || firstStopIndex >= firstStops.size() ||
            secondStopIndex < 0 || secondStopIndex >= secondStops.size()) {
            return false;
        }
        
        // Only allow swapping OrderStops
        if (!(firstStops.get(firstStopIndex) instanceof OrderStop) || 
            !(secondStops.get(secondStopIndex) instanceof OrderStop)) {
            return false;
        }
        
        // Extract the stops to swap
        OrderStop firstStop = (OrderStop) firstStops.get(firstStopIndex);
        OrderStop secondStop = (OrderStop) secondStops.get(secondStopIndex);
        
        // Check if the vehicles can handle the swapped stops
        Vehicle firstVehicle = firstRoute.getVehicle();
        Vehicle secondVehicle = secondRoute.getVehicle();
        
        if (!canVehicleHandleStop(firstVehicle, secondStop) || 
            !canVehicleHandleStop(secondVehicle, firstStop)) {
            return false;
        }
        
        // Perform the swap
        firstStops.set(firstStopIndex, secondStop);
        secondStops.set(secondStopIndex, firstStop);
        
        // Validate and recalculate both routes
        boolean firstValid = recalculateArrivalTimes(firstRoute);
        boolean secondValid = recalculateArrivalTimes(secondRoute);
        
        if (!firstValid || !secondValid) {
            // If either route is invalid, revert the swap
            firstStops.set(firstStopIndex, firstStop);
            secondStops.set(secondStopIndex, secondStop);
            // Restore original arrival times
            recalculateArrivalTimes(firstRoute);
            recalculateArrivalTimes(secondRoute);
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
        // Order route indices consistently to make the key invariant to the order of arguments
        if (firstRouteIndex < secondRouteIndex) {
            return "inter_swap_" + firstRouteIndex + "_" + firstStopIndex + "_" + 
                   secondRouteIndex + "_" + secondStopIndex;
        } else {
            return "inter_swap_" + secondRouteIndex + "_" + secondStopIndex + "_" + 
                   firstRouteIndex + "_" + firstStopIndex;
        }
    }
} 