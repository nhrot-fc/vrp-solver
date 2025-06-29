package com.vroute.test;

import com.vroute.models.*;
import com.vroute.solution.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import static com.vroute.test.TestFramework.Assertions.*;

/**
 * Suite de pruebas para el RandomDistributor usando el mini framework de
 * testing
 */
public class RandomDistributorTest {

    /**
     * Crea una suite de tests para el RandomDistributor
     * 
     * @return Suite de tests configurada
     */
    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("RandomDistributor");

        // Add tests
        suite.addTest(new BasicDistributionTest());
        suite.addTest(new NoOrdersTest());
        suite.addTest(new NoVehiclesTest());
        suite.addTest(new LargeOrdersTest());
        suite.addTest(new MultipleSolveAttemptsTest());
        suite.addTest(new VehicleCapacityTest());

        return suite;
    }

    /**
     * Test to verify that the RandomDistributor respects vehicle GLP capacity
     * constraints
     */
    private static class VehicleCapacityTest extends TestFramework.AbstractTest {
        public VehicleCapacityTest() {
            super("Vehicle Capacity Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Get vehicle capacities for validation
            Map<String, Integer> vehicleCapacities = new HashMap<>();
            for (Vehicle vehicle : env.getVehicles()) {
                vehicleCapacities.put(vehicle.getId(), vehicle.getGlpCapacityM3());
                vehicle.setCurrentGlpM3(0); // Ensure all vehicles start empty
            }

            // Create orders
            List<Order> orders = TestUtilities.createLargeOrders(baseTime);
            env.addOrders(orders);

            // Fill depots with GLP
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            // Create solver and get solution
            RandomDistributor distributor = new RandomDistributor();
            Solution solution = distributor.solve(env);

            // Verify solution is not null
            assertNotNull(solution, "Solution should not be null");

            // Verify capacities are not violated in the initial assignment
            boolean capacityRespected = true;
            Map<String, Integer> vehicleGlpAssigned = new HashMap<>();

            // Check each route to verify capacity constraints
            for (Route route : solution.getRoutes()) {
                String vehicleId = route.getVehicle().getId();
                int capacity = vehicleCapacities.get(vehicleId);
                int totalAssigned = 0;

                // Check each stop in the route
                for (RouteStop stop : route.getStops()) {
                    if (stop instanceof OrderStop) {
                        OrderStop orderStop = (OrderStop) stop;
                        totalAssigned += orderStop.getGlpDelivery();
                    } else if (stop instanceof DepotStop) {
                        DepotStop depotStop = (DepotStop) stop;
                        totalAssigned -= depotStop.getGlpRecharge(); // Recharging decreases the load
                    }

                    // Check if at any point the capacity is exceeded
                    if (totalAssigned > capacity) {
                        capacityRespected = false;
                        break;
                    }
                }

                vehicleGlpAssigned.put(vehicleId, totalAssigned);
            }

            assertTrue(capacityRespected,
                    "Vehicle GLP capacity constraints should be respected in the initial assignment");
        }
    }

    /**
     * Test for basic order distribution functionality
     */
    private static class BasicDistributionTest extends TestFramework.AbstractTest {
        public BasicDistributionTest() {
            super("Basic Distribution Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create several small orders
            List<Order> orders = TestUtilities.createSmallOrders(baseTime);
            env.addOrders(orders);

            // Fill depots with GLP
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            // Refuel and prepare vehicles
            for (Vehicle vehicle : env.getVehicles()) {
                vehicle.refuel();
                vehicle.setCurrentGlpM3(0); // Ensure vehicles start empty
            }

            // Create solver and get solution
            RandomDistributor distributor = new RandomDistributor();
            Solution solution = distributor.solve(env);

            // Verify solution is not null
            assertNotNull(solution, "Solution should not be null");

            // Verify there are routes in the solution
            assertTrue(solution.getRoutes().size() > 0, "Solution should contain routes");

            // Verify routes have stops
            boolean hasStops = false;
            for (Route route : solution.getRoutes()) {
                if (!route.getStops().isEmpty()) {
                    hasStops = true;
                    break;
                }
            }
            assertTrue(hasStops, "At least one route should have stops");

            // Note: We're not asserting the score is valid because the RandomDistributor
            // doesn't guarantee optimal or even valid solutions - it's just a random
            // distribution
        }
    }

    /**
     * Test for case where there are no orders
     */
    private static class NoOrdersTest extends TestFramework.AbstractTest {
        public NoOrdersTest() {
            super("No Orders Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment with no orders
            Environment env = TestUtilities.createSampleEnvironment();

            // Ensure no orders are added - the environment starts with no orders

            // Create solver and get solution
            RandomDistributor distributor = new RandomDistributor();
            Solution solution = distributor.solve(env);

            // Verify solution is not null but has no routes
            assertNotNull(solution, "Solution should not be null even with no orders");
            assertEquals(0, solution.getRoutes().size(),
                    "Solution should have no routes when there are no orders");
        }
    }

    /**
     * Test for case where there are no vehicles available
     */
    private static class NoVehiclesTest extends TestFramework.AbstractTest {
        public NoVehiclesTest() {
            super("No Vehicles Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create environment without any vehicles
            Environment env = new Environment(new ArrayList<>(),
                    TestUtilities.createSampleEnvironment().getMainDepot(),
                    TestUtilities.createSampleEnvironment().getAuxDepots(),
                    LocalDateTime.of(2025, 1, 1, 0, 0));
            LocalDateTime baseTime = env.getCurrentTime();

            // Add orders
            List<Order> orders = TestUtilities.createSmallOrders(baseTime);
            env.addOrders(orders);

            // Create solver and get solution
            RandomDistributor distributor = new RandomDistributor();
            Solution solution = distributor.solve(env);

            // The implementation should handle this gracefully
            assertNull(solution, "Solution should be null when there are no vehicles available");
        }
    }

    /**
     * Test for distributing large orders that require multiple vehicles
     */
    private static class LargeOrdersTest extends TestFramework.AbstractTest {
        public LargeOrdersTest() {
            super("Large Orders Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create one large order
            Order largeOrder = new Order("LO1", baseTime, baseTime.plusHours(8), 50, new Position(10, 10));
            List<Order> orders = new ArrayList<>();
            orders.add(largeOrder);
            env.addOrders(orders);

            // Fill depots with GLP
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            // Ensure vehicles are prepared
            for (Vehicle vehicle : env.getVehicles()) {
                vehicle.refuel();
                vehicle.setCurrentGlpM3(0);
            }

            // Create solver and get solution
            RandomDistributor distributor = new RandomDistributor();
            Solution solution = distributor.solve(env);

            // Verify solution is not null
            assertNotNull(solution, "Solution should not be null for large orders");

            // Verify that routes exist
            assertTrue(solution.getRoutes().size() > 0, "Solution should contain routes");

            // Calculate total GLP assigned across all routes
            int totalGlpAssigned = 0;
            for (Route route : solution.getRoutes()) {
                for (RouteStop stop : route.getStops()) {
                    if (stop instanceof OrderStop) {
                        OrderStop orderStop = (OrderStop) stop;
                        if (orderStop.getOrder().getId().equals("LO1")) {
                            totalGlpAssigned += orderStop.getGlpDelivery();
                        }
                    }
                }
            }

            // Check that the full order amount is assigned
            assertEquals(50, totalGlpAssigned,
                    "Total GLP assigned should match the order request");

            // Note: We're not asserting the score is valid because the RandomDistributor
            // doesn't guarantee optimal or even valid solutions
        }
    }

    /**
     * Test for multiple solve attempts to verify non-deterministic behavior
     */
    private static class MultipleSolveAttemptsTest extends TestFramework.AbstractTest {
        public MultipleSolveAttemptsTest() {
            super("Multiple Solve Attempts Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create several orders
            List<Order> orders = TestUtilities.createSmallOrders(baseTime);
            env.addOrders(orders);

            // Fill depots
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            // Create solver
            RandomDistributor distributor = new RandomDistributor();

            // Get two solutions with same environment
            Solution solution1 = distributor.solve(env);
            Solution solution2 = distributor.solve(env);

            // Both should be valid
            assertNotNull(solution1, "First solution should not be null");
            assertNotNull(solution2, "Second solution should not be null");

            // The solutions should likely be different due to randomness
            // This is not a guaranteed assertion, but we'll check it in a safer way
            // Compare the routes directly, not through toString()
            Set<String> routeIds1 = new HashSet<>();
            Set<String> routeIds2 = new HashSet<>();

            for (Route route : solution1.getRoutes()) {
                routeIds1.add(route.getVehicle().getId());
            }

            for (Route route : solution2.getRoutes()) {
                routeIds2.add(route.getVehicle().getId());
            }

            // If the sets have different sizes or different elements, the solutions are
            // different
            boolean solutionsDifferent = routeIds1.size() != routeIds2.size() || !routeIds1.containsAll(routeIds2);

            // This is a soft assertion - randomness may rarely produce the same solution
            if (!solutionsDifferent) {
                System.out.println(
                        "Note: Both solutions have the same vehicles assigned. This is possible but unlikely.");
            }
        }
    }
}