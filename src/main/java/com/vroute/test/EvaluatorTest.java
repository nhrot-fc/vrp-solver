package com.vroute.test;

import com.vroute.models.*;
import com.vroute.solution.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.vroute.test.TestFramework.Assertions.*;

/**
 * Suite de pruebas para el evaluador de soluciones usando el mini framework de
 * testing
 */
public class EvaluatorTest {

    /**
     * Crea una suite de tests para el evaluador
     * 
     * @return Suite de tests configurada
     */
    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("Evaluator");

        // Add tests
        suite.addTest(new SuccessfulEvaluationTest());
        suite.addTest(new NegativeGLPTest());
        suite.addTest(new ExcessiveGLPTest());
        suite.addTest(new NoPathTest());
        suite.addTest(new ExcessiveDeliveryTest());
        suite.addTest(new OrderDeliveryVerificationTest());

        return suite;
    }

    /**
     * Test for successful evaluation where all orders are correctly delivered
     */
    private static class SuccessfulEvaluationTest extends TestFramework.AbstractTest {
        public SuccessfulEvaluationTest() {
            super("Successful Evaluation Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create orders
            List<Order> orders = new ArrayList<>();
            orders.add(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)));
            env.addOrders(orders);

            // Create vehicle with initial GLP
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(15);

            // Create route that delivers exact amount
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)),
                    new Position(10, 10), 10));

            Route route = new Route(vehicle, stops, baseTime);
            List<Route> routes = new ArrayList<>();
            routes.add(route);

            Solution solution = new Solution(routes, env);

            // Verify result is not negative infinity
            assertFalse(Double.isInfinite(solution.getScore()), "Score should be finite for a valid solution");
        }
    }

    /**
     * Test for case where vehicle has negative GLP after delivery
     */
    private static class NegativeGLPTest extends TestFramework.AbstractTest {
        public NegativeGLPTest() {
            super("Negative GLP Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create orders
            List<Order> orders = new ArrayList<>();
            orders.add(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)));
            env.addOrders(orders);

            // Create vehicle with insufficient GLP
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(5); // Only 5 GLP but will try to deliver 10

            // Create route that tries to deliver more than available
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)),
                    new Position(10, 10), 10));

            Route route = new Route(vehicle, stops, baseTime);
            List<Route> routes = new ArrayList<>();
            routes.add(route);

            Solution solution = new Solution(routes, env);

            // Verify result is negative infinity
            assertTrue(Double.isInfinite(solution.getScore()) && solution.getScore() < 0,
                    "Score should be negative infinity when vehicle has negative GLP");
        }
    }

    /**
     * Test for case where vehicle has excessive GLP after recharge
     */
    private static class ExcessiveGLPTest extends TestFramework.AbstractTest {
        public ExcessiveGLPTest() {
            super("Excessive GLP Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Get vehicle and depot
            Vehicle vehicle = env.getVehicles().get(0);
            Depot depot = env.getMainDepot();

            // Set vehicle's current GLP near capacity
            int capacity = vehicle.getGlpCapacityM3();
            vehicle.setCurrentGlpM3(capacity - 5);

            // Create route that tries to recharge more than capacity allows
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new DepotStop(depot, 10)); // Try to add 10 GLP when only 5 space left

            Route route = new Route(vehicle, stops, baseTime);
            List<Route> routes = new ArrayList<>();
            routes.add(route);

            Solution solution = new Solution(routes, env);

            // Verify result is negative infinity
            assertTrue(Double.isInfinite(solution.getScore()) && solution.getScore() < 0,
                    "Score should be negative infinity when vehicle has excessive GLP");
        }
    }

    /**
     * Test for case where no path is found due to blockages
     */
    private static class NoPathTest extends TestFramework.AbstractTest {
        public NoPathTest() {
            super("No Path Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment with blockages
            Environment env = TestUtilities.createEnvironmentWithBlockages();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create order on the other side of blockages
            List<Order> orders = new ArrayList<>();
            orders.add(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(15, 15)));
            env.addOrders(orders);

            // Create vehicle
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(15);
            vehicle.setCurrentPosition(new Position(0, 0));

            // Create route that tries to go through blocked path
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(15, 15)),
                    new Position(15, 15), 10));

            Route route = new Route(vehicle, stops, baseTime);
            List<Route> routes = new ArrayList<>();
            routes.add(route);

            Solution solution = new Solution(routes, env);

            // Verify result is negative infinity
            assertTrue(Double.isInfinite(solution.getScore()) && solution.getScore() < 0,
                    "Score should be negative infinity when no path is found");
        }
    }

    /**
     * Test for case where order gets more GLP than requested
     */
    private static class ExcessiveDeliveryTest extends TestFramework.AbstractTest {
        public ExcessiveDeliveryTest() {
            super("Excessive Delivery Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create orders
            List<Order> orders = new ArrayList<>();
            orders.add(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)));
            env.addOrders(orders);

            // Create vehicle with sufficient GLP
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(20);

            // Create route that delivers more than requested
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)),
                    new Position(10, 10), 15)); // Try to deliver 15 when only 10 requested

            Route route = new Route(vehicle, stops, baseTime);
            List<Route> routes = new ArrayList<>();
            routes.add(route);

            Solution solution = new Solution(routes, env);

            // Verify result is negative infinity
            assertTrue(Double.isInfinite(solution.getScore()) && solution.getScore() < 0,
                    "Score should be negative infinity when delivery exceeds order amount");
        }
    }

    private static class OrderDeliveryVerificationTest extends TestFramework.AbstractTest {
        public OrderDeliveryVerificationTest() {
            super("Order Delivery Verification Test");
        }

        @Override
        protected void runTest() throws Throwable {
            // Create test environment
            Environment env = TestUtilities.createSampleEnvironment();
            LocalDateTime baseTime = env.getCurrentTime();

            // Create orders with different deadlines
            List<Order> orders = new ArrayList<>();
            orders.add(new Order("O1", baseTime, baseTime.plusHours(4), 10, new Position(10, 10)));
            orders.add(new Order("O2", baseTime, baseTime.plusHours(2), 15, new Position(20, 20)));
            env.addOrders(orders);

            // Test case 1: All orders delivered completely and on time
            {
                Vehicle vehicle = env.getVehicles().get(0);
                vehicle.setCurrentGlpM3(30); // Enough capacity for both orders

                List<RouteStop> stops = new ArrayList<>();
                stops.add(new OrderStop(orders.get(0), new Position(10, 10), 10));
                stops.add(new OrderStop(orders.get(1), new Position(20, 20), 15));

                Route route = new Route(vehicle, stops, baseTime);
                List<Route> routes = new ArrayList<>();
                routes.add(route);

                Solution solution = new Solution(routes, env);
                
                // This should not throw an exception
                solution.verifyOrderDeliveries();
                assertTrue(true, "Valid solution should pass verification");
            }

            // Test case 2: Incomplete delivery (order 1 gets less than requested)
            {
                Vehicle vehicle = env.getVehicles().get(1);
                vehicle.setCurrentGlpM3(20); // Not enough for both orders

                List<RouteStop> stops = new ArrayList<>();
                stops.add(new OrderStop(orders.get(0), new Position(10, 10), 5)); // Only 5 instead of 10
                stops.add(new OrderStop(orders.get(1), new Position(20, 20), 15));

                Route route = new Route(vehicle, stops, baseTime);
                List<Route> routes = new ArrayList<>();
                routes.add(route);

                Solution solution = new Solution(routes, env);
                
                // This should throw an AssertionError
                boolean exceptionThrown = false;
                try {
                    solution.verifyOrderDeliveries();
                } catch (AssertionError e) {
                    exceptionThrown = true;
                    assertTrue(e.getMessage().contains("incomplete"), 
                        "Exception message should mention incomplete delivery");
                }
                assertTrue(exceptionThrown, "Incomplete delivery should cause verification to fail");
            }

            // Test case 3: Late delivery (order 2 delivered after deadline)
            {
                Vehicle vehicle = env.getVehicles().get(2);
                vehicle.setCurrentGlpM3(30);

                List<RouteStop> stops = new ArrayList<>();
                stops.add(new OrderStop(orders.get(0), new Position(10, 10), 10));
                
                // Create a late route for order 2 (starting after its deadline)
                LocalDateTime lateTime = baseTime.plusHours(3); // After order 2's deadline
                Route route = new Route(vehicle, stops, baseTime);
                
                List<RouteStop> lateStops = new ArrayList<>();
                lateStops.add(new OrderStop(orders.get(1), new Position(20, 20), 15));
                Route lateRoute = new Route(vehicle, lateStops, lateTime);
                
                List<Route> routes = new ArrayList<>();
                routes.add(route);
                routes.add(lateRoute);

                Solution solution = new Solution(routes, env);
                
                // This should throw an AssertionError
                boolean exceptionThrown = false;
                try {
                    solution.verifyOrderDeliveries();
                } catch (AssertionError e) {
                    exceptionThrown = true;
                    assertTrue(e.getMessage().contains("late"), 
                        "Exception message should mention late delivery");
                }
                assertTrue(exceptionThrown, "Late delivery should cause verification to fail");
            }
        }
    }
}