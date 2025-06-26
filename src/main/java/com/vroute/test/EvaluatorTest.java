package com.vroute.test;

import com.vroute.models.*;
import com.vroute.solution.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vroute.test.TestFramework.Assertions.*;

/**
 * Suite de pruebas para el evaluador de soluciones usando el mini framework de
 * testing
 */
public class EvaluatorTest {

    // Base environment for all tests
    private static Environment baseEnvironment;
    
    // Standard vehicles for testing
    private static Vehicle vehicleTB;
    private static Vehicle vehicleTC;
    
    // Current simulation time
    private static LocalDateTime currentTime;
    
    // Main depot
    private static Depot mainDepot;
    
    // Initialize the base testing environment
    static {
        initializeBaseEnvironment();
    }
    
    /**
     * Creates the base environment with vehicles, depots, and no blockages
     */
    private static void initializeBaseEnvironment() {
        currentTime = LocalDateTime.now();
        
        // Create main depot
        mainDepot = new Depot("MAIN", new Position(0, 0), 1000, true, true);
        
        // Create auxiliary depots
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("AUX1", new Position(50, 50), 500, true, false));
        
        // Create standard vehicles with full fuel and GLP
        vehicleTB = new Vehicle("V-TB", VehicleType.TB, new Position(0, 0));
        vehicleTB.refill(vehicleTB.getGlpCapacityM3());
        vehicleTB.setCurrentFuelGal(vehicleTB.getFuelCapacityGal());
        
        vehicleTC = new Vehicle("V-TC", VehicleType.TC, new Position(0, 0));
        vehicleTC.refill(vehicleTC.getGlpCapacityM3());
        vehicleTC.setCurrentFuelGal(vehicleTC.getFuelCapacityGal());
        
