package com.vroute.taboo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.vroute.models.Environment;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.solution.Solution;

/**
 * Generates different types of moves for a given solution.
 */
public class MoveGenerator {
    private final Random random;
    private Environment environment;
    
    /**
     * Creates a new move generator with a random seed.
     */
    public MoveGenerator() {
        this.random = new Random();
    }
    
    /**
     * Creates a new move generator with a specified seed.
     * 
     * @param seed The random seed
     */
    public MoveGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * Sets the environment reference for path calculations.
     * 
     * @param env The environment containing road and blockage information
     */
    public void setEnvironment(Environment env) {
        this.environment = env;
        
        // Set environment in all move types
        RelocateMove.setEnvironment(env);
        SwapMove.setEnvironment(env);
        TwoOptMove.setEnvironment(env);
    }
    
    /**
     * Generate a list of relocate moves for the given solution.
     * 
     * @param solution The current solution
     * @param maxMoves Maximum number of moves to generate
     * @return List of relocate moves
     */
    public List<RelocateMove> generateRelocateMoves(Solution solution, int maxMoves) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set for path calculations");
        }
        
        List<RelocateMove> moves = new ArrayList<>();
        List<Route> routes = solution.getRoutes();
        
        if (routes.size() <= 1) {
            return moves; // Not enough routes for relocate moves
        }
        
        // For each route pair, find order stops that can be relocated
        for (int i = 0; i < routes.size(); i++) {
            Route sourceRoute = routes.get(i);
            List<RouteStop> sourceStops = sourceRoute.getStops();
            
            // Find order stops in the source route
            List<Integer> orderPositions = new ArrayList<>();
            for (int pos = 0; pos < sourceStops.size(); pos++) {
                if (sourceStops.get(pos) instanceof OrderStop) {
                    orderPositions.add(pos);
                }
            }
            
            if (orderPositions.isEmpty()) {
                continue; // No orders to relocate
            }
            
            // For each target route
            for (int j = 0; j < routes.size(); j++) {
                if (i == j) continue; // Skip same route
                
                Route targetRoute = routes.get(j);
                List<RouteStop> targetStops = targetRoute.getStops();
                
                // For some random order positions in source
                int numToGenerate = Math.min(maxMoves / routes.size(), orderPositions.size());
                for (int k = 0; k < numToGenerate; k++) {
                    // Pick a random order position
                    int pickIndex = random.nextInt(orderPositions.size());
                    int sourcePosition = orderPositions.get(pickIndex);
                    OrderStop orderStop = (OrderStop) sourceStops.get(sourcePosition);
                    
                    // Pick a random insert position in target
                    int targetPosition = random.nextInt(targetStops.size() + 1);
                    
                    // Create the move
                    RelocateMove move = new RelocateMove(
                            sourceRoute.getId(),
                            targetRoute.getId(),
                            sourcePosition,
                            targetPosition,
                            orderStop.getEntityID());
                    
                    moves.add(move);
                    
                    // Remove this position to avoid duplicates
                    orderPositions.remove(pickIndex);
                    if (orderPositions.isEmpty()) {
                        break;
                    }
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Generate a list of swap moves for the given solution.
     * 
     * @param solution The current solution
     * @param maxMoves Maximum number of moves to generate
     * @return List of swap moves
     */
    public List<SwapMove> generateSwapMoves(Solution solution, int maxMoves) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set for path calculations");
        }
        
        List<SwapMove> moves = new ArrayList<>();
        List<Route> routes = solution.getRoutes();
        
        if (routes.isEmpty()) {
            return moves;
        }
        
        int movesGenerated = 0;
        
        // For each route (inter-route swaps)
        for (int i = 0; i < routes.size(); i++) {
            Route firstRoute = routes.get(i);
            List<RouteStop> firstStops = firstRoute.getStops();
            
            // Find order stops in the first route
            List<Integer> firstOrderPositions = new ArrayList<>();
            for (int pos = 0; pos < firstStops.size(); pos++) {
                if (firstStops.get(pos) instanceof OrderStop) {
                    firstOrderPositions.add(pos);
                }
            }
            
            if (firstOrderPositions.isEmpty()) {
                continue;
            }
            
            // For each other route
            for (int j = i; j < routes.size(); j++) {
                Route secondRoute = routes.get(j);
                List<RouteStop> secondStops = secondRoute.getStops();
                
                // Find order stops in the second route
                List<Integer> secondOrderPositions = new ArrayList<>();
                for (int pos = 0; pos < secondStops.size(); pos++) {
                    if (secondStops.get(pos) instanceof OrderStop) {
                        secondOrderPositions.add(pos);
                    }
                }
                
                if (secondOrderPositions.isEmpty() || 
                    (i == j && secondOrderPositions.size() < 2)) {
                    continue;
                }
                
                // Generate swaps
                int numMoves = Math.min(maxMoves / (routes.size() * routes.size()), 
                                      Math.min(firstOrderPositions.size(), secondOrderPositions.size()));
                
                for (int k = 0; k < numMoves && movesGenerated < maxMoves; k++) {
                    // Select random positions
                    int firstIdx = random.nextInt(firstOrderPositions.size());
                    int firstPos = firstOrderPositions.get(firstIdx);
                    
                    int secondIdx;
                    if (i == j) {
                        // For same route, make sure we pick different positions
                        do {
                            secondIdx = random.nextInt(secondOrderPositions.size());
                        } while (secondOrderPositions.get(secondIdx).equals(firstPos));
                    } else {
                        secondIdx = random.nextInt(secondOrderPositions.size());
                    }
                    int secondPos = secondOrderPositions.get(secondIdx);
                    
                    // Create the swap move
                    OrderStop firstOrderStop = (OrderStop) firstStops.get(firstPos);
                    OrderStop secondOrderStop = (OrderStop) secondStops.get(secondPos);
                    
                    SwapMove move = new SwapMove(
                            firstRoute.getId(),
                            secondRoute.getId(),
                            firstPos,
                            secondPos,
                            firstOrderStop.getEntityID(),
                            secondOrderStop.getEntityID());
                    
                    moves.add(move);
                    movesGenerated++;
                    
                    // Remove used positions
                    firstOrderPositions.remove(firstIdx);
                    if (i == j) {
                        // For same route, adjust second positions too
                        secondOrderPositions = new ArrayList<>(firstOrderPositions);
                    } else {
                        secondOrderPositions.remove(secondIdx);
                    }
                    
                    if (firstOrderPositions.isEmpty() || 
                        secondOrderPositions.isEmpty()) {
                        break;
                    }
                }
            }
        }
        
        return moves;
    }
    
    /**
     * Generate a list of 2-opt moves for the given solution.
     * 
     * @param solution The current solution
     * @param maxMoves Maximum number of moves to generate
     * @return List of 2-opt moves
     */
    public List<TwoOptMove> generateTwoOptMoves(Solution solution, int maxMoves) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set for path calculations");
        }
        
        List<TwoOptMove> moves = new ArrayList<>();
        List<Route> routes = solution.getRoutes();
        
        if (routes.isEmpty()) {
            return moves;
        }
        
        int movesGenerated = 0;
        int movesPerRoute = maxMoves / routes.size();
        
        for (Route route : routes) {
            List<RouteStop> stops = route.getStops();
            
            // Need at least 4 stops for 2-opt to make sense
            if (stops.size() < 4) {
                continue;
            }
            
            // Generate moves for this route
            for (int k = 0; k < movesPerRoute && movesGenerated < maxMoves; k++) {
                // Pick random start and end positions
                // Ensure they're at least 2 positions apart
                int startPos = random.nextInt(stops.size() - 3);
                int endPos = startPos + 2 + random.nextInt(stops.size() - startPos - 2);
                
                TwoOptMove move = new TwoOptMove(route.getId(), startPos, endPos);
                moves.add(move);
                movesGenerated++;
            }
        }
        
        return moves;
    }
    
    /**
     * Generate all types of moves for the given solution.
     * 
     * @param solution The current solution
     * @param maxMovesPerType Maximum number of moves to generate per type
     * @return List of all moves
     */
    public List<TabuMove> generateAllMoves(Solution solution, int maxMovesPerType) {
        if (environment == null) {
            throw new IllegalStateException("Environment not set for path calculations");
        }
        
        List<TabuMove> allMoves = new ArrayList<>();
        
        // Generate relocate moves
        List<RelocateMove> relocateMoves = generateRelocateMoves(solution, maxMovesPerType);
        allMoves.addAll(relocateMoves);
        
        // Generate swap moves
        List<SwapMove> swapMoves = generateSwapMoves(solution, maxMovesPerType);
        allMoves.addAll(swapMoves);
        
        // Generate 2-opt moves
        List<TwoOptMove> twoOptMoves = generateTwoOptMoves(solution, maxMovesPerType);
        allMoves.addAll(twoOptMoves);
        
        return allMoves;
    }
} 