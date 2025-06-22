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
            // Crear un vehículo con suficiente combustible y GLP
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.refill(vehicle.getGlpCapacityM3());
            vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal());

            // Crear un pedido
            Order order = createTestOrder("O1", new Position(10, 0), 20, LocalDateTime.now().plusHours(2));

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

            // Crear una solución con esta ruta
            List<Route> routes = new ArrayList<>();
            routes.add(route);
            Solution solution = new Solution(orders, routes);

            // Evaluar la solución
            double cost = Evaluator.evaluateSolution(solution);

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

            // Crear una solución con esta ruta
            List<Route> routes = new ArrayList<>();
            routes.add(route);
            Solution solution = new Solution(orders, routes);

            // Evaluar la solución
            double cost = Evaluator.evaluateSolution(solution);

            // Aserciones
            assertTrue(Double.isInfinite(cost), "El costo debe ser infinito por falta de combustible");
            assertEquals(Double.POSITIVE_INFINITY, cost, "El costo debe ser específicamente POSITIVE_INFINITY");
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
            // Configurar para requerir GLP completo (no parcial)
            boolean prevAllowPartial = Evaluator.isAllowPartialGlpDelivery();
            Evaluator.setAllowPartialGlpDelivery(false);

            try {
                // Crear un vehículo con poco GLP
                Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));

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

                // Crear una solución con esta ruta
                List<Route> routes = new ArrayList<>();
                routes.add(route);
                Solution solution = new Solution(orders, routes);

                // Evaluar la solución
                double cost = Evaluator.evaluateSolution(solution);

                // Aserciones
                assertTrue(Double.isInfinite(cost), "El costo debe ser infinito por falta de GLP");
                assertEquals(Double.POSITIVE_INFINITY, cost, "El costo debe ser específicamente POSITIVE_INFINITY");
            } finally {
                // Restaurar el valor original
                Evaluator.setAllowPartialGlpDelivery(prevAllowPartial);
            }
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
            // Configurar para no permitir exceder capacidad de GLP
            boolean prevEnforceCapacity = Evaluator.isEnforceGlpCapacityLimit();
            Evaluator.setEnforceGlpCapacityLimit(false);

            try {
                // Crear un vehículo con capacidad limitada de GLP
                Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
                vehicle.refill(vehicle.getGlpCapacityM3());

                // Crear un pedido
                Order order = createTestOrder("O1", new Position(10, 0), 20, LocalDateTime.now().plusHours(2));

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

                // Crear una solución con esta ruta
                List<Route> routes = new ArrayList<>();
                routes.add(route);
                Solution solution = new Solution(orders, routes);

                // Evaluar la solución
                double cost = Evaluator.evaluateSolution(solution);

                // Aserciones
                assertTrue(Double.isInfinite(cost), "El costo debe ser infinito por exceder capacidad de GLP");
                assertEquals(Double.POSITIVE_INFINITY, cost, "El costo debe ser específicamente POSITIVE_INFINITY");
            } finally {
                // Restaurar el valor original
                Evaluator.setEnforceGlpCapacityLimit(prevEnforceCapacity);
            }
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
            // Asegurar que se permiten entregas parciales para este test
            boolean prevAllowPartial = Evaluator.isAllowPartialGlpDelivery();
            Evaluator.setAllowPartialGlpDelivery(true);

            try {
                // Crear un vehículo
                Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
                vehicle.refill(vehicle.getGlpCapacityM3());

                // Crear un pedido con fecha límite próxima
                Order order = createTestOrder("O1", new Position(10, 0), 20, LocalDateTime.now().plusMinutes(30));

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

                // Crear una solución con esta ruta
                List<Route> routes = new ArrayList<>();
                routes.add(route);
                Solution solution = new Solution(orders, routes);

                // Evaluar la solución
                double cost = Evaluator.evaluateSolution(solution);

                // Aserciones
                assertFalse(Double.isInfinite(cost), "El costo no debe ser infinito aunque la entrega sea tardía");
                assertTrue(cost > 0, "El costo debe ser positivo");

                // Comprobar que hay componentes de costo por tardanza
                List<Evaluator.CostComponent> costComponents = Evaluator.getDetailedCostBreakdown(solution);

                // Imprimir todos los componentes para diagnóstico
                System.out.println("Componentes de costo encontrados:");
                for (Evaluator.CostComponent comp : costComponents) {
                    System.out.println("- " + comp.getDescription() + ": " + comp.getCost());
                }

                boolean hasTardinessCost = false;
                for (Evaluator.CostComponent comp : costComponents) {
                    if (comp.getCost() > 0 && comp.getDescription().toLowerCase().contains("tard") ||
                            comp.getDescription().toLowerCase().contains("late")) {
                        hasTardinessCost = true;
                        break;
                    }
                }

                assertTrue(hasTardinessCost, "Debe existir un componente de costo por tardanza");
            } finally {
                // Restaurar el valor original
                Evaluator.setAllowPartialGlpDelivery(prevAllowPartial);
            }
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
            Solution complexSolution = createComplexSolution();

            // Evaluar la solución
            double cost = Evaluator.evaluateSolution(complexSolution);

            if (Double.isInfinite(cost)) {
                // Si la solución es inválida, identificamos cuál ruta es problemática
                boolean foundValidRoute = false;

                for (int i = 0; i < complexSolution.getRoutes().size(); i++) {
                    Route route = complexSolution.getRoutes().get(i);
                    double routeCost = Evaluator.evaluateRoute(route, complexSolution.getOrders());

                    if (!Double.isInfinite(routeCost)) {
                        foundValidRoute = true;
                    }
                }

                assertTrue(foundValidRoute, "Al menos una ruta debe ser válida en la solución compleja");
                // Nota: Esto podría fallar si todas las rutas son inválidas, pero eso sería un
                // problema de la solución de prueba
            } else {
                // Si la solución es válida, simplemente verificamos que el costo sea positivo
                assertTrue(cost > 0, "El costo debe ser positivo");
            }
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
            // Activar validación de capacidad máxima
            boolean prevValidate = Evaluator.isValidateGlpNotExceedsCapacity();
            Evaluator.setValidateGlpNotExceedsCapacity(true);

            try {
                // Crear un vehículo tipo TB que tiene capacidad 15 según el enum
                Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));

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

                    // Evaluar la ruta (debería detectar que el GLP excede la capacidad)
                    double routeCost = Evaluator.evaluateRoute(route, orders);

                    // Verificar si el evaluador detecta el problema de capacidad
                    assertTrue(Double.isInfinite(routeCost),
                            "El evaluador debería detectar que el GLP excede la capacidad máxima");

                } catch (Exception e) {
                    throw new AssertionError("Error al probar capacidades: " + e.getMessage(), e);
                }
            } finally {
                // Restaurar el valor original
                Evaluator.setValidateGlpNotExceedsCapacity(prevValidate);
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
            // Crear un vehículo con GLP dentro de su capacidad
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
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

            // Crear una solución con esta ruta
            List<Route> routes = new ArrayList<>();
            routes.add(route);
            Solution solution = new Solution(orders, routes);

            // Evaluar la solución
            double cost = Evaluator.evaluateSolution(solution);

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
     * Crea una solucion compleja para analizar problemas específicos del algoritmo
     * ALNS o Tabu
     */
    private static Solution createComplexSolution() {
        // Crear vehículos
        Vehicle vehicle1 = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
        vehicle1.refill(vehicle1.getGlpCapacityM3());
        Vehicle vehicle2 = new Vehicle("V2", VehicleType.TB, new Position(5, 5));
        vehicle2.refill(vehicle2.getGlpCapacityM3());

        // Crear pedidos
        Map<String, Order> orders = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            Position pos = new Position(i * 10, i * 5);
            int glp = 10 + i;
            Order order = createTestOrder("O" + i, pos, glp, LocalDateTime.now().plusHours(2));
            orders.put(order.getId(), order);
        }

        // Crear depósito auxiliar
        Depot auxDepot = new Depot("DEPOT1", new Position(50, 50), 200, true, false);

        // Ruta 1: V1 sirve a O1, O3, O5, va al depósito y luego sirve O7, O9
        List<RouteStop> stops1 = new ArrayList<>();
        stops1.add(new OrderStop("O1", orders.get("O1").getPosition(), LocalDateTime.now().plusMinutes(30),
                orders.get("O1").getGlpRequestM3()));
        stops1.add(new OrderStop("O3", orders.get("O3").getPosition(), LocalDateTime.now().plusMinutes(60),
                orders.get("O3").getGlpRequestM3()));
        stops1.add(new OrderStop("O5", orders.get("O5").getPosition(), LocalDateTime.now().plusMinutes(90),
                orders.get("O5").getGlpRequestM3()));
        stops1.add(new DepotStop(auxDepot, LocalDateTime.now().plusMinutes(120), 50));
        stops1.add(new OrderStop("O7", orders.get("O7").getPosition(), LocalDateTime.now().plusMinutes(150),
                orders.get("O7").getGlpRequestM3()));
        stops1.add(new OrderStop("O9", orders.get("O9").getPosition(), LocalDateTime.now().plusMinutes(180),
                orders.get("O9").getGlpRequestM3()));
        Route route1 = new Route("R1", vehicle1, stops1);

        // Ruta 2: V2 sirve a O2, O4, O6, O8, O10
        List<RouteStop> stops2 = new ArrayList<>();
        stops2.add(new OrderStop("O2", orders.get("O2").getPosition(), LocalDateTime.now().plusMinutes(30),
                orders.get("O2").getGlpRequestM3()));
        stops2.add(new OrderStop("O4", orders.get("O4").getPosition(), LocalDateTime.now().plusMinutes(60),
                orders.get("O4").getGlpRequestM3()));
        stops2.add(new OrderStop("O6", orders.get("O6").getPosition(), LocalDateTime.now().plusMinutes(90),
                orders.get("O6").getGlpRequestM3()));
        stops2.add(new OrderStop("O8", orders.get("O8").getPosition(), LocalDateTime.now().plusMinutes(120),
                orders.get("O8").getGlpRequestM3()));
        stops2.add(new OrderStop("O10", orders.get("O10").getPosition(), LocalDateTime.now().plusMinutes(150),
                orders.get("O10").getGlpRequestM3()));
        Route route2 = new Route("R2", vehicle2, stops2);

        List<Route> routes = new ArrayList<>();
        routes.add(route1);
        routes.add(route2);

        return new Solution(orders, routes);
    }
}