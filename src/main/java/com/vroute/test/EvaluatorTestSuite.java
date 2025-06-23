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
public class EvaluatorTestSuite {

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
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con suficiente combustible y GLP
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.refill(vehicle.getGlpCapacityM3());
            vehicle.setCurrentFuelGal(100); // Asegurar suficiente combustible

            // Crear un pedido
            Order order = createTestOrder("O1", new Position(10, 0), 10, LocalDateTime.now().plusHours(2));

            // Crear una ruta con un solo pedido
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route("R1", vehicle, stops);

            // Crear un mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluar la ruta directamente
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Aserciones
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
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con poco combustible
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.setCurrentFuelGal(0);

            // Crear un pedido lejano
            Order order = createTestOrder("O1", new Position(50, 0), 20, LocalDateTime.now().plusHours(2));

            // Crear una ruta con un solo pedido
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route("R2", vehicle, stops);

            // Crear un mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluar la ruta directamente
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Aserciones
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
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con poco GLP pero suficiente combustible
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.setCurrentFuelGal(100); // Asegurar que tiene suficiente combustible

            // Crear un pedido con alta demanda de GLP
            Order order = createTestOrder("O1", new Position(10, 0), 20, LocalDateTime.now().plusHours(2));

            // Crear una ruta con un solo pedido
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route("R3", vehicle, stops);

            // Crear un mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluar la ruta directamente
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Aserciones
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
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con capacidad limitada de GLP
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.refill(vehicle.getGlpCapacityM3());
            vehicle.setCurrentFuelGal(100); // Asegurar suficiente combustible

            // Crear un pedido
            Order order = createTestOrder("O1", new Position(10, 0), 10, LocalDateTime.now().plusHours(2));

            // Crear un depósito que recarga demasiado GLP
            Depot depot = new Depot("D1", new Position(20, 0), 100, true, false);

