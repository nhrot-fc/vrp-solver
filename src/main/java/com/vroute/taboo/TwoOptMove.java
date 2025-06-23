package com.vroute.taboo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vroute.models.Position;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * A 2-opt move that inverts a segment of a route to improve it
 */
public class TwoOptMove implements TabuMove {
    private final int routeIndex;
    private final int fromIndex;
    private final int toIndex;
    private Solution solution;
    
    /**
     * Create a 2-opt move
     * @param routeIndex Index of the route in the solution
     * @param fromIndex Start index of the segment to invert (inclusive)
     * @param toIndex End index of the segment to invert (inclusive)
     */
    public TwoOptMove(int routeIndex, int fromIndex, int toIndex) {
        this.routeIndex = routeIndex;
        // Ensure fromIndex < toIndex
        this.fromIndex = Math.min(fromIndex, toIndex);
        this.toIndex = Math.max(fromIndex, toIndex);
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
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0 || 
            fromIndex >= stops.size() || toIndex >= stops.size() ||
            toIndex - fromIndex < 1) { // Need at least 2 elements to invert
            return false;
        }
        
        // Make a backup of the original stops
        List<RouteStop> originalStops = new ArrayList<>(stops);
        
        // Invert the segment
        Collections.reverse(stops.subList(fromIndex, toIndex + 1));
        
        // Validate and recalculate arrival times
        if (!recalculateArrivalTimes(route)) {
            // If invalid, revert to original
            for (int i = 0; i < stops.size(); i++) {
                stops.set(i, originalStops.get(i));
            }
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
        return "2opt_" + routeIndex + "_" + fromIndex + "_" + toIndex;
    }
} 