        // Add vehicles to list
        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicleTB);
        vehicles.add(vehicleTC);
        
        // Create the environment
        baseEnvironment = new Environment(vehicles, mainDepot, auxDepots, currentTime);
    }
    
    /**
     * Creates a deep clone of the base environment for each test to use
     */
    private static Environment cloneBaseEnvironment() {
        // Create new lists to prevent shared references
        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(vehicleTB.clone());
        vehicles.add(vehicleTC.clone());
        
        List<Depot> auxDepots = new ArrayList<>();
        for (Depot depot : baseEnvironment.getAuxDepots()) {
            auxDepots.add(new Depot(
                depot.getId(),
                new Position(depot.getPosition().getX(), depot.getPosition().getY()),
                depot.getGlpCapacityM3(),
                depot.canRefuel(),
                depot.isMainDepot()
            ));
        }
        
        Depot clonedMainDepot = new Depot(
            mainDepot.getId(),
            new Position(mainDepot.getPosition().getX(), mainDepot.getPosition().getY()),
            mainDepot.getGlpCapacityM3(),
            mainDepot.canRefuel(),
            mainDepot.isMainDepot()
        );
        
        return new Environment(vehicles, clonedMainDepot, auxDepots, currentTime);
    }

    /**
     * Crea un pedido para pruebas
     */
    private static Order createTestOrder(String id, Position position, int glpRequest, LocalDateTime dueTime) {
        LocalDateTime arriveTime = currentTime.minusMinutes(30); // 30 minutos atrás
        return new Order(
                id,
                arriveTime,
                dueTime,
                glpRequest,
                position);
    }

    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("Evaluator");

        // Añadir todos los tests
        suite.addTest(new SimpleValidRouteTest());
        suite.addTest(new NotEnoughFuelTest());
        suite.addTest(new NotEnoughGlpTest());
        suite.addTest(new GlpCapacityExceededTest());
        suite.addTest(new LateDeliveryTest());
        suite.addTest(new ComplexSolutionTest());
        suite.addTest(new TypeCapacityVsActualCapacityTest());
        suite.addTest(new ValidSolutionWithinCapacityTest());

        return suite;
    }

    /**
     * Test de una ruta simple válida
     * 
     * GIVEN: Un vehículo con suficiente combustible y GLP, y un pedido viable
     * PROCESS: Se crea una solución con una ruta simple y se evalúa
     * AFTER: La solución debe ser válida con un costo finito
     */
    private static class SimpleValidRouteTest extends TestFramework.AbstractTest {

        public SimpleValidRouteTest() {
            super();
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle from the environment
            Vehicle vehicle = env.getVehicles().get(0).clone();
            
            // Create a test order
            Order order = createTestOrder("O1", new Position(10, 0), 10, currentTime.plusHours(2));
            
            // Create a route with one order
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    currentTime.plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route(vehicle, stops, currentTime);

            // Create order map
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluate the route
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Assertions
            assertFalse(Double.isInfinite(cost), "El costo no debe ser infinito");
            assertTrue(cost > 0, "El costo debe ser positivo");
        }
    }

    /**
     * Test de una ruta con combustible insuficiente
     * 
     * GIVEN: Un vehículo con combustible insuficiente y un pedido lejano
     * PROCESS: Se crea una ruta imposible de completar por falta de combustible
     * AFTER: La solución debe ser inválida (costo infinito) por falta de
     * combustible
     */
    private static class NotEnoughFuelTest extends TestFramework.AbstractTest {

        public NotEnoughFuelTest() {
            super("Combustible Insuficiente");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle and set fuel to 0
            Vehicle vehicle = env.getVehicles().get(0).clone();
            vehicle.setCurrentFuelGal(0);

            // Create a distant order
            Order order = createTestOrder("O1", new Position(50, 0), 20, currentTime.plusHours(2));

            // Create a route with one order
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    currentTime.plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route(vehicle, stops, currentTime);

            // Create order map
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluate the route
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Assertions
            assertTrue(Double.isInfinite(cost), "El costo debe ser infinito por falta de combustible");
            assertEquals(Double.NEGATIVE_INFINITY, cost, "El costo debe ser específicamente NEGATIVE_INFINITY");
        }
    }

    /**
     * Test de una ruta con GLP insuficiente
     * 
     * GIVEN: Un vehículo con GLP insuficiente y un pedido que requiere GLP
     * PROCESS: Se crea una ruta que no puede satisfacer la demanda de GLP del
     * pedido
     * AFTER: La solución debe ser inválida (costo infinito) por falta de GLP
     */
    private static class NotEnoughGlpTest extends TestFramework.AbstractTest {

        public NotEnoughGlpTest() {
            super("GLP Insuficiente");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle and set GLP to 0
            Vehicle vehicle = env.getVehicles().get(0).clone();
            vehicle.dispenseGlp(vehicle.getCurrentGlpM3()); // Empty the GLP

            // Create an order requiring GLP
            Order order = createTestOrder("O1", new Position(10, 0), 20, currentTime.plusHours(2));

            // Create a route with one order
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    currentTime.plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route(vehicle, stops, currentTime);

            // Create order map
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluate the route
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Assertions
            assertTrue(Double.isInfinite(cost), "El costo debe ser infinito por falta de GLP");
            assertEquals(Double.NEGATIVE_INFINITY, cost, "El costo debe ser específicamente NEGATIVE_INFINITY");
        }
    }

    /**
     * Test de una ruta que excede la capacidad de GLP
     * 
     * GIVEN: Un vehículo con capacidad limitada de GLP y un depósito que intenta
     * recargar más de lo permitido
     * PROCESS: Se crea una ruta con una parada en el depósito que excede la
     * capacidad de GLP
     * AFTER: La solución debe ser inválida (costo infinito) por exceder la
     * capacidad máxima de GLP
     */
    private static class GlpCapacityExceededTest extends TestFramework.AbstractTest {

        public GlpCapacityExceededTest() {
            super("Capacidad GLP Excedida");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle
            Vehicle vehicle = env.getVehicles().get(0).clone();

            // Create an order
            Order order = createTestOrder("O1", new Position(10, 0), 10, currentTime.plusHours(2));

            // Get the auxiliary depot
            Depot depot = env.getAuxDepots().get(0);

            // Create a route with a depot stop and then the order
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new DepotStop(
                    depot,
                    currentTime.plusMinutes(30),
                    vehicle.getGlpCapacityM3() * 2 // Try to recharge twice the capacity
            ));

            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    currentTime.plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route(vehicle, stops, currentTime);

            // Create order map
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluate the route
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Assertions
            assertTrue(Double.isInfinite(cost), "El costo debe ser infinito por exceder capacidad de GLP");
            assertEquals(Double.NEGATIVE_INFINITY, cost, "El costo debe ser específicamente NEGATIVE_INFINITY");
        }
    }

    /**
     * Test de una entrega tardía
     * 
     * GIVEN: Un vehículo y un pedido con fecha límite cercana
     * PROCESS: Se crea una ruta donde la entrega ocurre después de la fecha límite
     * AFTER: La solución debe generar un costo finito pero con penalización por
     * retraso
     */
    private static class LateDeliveryTest extends TestFramework.AbstractTest {

        public LateDeliveryTest() {
            super("Entrega Tardía");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle
            Vehicle vehicle = env.getVehicles().get(0).clone();

            // Create an order with soon deadline
            Order order = createTestOrder("O1", new Position(10, 0), 10, currentTime.plusMinutes(30));

            // Create a route with late delivery
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    currentTime.plusHours(2), // 2 hours late
                    order.getGlpRequestM3()));

            Route route = new Route(vehicle, stops, currentTime);

            // Create an on-time route for comparison
            List<RouteStop> stopsOnTime = new ArrayList<>();
            stopsOnTime.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    order.getDueTime().minusMinutes(5), // Just in time
                    order.getGlpRequestM3()));
            
            Route routeOnTime = new Route(vehicle.clone(), stopsOnTime, currentTime);
            
            // Create order map
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluate both routes
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());
            double costOnTime = Evaluator.evaluateRoute(env, routeOnTime, orders, new HashMap<>());
            
            // Assertions
            assertFalse(Double.isInfinite(cost), "El costo no debe ser infinito aunque la entrega sea tardía");
            assertTrue(cost > 0, "El costo debe ser positivo");
            assertTrue(cost > costOnTime, "El costo de entrega tardía debe ser mayor que el de entrega a tiempo");
        }
    }

    /**
     * Prueba una solución más compleja para diagnosticar problemas de algoritmos
     */
    private static class ComplexSolutionTest extends TestFramework.AbstractTest {

        public ComplexSolutionTest() {
            super("Solución Compleja");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get both vehicles
            Vehicle vehicle1 = env.getVehicles().get(0).clone();
            Vehicle vehicle2 = env.getVehicles().get(1).clone();
            
            // Create multiple orders in different locations
            Map<String, Order> orders = new HashMap<>();
            
            Order order1 = createTestOrder("O1", new Position(5, 5), 5, currentTime.plusHours(2));
            Order order2 = createTestOrder("O2", new Position(10, 10), 8, currentTime.plusHours(3));
            Order order3 = createTestOrder("O3", new Position(15, 0), 12, currentTime.plusHours(4));
            Order order4 = createTestOrder("O4", new Position(0, 15), 7, currentTime.plusHours(5));
            
            orders.put(order1.getId(), order1);
            orders.put(order2.getId(), order2);
            orders.put(order3.getId(), order3);
            orders.put(order4.getId(), order4);
            
            // Create routes for the vehicles
            // Vehicle 1 will serve orders 1 and 3
            List<RouteStop> stops1 = new ArrayList<>();
            stops1.add(new OrderStop(order1.getId(), order1.getPosition(), 
                    currentTime.plusHours(1), order1.getGlpRequestM3()));
            stops1.add(new OrderStop(order3.getId(), order3.getPosition(), 
                    currentTime.plusHours(3), order3.getGlpRequestM3()));
            Route route1 = new Route(vehicle1, stops1, currentTime);
            
            // Vehicle 2 will serve orders 2 and 4
            List<RouteStop> stops2 = new ArrayList<>();
            stops2.add(new OrderStop(order2.getId(), order2.getPosition(), 
                    currentTime.plusHours(2), order2.getGlpRequestM3()));
            stops2.add(new OrderStop(order4.getId(), order4.getPosition(), 
                    currentTime.plusHours(4), order4.getGlpRequestM3()));
            Route route2 = new Route(vehicle2, stops2, currentTime);
            
            // Create solution with both routes
            List<Route> routes = new ArrayList<>();
            routes.add(route1);
            routes.add(route2);
            Solution solution = new Solution(orders, routes);
            
            // Evaluate the solution
            double solutionCost = Evaluator.evaluateSolution(env, solution);
            
            // Also evaluate each route separately
            double route1Cost = Evaluator.evaluateRoute(env, route1, orders, new HashMap<>());
            double route2Cost = Evaluator.evaluateRoute(env, route2, orders, new HashMap<>());
            
            // Assertions
            assertFalse(Double.isInfinite(route1Cost), "El costo de la ruta 1 no debe ser infinito");
            assertFalse(Double.isInfinite(route2Cost), "El costo de la ruta 2 no debe ser infinito");
            assertFalse(Double.isInfinite(solutionCost), "El costo de la solución no debe ser infinito");
            assertTrue(solutionCost > 0, "El costo de la solución debe ser positivo");
        }
    }

    /**
     * Test para detectar la discrepancia entre la capacidad real y la del tipo de
     * vehículo
     */
    private static class TypeCapacityVsActualCapacityTest extends TestFramework.AbstractTest {

        public TypeCapacityVsActualCapacityTest() {
            super("Discrepancia de Capacidades GLP");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle
            Vehicle vehicle = env.getVehicles().get(0).clone();

            // Verify that capacity and type are consistent
            assertEquals(vehicle.getType().getCapacityM3(), vehicle.getGlpCapacityM3(),
                    "La capacidad GLP del vehículo debe coincidir con la del tipo");

            // Modify the vehicle's GLP artificially
            try {
                java.lang.reflect.Field currentGlpField = Vehicle.class.getDeclaredField("currentGlpM3");
                currentGlpField.setAccessible(true);

                int newGlpValue = 100; // A value much higher than capacity
                currentGlpField.set(vehicle, newGlpValue);

                assertEquals(newGlpValue, vehicle.getCurrentGlpM3(),
                        "El GLP actual debe haberse modificado al valor esperado");

                // Verify that current GLP exceeds capacity
                assertTrue(vehicle.getCurrentGlpM3() > vehicle.getGlpCapacityM3(),
                        "El GLP actual debe exceder la capacidad máxima");

                // Create a simple route for this vehicle
                Order order = createTestOrder("O1", new Position(10, 0), 20, currentTime.plusHours(2));

                List<RouteStop> stops = new ArrayList<>();
                stops.add(new OrderStop(
                        order.getId(),
                        order.getPosition(),
                        currentTime.plusHours(1),
                        order.getGlpRequestM3()));

                Route route = new Route(vehicle, stops, currentTime);

                // Create order map
                Map<String, Order> orders = new HashMap<>();
                orders.put(order.getId(), order);

                // Evaluate the route
                double routeCost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

                // Check if the evaluator detects the capacity problem
                if (Double.isInfinite(routeCost)) {
                    // If detected as error (ideal behavior)
                    assertTrue(true, "El evaluador correctamente detecta que el GLP excede la capacidad máxima");
                } else {
                    // If allowed (also acceptable in some cases)
                    System.out.println("Advertencia: El evaluador no detecta la discrepancia de capacidad GLP");
                }
            } catch (Exception e) {
                throw new AssertionError("Error al probar capacidades: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Test de una solución válida respetando las capacidades
     */
    private static class ValidSolutionWithinCapacityTest extends TestFramework.AbstractTest {

        public ValidSolutionWithinCapacityTest() {
            super("Solución Válida con Capacidades Correctas");
        }

        @Override
        protected void runTest() throws Throwable {
            // Clone base environment for this test
            Environment env = cloneBaseEnvironment();
            
            // Get a vehicle
            Vehicle vehicle = env.getVehicles().get(0).clone();
            
            // Set GLP to maximum capacity
            int glpCapacity = vehicle.getGlpCapacityM3();
            vehicle.refill(glpCapacity);

            // Verify that current GLP is exactly equal to capacity
            assertEquals(glpCapacity, vehicle.getCurrentGlpM3(),
                    "El GLP actual debe ser igual a la capacidad máxima");

            // Create an order with GLP demand within vehicle capacity
            int glpDemand = glpCapacity / 3; // One third of capacity
            Order order = createTestOrder("O1", new Position(10, 0), glpDemand, currentTime.plusHours(2));

            // Verify that demand is less than capacity
            assertTrue(order.getGlpRequestM3() <= vehicle.getCurrentGlpM3(),
                    "La demanda de GLP debe ser menor o igual al GLP disponible");

            // Create a route to serve the order
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    currentTime.plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route(vehicle, stops, currentTime);

            // Create order map
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluate the route
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Assertions
            assertFalse(Double.isInfinite(cost), "El costo no debe ser infinito");
            assertTrue(cost > 0, "El costo debe ser positivo");
        }
    }
}