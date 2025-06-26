package com.vroute.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.vroute.models.Depot;
import com.vroute.models.Environment;
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
            orderStops.add(new OrderStop("O1", new Position(5, 5), LocalDateTime.now(), 25));
            
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
            orderStops.add(new OrderStop("O1", new Position(5, 5), LocalDateTime.now(), 10));
            orderStops.add(new OrderStop("O2", new Position(10, 10), LocalDateTime.now(), 15));
            
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
            orderStops.add(new OrderStop("O1", new Position(20, 20), LocalDateTime.now(), 15));
            
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
            Assertions.assertTrue(fixedRoute.getVehicle().getCurrentFuelGal() > 0, "Vehicle should have fuel at the end");
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
            orderStops.add(new OrderStop("O1", new Position(15, 15), LocalDateTime.now(), 25));
            orderStops.add(new OrderStop("O2", new Position(30, 30), LocalDateTime.now(), 25));
            orderStops.add(new OrderStop("O3", new Position(45, 45), LocalDateTime.now(), 25));
            orderStops.add(new OrderStop("O4", new Position(70, 50), LocalDateTime.now(), 25));
            
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
            orderStops.add(new OrderStop("O1", new Position(5, 5), LocalDateTime.now(), 100));
            
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
            orderStops.add(new OrderStop("O1", new Position(15, 15), LocalDateTime.now(), 5));
            
            // Fix the route - should navigate around blockages
            Route fixedRoute = RouteFixer.fixRoute(env, orderStops, vehicle, env.getCurrentTime());
            
            // Verify route is not null
            Assertions.assertNotNull(fixedRoute, "Route should not be null despite blockages");
            
            // Verify the route contains the order
            boolean hasOrderStop = false;
            for (RouteStop stop : fixedRoute.getStops()) {
                if (stop instanceof OrderStop) {
                    OrderStop orderStop = (OrderStop) stop;
                    if (orderStop.getEntityID().equals("O1")) {
                        hasOrderStop = true;
                        break;
                    }
                }
            }
            Assertions.assertTrue(hasOrderStop, "Route should contain the order despite blockages");
        }
    }
}
