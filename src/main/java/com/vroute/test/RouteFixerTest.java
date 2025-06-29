package com.vroute.test;

import java.util.ArrayList;
import java.util.List;

import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Position;
import com.vroute.models.Vehicle;
import com.vroute.solution.DepotStop;
import com.vroute.solution.OrderStop;
import com.vroute.solution.Route;
import com.vroute.solution.RouteStop;
import com.vroute.taboo.RouteFixer;
import com.vroute.test.TestFramework.Assertions;

public class RouteFixerTest {
    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("RouteFixerTest");
        suite.addTest(new FixRouteWithNoGLP());
        suite.addTest(new FixRouteWithEnoughGLP());
        suite.addTest(new FixRouteWithNoFuel());
        suite.addTest(new FixRouteWithMultipleDepotStops());
        suite.addTest(new FixRouteWithNoSolution());
        suite.addTest(new FixRouteWithBlockages());
        // Add new test cases for GLP capacity constraints
        suite.addTest(new EdgeCaseVehicleAtCapacity());
        suite.addTest(new MultipleDifferentVehicleTypes());
        suite.addTest(new GlpEdgeCapacityCases());
        suite.addTest(new LargeOrderSplitBetweenDepotStops());
        return suite;
    }

    private static class FixRouteWithNoGLP extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            // Fill the main depot with GLP
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            // Create a vehicle with no GLP
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(0);
            vehicle.refuel(); // Make sure it has enough fuel

            // Create order stops that require GLP
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 25, new Position(5, 5)),
                    new Position(5, 5), 25));

            // Fix the route - should add a depot stop before the order
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null");

            // Verify route has at least 2 stops: depot and order
            Assertions.assertTrue(fixedRoute.getStops().size() >= 2, "Route should have at least 2 stops");

            // Verify first stop is a depot stop
            Assertions.assertTrue(fixedRoute.getStops().get(0) instanceof DepotStop, "First stop should be a depot");

            // Verify second stop is the order
            Assertions.assertTrue(fixedRoute.getStops().get(1) instanceof OrderStop, "Second stop should be the order");

            // Verify GLP was loaded at the depot
            DepotStop depotStop = (DepotStop) fixedRoute.getStops().get(0);
            Assertions.assertTrue(depotStop.getGlpRecharge() > 0, "Vehicle should have loaded GLP at depot");
        }
    }

    private static class FixRouteWithEnoughGLP extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }

            // Create a vehicle with enough GLP
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(25); // Fill GLP
            vehicle.refuel(); // Make sure it has enough fuel

            // Create order stops
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 10, new Position(5, 5)),
                    new Position(5, 5), 10));
            orderStops.add(new OrderStop(
                    new Order("O2", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 25, new Position(10, 10)),
                    new Position(10, 10), 15));

            // Fix the route - should not add any depot stops
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null");

            // Verify route has exactly 2 stops (the orders, no depots)
            Assertions.assertEquals(2, fixedRoute.getStops().size(), "Route should have exactly 2 stops");

            // Verify both stops are order stops
            for (RouteStop stop : fixedRoute.getStops()) {
                Assertions.assertTrue(stop instanceof OrderStop, "Stop should be an order");
            }
        }
    }

    private static class FixRouteWithNoFuel extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }
            // Create a vehicle with GLP but no fuel
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(vehicle.getGlpCapacityM3()); // Fill GLP
            vehicle.setCurrentFuelGal(0); // No fuel

            // Create order stops
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 15, new Position(20, 20)),
                    new Position(20, 20), 15));

            // Fix the route - should add a depot stop for refueling
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null");

            // Verify route has at least 2 stops: fuel depot and order
            Assertions.assertTrue(fixedRoute.getStops().size() >= 2, "Route should have at least 2 stops");

            // Verify the route contains a depot stop
            boolean hasDepotStop = false;
            for (RouteStop stop : fixedRoute.getStops()) {
                if (stop instanceof DepotStop) {
                    hasDepotStop = true;
                    break;
                }
            }
            Assertions.assertTrue(hasDepotStop, "Route should contain a depot stop for refueling");

            // Verify the vehicle has enough fuel at the end
            Assertions.assertTrue(fixedRoute.getVehicle().getCurrentFuelGal() > 0,
                    "Vehicle should have fuel at the end");
        }
    }

    private static class FixRouteWithMultipleDepotStops extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            for (Depot depot : env.getAuxDepots()) {
                depot.refillGLP();
            }
            // Create a vehicle with minimal GLP and fuel
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(0); // Some GLP
            vehicle.setCurrentFuelGal(5); // Minimal fuel

            // Create order stops that require more GLP than available and are far apart
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 25, new Position(15, 15)),
                    new Position(15, 15), 25));
            orderStops.add(new OrderStop(
                    new Order("O2", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 25, new Position(30, 30)),
                    new Position(30, 30), 25));
            orderStops.add(new OrderStop(
                    new Order("O3", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 25, new Position(45, 45)),
                    new Position(45, 45), 25));
            orderStops.add(new OrderStop(
                    new Order("O4", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 25, new Position(70, 50)),
                    new Position(70, 50), 25));

            // Fix the route - should add multiple depot stops
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null");

            // Count depot stops
            int depotStops = 0;
            for (RouteStop stop : fixedRoute.getStops()) {
                if (stop instanceof DepotStop) {
                    depotStops++;
                }
            }

            // Verify there are multiple depot stops
            Assertions.assertTrue(depotStops >= 2, "Route should have multiple depot stops");
        }
    }

    private static class FixRouteWithNoSolution extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();

            // Create a vehicle with no GLP
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(0);
            vehicle.refuel();

            // Create order stops that require GLP
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(new Order("EXCESS_GLP_O1", env.getCurrentTime(),
                    env.getCurrentTime().plusHours(4), 100, new Position(5, 5)), new Position(5, 5), 100));

            // Fix the route - should return null as there's no GLP source
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is null
            Assertions.assertNull(fixedRoute, "Route should be null as there's no solution");
        }
    }

    private static class FixRouteWithBlockages extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();

            // Create a vehicle with enough GLP and fuel
            Vehicle vehicle = env.getVehicles().get(0);
            vehicle.setCurrentGlpM3(vehicle.getGlpCapacityM3()); // Fill GLP
            vehicle.refuel(); // Fill fuel

            // Create order stops that would require passing through blockages
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 5, new Position(15, 15)),
                    new Position(15, 15), 5));

            // Fix the route - should navigate around blockages
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null despite blockages");

            // Verify the route contains the order
            boolean hasOrderStop = false;
            for (RouteStop stop : fixedRoute.getStops()) {
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    if (orderStop.getOrder().getId().equals("O1")) {
                        hasOrderStop = true;
                        break;
                    }
                }
            }
            Assertions.assertTrue(hasOrderStop, "Route should contain the order despite blockages");
        }
    }

    /**
     * Test for edge case when vehicle is exactly at capacity
     */
    private static class EdgeCaseVehicleAtCapacity extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            
            // Create a vehicle with GLP filled exactly to capacity
            Vehicle vehicle = env.getVehicles().get(0);
            int capacity = vehicle.getGlpCapacityM3();
            vehicle.setCurrentGlpM3(capacity);
            vehicle.refuel();

            // Create order stops
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), capacity, new Position(5, 5)),
                    new Position(5, 5), capacity));

            // Fix the route
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());

            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null");
            
            // Verify route has exactly 1 stop (the order)
            Assertions.assertEquals(1, fixedRoute.getStops().size(), "Route should have exactly 1 stop");
            
            // Verify the stop is an order stop
            Assertions.assertTrue(fixedRoute.getStops().get(0) instanceof OrderStop, "Stop should be an order");
            
            // Verify the vehicle GLP is now empty
            Assertions.assertEquals(0, fixedRoute.getVehicle().getCurrentGlpM3(), 
                    "Vehicle should have 0 GLP after delivery");
        }
    }

    /**
     * Test for handling multiple vehicle types with different capacities
     */
    private static class MultipleDifferentVehicleTypes extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            
            // Get vehicles of different types (TA, TB, TC, TD)
            Vehicle taVehicle = null;
            Vehicle tbVehicle = null;
            Vehicle tcVehicle = null;
            Vehicle tdVehicle = null;
            
            for (Vehicle v : env.getVehicles()) {
                if (v.getType().name().equals("TA") && taVehicle == null) {
                    taVehicle = v;
                } else if (v.getType().name().equals("TB") && tbVehicle == null) {
                    tbVehicle = v;
                } else if (v.getType().name().equals("TC") && tcVehicle == null) {
                    tcVehicle = v;
                } else if (v.getType().name().equals("TD") && tdVehicle == null) {
                    tdVehicle = v;
                }
                
                if (taVehicle != null && tbVehicle != null && tcVehicle != null && tdVehicle != null) {
                    break;
                }
            }
            
            // Ensure we have all vehicle types
            Assertions.assertNotNull(taVehicle, "TA vehicle should exist");
            Assertions.assertNotNull(tbVehicle, "TB vehicle should exist");
            Assertions.assertNotNull(tcVehicle, "TC vehicle should exist");
            Assertions.assertNotNull(tdVehicle, "TD vehicle should exist");
            
            // Set all vehicles to empty GLP but full fuel
            taVehicle.setCurrentGlpM3(0);
            tbVehicle.setCurrentGlpM3(0);
            tcVehicle.setCurrentGlpM3(0);
            tdVehicle.setCurrentGlpM3(0);
            
            taVehicle.refuel();
            tbVehicle.refuel();
            tcVehicle.refuel();
            tdVehicle.refuel();
            
            // Create order stops for each vehicle type with different GLP amounts
            // that match exactly each vehicle's capacity
            List<OrderStop> taOrderStops = new ArrayList<>();
            taOrderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            taVehicle.getGlpCapacityM3(), new Position(5, 5)),
                    new Position(5, 5), taVehicle.getGlpCapacityM3()));
            
            List<OrderStop> tbOrderStops = new ArrayList<>();
            tbOrderStops.add(new OrderStop(
                    new Order("O2", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            tbVehicle.getGlpCapacityM3(), new Position(10, 10)),
                    new Position(10, 10), tbVehicle.getGlpCapacityM3()));
            
            List<OrderStop> tcOrderStops = new ArrayList<>();
            tcOrderStops.add(new OrderStop(
                    new Order("O3", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            tcVehicle.getGlpCapacityM3(), new Position(15, 15)),
                    new Position(15, 15), tcVehicle.getGlpCapacityM3()));
            
            List<OrderStop> tdOrderStops = new ArrayList<>();
            tdOrderStops.add(new OrderStop(
                    new Order("O4", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            tdVehicle.getGlpCapacityM3(), new Position(20, 20)),
                    new Position(20, 20), tdVehicle.getGlpCapacityM3()));
            
            // Fix routes for each vehicle
            Route taRoute = RouteFixer.fixRoute(env, taOrderStops, taVehicle, env.getCurrentTime());
            Route tbRoute = RouteFixer.fixRoute(env, tbOrderStops, tbVehicle, env.getCurrentTime());
            Route tcRoute = RouteFixer.fixRoute(env, tcOrderStops, tcVehicle, env.getCurrentTime());
            Route tdRoute = RouteFixer.fixRoute(env, tdOrderStops, tdVehicle, env.getCurrentTime());
            
            // Verify all routes are valid
            Assertions.assertNotNull(taRoute, "TA route should not be null");
            Assertions.assertNotNull(tbRoute, "TB route should not be null");
            Assertions.assertNotNull(tcRoute, "TC route should not be null");
            Assertions.assertNotNull(tdRoute, "TD route should not be null");
            
            // For each route, verify:
            // 1. Route has at least 2 stops (depot and order)
            // 2. First stop is a depot
            // 3. Last stop is the order
            // 4. Vehicle GLP is 0 at the end
            
            // TA route checks
            Assertions.assertTrue(taRoute.getStops().size() >= 2, "TA route should have at least 2 stops");
            Assertions.assertTrue(taRoute.getStops().get(0) instanceof DepotStop, "First TA stop should be a depot");
            Assertions.assertTrue(taRoute.getStops().get(taRoute.getStops().size() - 1) instanceof OrderStop, 
                    "Last TA stop should be an order");
            Assertions.assertEquals(0, taRoute.getVehicle().getCurrentGlpM3(), "TA vehicle should be empty at end");
            
            // TB route checks
            Assertions.assertTrue(tbRoute.getStops().size() >= 2, "TB route should have at least 2 stops");
            Assertions.assertTrue(tbRoute.getStops().get(0) instanceof DepotStop, "First TB stop should be a depot");
            Assertions.assertTrue(tbRoute.getStops().get(tbRoute.getStops().size() - 1) instanceof OrderStop, 
                    "Last TB stop should be an order");
            Assertions.assertEquals(0, tbRoute.getVehicle().getCurrentGlpM3(), "TB vehicle should be empty at end");
            
            // TC route checks
            Assertions.assertTrue(tcRoute.getStops().size() >= 2, "TC route should have at least 2 stops");
            Assertions.assertTrue(tcRoute.getStops().get(0) instanceof DepotStop, "First TC stop should be a depot");
            Assertions.assertTrue(tcRoute.getStops().get(tcRoute.getStops().size() - 1) instanceof OrderStop, 
                    "Last TC stop should be an order");
            Assertions.assertEquals(0, tcRoute.getVehicle().getCurrentGlpM3(), "TC vehicle should be empty at end");
            
            // TD route checks
            Assertions.assertTrue(tdRoute.getStops().size() >= 2, "TD route should have at least 2 stops");
            Assertions.assertTrue(tdRoute.getStops().get(0) instanceof DepotStop, "First TD stop should be a depot");
            Assertions.assertTrue(tdRoute.getStops().get(tdRoute.getStops().size() - 1) instanceof OrderStop, 
                    "Last TD stop should be an order");
            Assertions.assertEquals(0, tdRoute.getVehicle().getCurrentGlpM3(), "TD vehicle should be empty at end");
            
            // Verify depot stops have correct GLP amounts to match vehicle capacities
            DepotStop taDepotStop = (DepotStop) taRoute.getStops().get(0);
            DepotStop tbDepotStop = (DepotStop) tbRoute.getStops().get(0);
            DepotStop tcDepotStop = (DepotStop) tcRoute.getStops().get(0);
            DepotStop tdDepotStop = (DepotStop) tdRoute.getStops().get(0);
            
            Assertions.assertEquals(taVehicle.getGlpCapacityM3(), taDepotStop.getGlpRecharge(), 
                    "TA vehicle should recharge exact capacity");
            Assertions.assertEquals(tbVehicle.getGlpCapacityM3(), tbDepotStop.getGlpRecharge(), 
                    "TB vehicle should recharge exact capacity");
            Assertions.assertEquals(tcVehicle.getGlpCapacityM3(), tcDepotStop.getGlpRecharge(), 
                    "TC vehicle should recharge exact capacity");
            Assertions.assertEquals(tdVehicle.getGlpCapacityM3(), tdDepotStop.getGlpRecharge(), 
                    "TD vehicle should recharge exact capacity");
        }
    }
    
    /**
     * Test for various edge cases of GLP capacity
     */
    private static class GlpEdgeCapacityCases extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            
            // Create a vehicle
            Vehicle vehicle = env.getVehicles().get(0);
            int capacity = vehicle.getGlpCapacityM3();
            vehicle.refuel();
            
            // Test Case 1: Vehicle has just 1 less than capacity, needs to deliver exact capacity
            vehicle.setCurrentGlpM3(capacity - 1);
            
            List<OrderStop> orderStops1 = new ArrayList<>();
            orderStops1.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            capacity, new Position(5, 5)),
                    new Position(5, 5), capacity));
            
            Route route1 = RouteFixer.fixRoute(env, orderStops1, vehicle, env.getCurrentTime());
            Assertions.assertNotNull(route1, "Route should not be null");
            
            // Verify the route has a depot stop
            boolean hasDepotStop = false;
            for (RouteStop stop : route1.getStops()) {
                if (stop instanceof DepotStop) {
                    hasDepotStop = true;
                    DepotStop depotStop = (DepotStop) stop;
                    // The RouteFixer might add extra GLP to avoid future depot visits
                    // So we check that it added at least the needed amount
                    Assertions.assertTrue(depotStop.getGlpRecharge() >= 1, 
                            "Should recharge at least 1 unit of GLP");
                    break;
                }
            }
            Assertions.assertTrue(hasDepotStop, "Route should have depot stop");
            
            // Test Case 2: Vehicle has 1 GLP, order needs 1 GLP
            vehicle.setCurrentGlpM3(1);
            
            List<OrderStop> orderStops2 = new ArrayList<>();
            orderStops2.add(new OrderStop(
                    new Order("O2", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            1, new Position(5, 5)),
                    new Position(5, 5), 1));
            
            Route route2 = RouteFixer.fixRoute(env, orderStops2, vehicle, env.getCurrentTime());
            Assertions.assertNotNull(route2, "Route should not be null");
            
            // Verify the route has exactly 1 stop (the order)
            Assertions.assertEquals(1, route2.getStops().size(), "Route should have exactly 1 stop");
            Assertions.assertTrue(route2.getStops().get(0) instanceof OrderStop, "Stop should be an order stop");
            
            // Test Case 3: Vehicle has exactly half capacity, order needs exactly capacity
            vehicle.setCurrentGlpM3(capacity / 2);
            
            List<OrderStop> orderStops3 = new ArrayList<>();
            orderStops3.add(new OrderStop(
                    new Order("O3", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            capacity, new Position(5, 5)),
                    new Position(5, 5), capacity));
            
            Route route3 = RouteFixer.fixRoute(env, orderStops3, vehicle, env.getCurrentTime());
            Assertions.assertNotNull(route3, "Route should not be null");
            
            // Verify the route has a depot stop
            hasDepotStop = false;
            for (RouteStop stop : route3.getStops()) {
                if (stop instanceof DepotStop) {
                    hasDepotStop = true;
                    DepotStop depotStop = (DepotStop) stop;
                    // The RouteFixer might add extra GLP to avoid future depot visits
                    // So we check that it added at least the needed amount
                    Assertions.assertTrue(depotStop.getGlpRecharge() >= capacity / 2, 
                            "Should recharge at least half capacity units of GLP");
                    break;
                }
            }
            Assertions.assertTrue(hasDepotStop, "Route should have depot stop");
        }
    }
    
    /**
     * Test for large order requiring multiple depot stops
     */
    private static class LargeOrderSplitBetweenDepotStops extends TestFramework.AbstractTest {
        @Override
        protected void runTest() throws Throwable {
            Environment env = TestUtilities.createSampleEnvironment();
            env.getMainDepot().refillGLP();
            
            // Create a vehicle
            Vehicle vehicle = env.getVehicles().get(0);
            int capacity = vehicle.getGlpCapacityM3();
            vehicle.setCurrentGlpM3(0);
            vehicle.refuel();
            
            // Create a large order that's more than 2x the vehicle capacity
            int largeOrderAmount = capacity * 3;
            
            // Create three order stops for the same large order
            List<OrderStop> orderStops = new ArrayList<>();
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            largeOrderAmount, new Position(5, 5)),
                    new Position(5, 5), capacity));
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            largeOrderAmount, new Position(5, 5)),
                    new Position(5, 5), capacity));
            orderStops.add(new OrderStop(
                    new Order("O1", env.getCurrentTime(), env.getCurrentTime().plusHours(4), 
                            largeOrderAmount, new Position(5, 5)),
                    new Position(5, 5), capacity));
            
            // Fix the route
            Route route = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());
            
            // Verify route is not null
            Assertions.assertNotNull(route, "Route should not be null");
            
            // Count the number of depot stops and order stops
            int depotStopCount = 0;
            int orderStopCount = 0;
            
            for (RouteStop stop : route.getStops()) {
                if (stop instanceof DepotStop) {
                    depotStopCount++;
                } else if (stop instanceof OrderStop) {
                    orderStopCount++;
                }
            }
            
            // Verify the route has at least 3 depot stops (one before each order)
            Assertions.assertTrue(depotStopCount >= 3, 
                    "Route should have at least 3 depot stops for GLP");
            
            // Verify the route has exactly 3 order stops
            Assertions.assertEquals(3, orderStopCount, 
                    "Route should have exactly 3 order stops");
            
            // Verify alternative: route has at least one depot stop between each order
            boolean validDepotStopPattern = true;
            boolean lastWasOrder = false;
            
            for (RouteStop stop : route.getStops()) {
                if (stop instanceof OrderStop) {
                    if (lastWasOrder) {
                        validDepotStopPattern = false;
                        break;
                    }
                    lastWasOrder = true;
                } else if (stop instanceof DepotStop) {
                    lastWasOrder = false;
                }
            }
            
            Assertions.assertTrue(validDepotStopPattern, 
                    "There should be at least one depot stop between consecutive order stops");
            
            // Verify the vehicle ends with 0 GLP
            Assertions.assertEquals(0, route.getVehicle().getCurrentGlpM3(), 
                    "Vehicle should have 0 GLP at the end");
        }
    }
}
