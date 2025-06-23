package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.vroute.models.Position;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A move that swaps two stops within the same route
 */
public class SwapMove implements TabuMove {
    private final int routeIndex;
    private final int firstIndex;
    private final int secondIndex;
    private Solution solution;
    
    /**
     * Create a swap move
     * @param routeIndex Index of the route in the solution
     * @param firstIndex Position of the first stop to swap
     * @param secondIndex Position of the second stop to swap
     */
    public SwapMove(int routeIndex, int firstIndex, int secondIndex) {
        this.routeIndex = routeIndex;
        this.firstIndex = firstIndex;
        this.secondIndex = secondIndex;
    }
    
    @Override
    public boolean apply(Solution solution) {
        this.solution = solution;
        
        // Get the route to modify
        List<Route> routes = solution.getRoutes();
        if (routeIndex >= routes.size()) {
            return false;
        }
        
        Route route = routes.get(routeIndex);
        List<RouteStop> stops = route.getStops();
        
        // Validate indices
        if (firstIndex == secondIndex || firstIndex < 0 || secondIndex < 0 || 
            firstIndex >= stops.size() || secondIndex >= stops.size()) {
            return false;
        }
        
        // Cannot swap depot stops
        if (!(stops.get(firstIndex) instanceof OrderStop) || 
            !(stops.get(secondIndex) instanceof OrderStop)) {
            return false;
        }
        
        // Perform the swap
        Collections.swap(stops, firstIndex, secondIndex);
        
        // Validate and recalculate arrival times
        if (!recalculateArrivalTimes(route)) {
            // If invalid, revert the swap
            Collections.swap(stops, firstIndex, secondIndex);
            return false;
        }
        
        return true;
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
        // Order indices consistently to make the key invariant to the order of arguments
        int min = Math.min(firstIndex, secondIndex);
        int max = Math.max(firstIndex, secondIndex);
        return "swap_" + routeIndex + "_" + min + "_" + max;
    }
} 