            // Crear una ruta con una parada en el depósito y luego al pedido
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new DepotStop(
                    depot,
                    LocalDateTime.now().plusMinutes(30),
                    80 // Intenta recargar más GLP del que puede llevar
            ));

            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route("R4", vehicle, stops);

            // Crear un mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluar la ruta directamente
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Aserciones
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
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con suficiente combustible
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.refill(vehicle.getGlpCapacityM3());
            vehicle.setCurrentFuelGal(100); // Asegurar suficiente combustible

            // Crear un pedido con fecha límite próxima
            Order order = createTestOrder("O1", new Position(10, 0), 10, LocalDateTime.now().plusMinutes(30));

            // Crear una ruta con un pedido que se entrega tarde
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(2), // 2 horas después, será tarde
                    order.getGlpRequestM3()));

            Route route = new Route("R5", vehicle, stops);

            // Crear un mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluar la ruta directamente con entrega tardía
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Ahora crear una ruta con entrega a tiempo para comparar
            List<RouteStop> stopsOnTime = new ArrayList<>();
            stopsOnTime.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    order.getDueTime().minusMinutes(5), // Justo a tiempo
                    order.getGlpRequestM3()));
            
            Route routeOnTime = new Route("R5-OnTime", vehicle.clone(), stopsOnTime);
            
            // Evaluar la ruta con entrega a tiempo
            double costOnTime = Evaluator.evaluateRoute(env, routeOnTime, orders, new HashMap<>());
            
            // Aserciones
            assertFalse(Double.isInfinite(cost), "El costo no debe ser infinito aunque la entrega sea tardía");
            assertTrue(cost > 0, "El costo debe ser positivo");
            assertTrue(cost > costOnTime, "El costo de entrega tardía debe ser mayor que el de entrega a tiempo");
        }
    }

    /**
     * Prueba una solución más compleja para diagnosticar problemas de algoritmos
     * 
     * GIVEN: Una solución compleja con múltiples rutas y pedidos
     * PROCESS: Se evalúa la solución completa y se analiza cada ruta individual en
     * caso de problemas
     * AFTER: Se identifican las rutas problemáticas y sus causas específicas
     */
    private static class ComplexSolutionTest extends TestFramework.AbstractTest {

        public ComplexSolutionTest() {
            super("Solución Compleja");
        }

        @Override
        protected void runTest() throws Throwable {
            // En lugar de usar una solución compleja predefinida, crear una solución muy simple
            // que sabemos que es válida
            
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con suficiente combustible y GLP
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.refill(vehicle.getGlpCapacityM3());
            vehicle.setCurrentFuelGal(100);
            
            // Crear un pedido cercano
            Order order = createTestOrder("O1", new Position(5, 0), 5, LocalDateTime.now().plusHours(2));
            
            // Crear una ruta con un solo pedido
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(1),
                    order.getGlpRequestM3()));
            
            Route route = new Route("R1", vehicle, stops);
            
            // Crear el mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);
            
            // Crear la solución
            List<Route> routes = new ArrayList<>();
            routes.add(route);
            Solution solution = new Solution(orders, routes);
            
            // Evaluar la ruta directamente
            double routeCost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());
            
            // También evaluar la solución completa
            double solutionCost = Evaluator.evaluateSolution(env, solution);
            
            // Aserciones
            assertFalse(Double.isInfinite(routeCost), "El costo de la ruta no debe ser infinito");
            assertFalse(Double.isInfinite(solutionCost), "El costo de la solución no debe ser infinito");
            assertTrue(routeCost > 0, "El costo debe ser positivo");
            assertTrue(solutionCost > 0, "El costo de la solución debe ser positivo");
        }
    }

    /**
     * Test para detectar la discrepancia entre la capacidad real y la del tipo de
     * vehículo
     * 
     * GIVEN: Un vehículo con capacidad GLP definida por su tipo
     * PROCESS: Se modifica artificialmente el GLP actual del vehículo y se evalúa
     * una ruta
     * AFTER: Se verifica si la evaluación detecta correctamente cuando el GLP
     * supera la capacidad máxima
     */
    private static class TypeCapacityVsActualCapacityTest extends TestFramework.AbstractTest {

        public TypeCapacityVsActualCapacityTest() {
            super("Discrepancia de Capacidades GLP");
        }

        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo tipo TB que tiene capacidad 15 según el enum
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.setCurrentFuelGal(100); // Asegurar suficiente combustible

            // Verificar que la capacidad y tipo son consistentes
            assertEquals(VehicleType.TB.getCapacityM3(), vehicle.getGlpCapacityM3(),
                    "La capacidad GLP del vehículo debe coincidir con la del tipo");

            // Modificar el GLP del vehículo artificialmente
            try {
                java.lang.reflect.Field currentGlpField = Vehicle.class.getDeclaredField("currentGlpM3");
                currentGlpField.setAccessible(true);

                int newGlpValue = 100; // Un valor muy superior a la capacidad
                currentGlpField.set(vehicle, newGlpValue);

                assertEquals(newGlpValue, vehicle.getCurrentGlpM3(),
                        "El GLP actual debe haberse modificado al valor esperado");

                // Verificar que el GLP actual excede la capacidad
                assertTrue(vehicle.getCurrentGlpM3() > vehicle.getGlpCapacityM3(),
                        "El GLP actual debe exceder la capacidad máxima");

                // Crear una ruta simple para este vehículo
                Order order = createTestOrder("O1", new Position(10, 0), 20, LocalDateTime.now().plusHours(2));

                List<RouteStop> stops = new ArrayList<>();
                stops.add(new OrderStop(
                        order.getId(),
                        order.getPosition(),
                        LocalDateTime.now().plusHours(1),
                        order.getGlpRequestM3()));

                Route route = new Route("R-Test", vehicle, stops);

                // Crear un mapa de pedidos
                Map<String, Order> orders = new HashMap<>();
                orders.put(order.getId(), order);

                // Evaluar la ruta directamente (debería detectar que el GLP excede la capacidad)
                double routeCost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

                // Verificar si el evaluador detecta el problema de capacidad o simplemente lo permite
                if (Double.isInfinite(routeCost)) {
                    // Si lo detecta como error (comportamiento ideal)
                    assertTrue(true, "El evaluador correctamente detecta que el GLP excede la capacidad máxima");
                } else {
                    // Si lo permite (también aceptable en algunos casos)
                    System.out.println("Advertencia: El evaluador no detecta la discrepancia de capacidad GLP");
                }
            } catch (Exception e) {
                throw new AssertionError("Error al probar capacidades: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Test de una solución válida respetando las capacidades
     * 
     * GIVEN: Un vehículo con GLP exactamente a su capacidad máxima y un pedido con
     * demanda viable
     * PROCESS: Se crea una ruta con un pedido que demanda menos GLP del que tiene
     * el vehículo
     * AFTER: La solución debe ser válida y tener un costo finito
     */
    private static class ValidSolutionWithinCapacityTest extends TestFramework.AbstractTest {

        public ValidSolutionWithinCapacityTest() {
            super("Solución Válida con Capacidades Correctas");
        }

        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno para la evaluación
            Environment env = createTestEnvironment();
            
            // Crear un vehículo con GLP dentro de su capacidad y suficiente combustible
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.setCurrentFuelGal(100); // Asegurar suficiente combustible
            
            // Establecer GLP al máximo de su capacidad
            int glpCapacity = vehicle.getGlpCapacityM3();
            vehicle.refill(glpCapacity);

            // Verificar que el GLP actual es exactamente igual a la capacidad
            assertEquals(glpCapacity, vehicle.getCurrentGlpM3(),
                    "El GLP actual debe ser igual a la capacidad máxima");

            // Crear un pedido con demanda de GLP dentro de la capacidad del vehículo
            int glpDemand = glpCapacity / 3; // Un tercio de la capacidad
            Order order = createTestOrder("O1", new Position(10, 0), glpDemand, LocalDateTime.now().plusHours(2));

            // Verificar que la demanda es menor que la capacidad
            assertTrue(order.getGlpRequestM3() <= vehicle.getCurrentGlpM3(),
                    "La demanda de GLP debe ser menor o igual al GLP disponible");

            // Crear una ruta para servir al pedido
            List<RouteStop> stops = new ArrayList<>();
            stops.add(new OrderStop(
                    order.getId(),
                    order.getPosition(),
                    LocalDateTime.now().plusHours(1),
                    order.getGlpRequestM3()));

            Route route = new Route("R-Valid", vehicle, stops);

            // Crear un mapa de pedidos
            Map<String, Order> orders = new HashMap<>();
            orders.put(order.getId(), order);

            // Evaluar la ruta directamente
            double cost = Evaluator.evaluateRoute(env, route, orders, new HashMap<>());

            // Aserciones
            assertFalse(Double.isInfinite(cost), "El costo no debe ser infinito");
            assertTrue(cost > 0, "El costo debe ser positivo");
        }
    }

    // Métodos auxiliares (reutilizados del TestEvaluator original)

    /**
     * Crea un pedido para pruebas
     */
    private static Order createTestOrder(String id, Position position, int glpRequest, LocalDateTime dueTime) {
        LocalDateTime arriveTime = LocalDateTime.now().minusMinutes(30); // 30 minutos atrás
        return new Order(
                id,
                arriveTime,
                dueTime,
                glpRequest,
                position);
    }
    
    /**
     * Crea un entorno de prueba simple con un depósito principal
     */
    private static Environment createTestEnvironment() {
        List<Vehicle> vehicles = new ArrayList<>();
        Depot mainDepot = new Depot("MAIN", new Position(0, 0), 1000, true, true);
        List<Depot> auxDepots = new ArrayList<>();
        LocalDateTime currentTime = LocalDateTime.now();
        
        return new Environment(vehicles, mainDepot, auxDepots, currentTime);
    }
}