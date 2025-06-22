package com.vroute.test;

import com.vroute.models.*;
import com.vroute.solution.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.vroute.test.TestFramework.Assertions.*;

/**
 * Suite de pruebas para el SIHSolver y su integración con el Evaluator
 */
public class SIHSolverTestSuite {

    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("SIHSolver");

        // Añadir todos los tests
        suite.addTest(new BasicSIHSolutionTest());
        suite.addTest(new EmptyProblemTest());
        suite.addTest(new NoFeasibleSolutionTest());
        suite.addTest(new PartialDeliveryTest());
        suite.addTest(new MultiVehicleTest());
        suite.addTest(new DepotRefuelingTest());
        suite.addTest(new TimeWindowConstraintTest());
        suite.addTest(new GenerateInitialSolutionTest());

        return suite;
    }

    /**
     * Test básico del SIHSolver
     * 
     * GIVEN: Un entorno con pedidos y vehículos simples
     * PROCESS: Se ejecuta el solver SIH
     * AFTER: Debe generar una solución válida según el Evaluator
     */
    private static class BasicSIHSolutionTest extends TestFramework.AbstractTest {
        
        public BasicSIHSolutionTest() {
            super();
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno simple
            Environment environment = createSimpleEnvironment();
            
            // Crear el solver
            SIHSolver solver = new SIHSolver();
            
            // Resolver el problema
            Solution solution = solver.solve(environment);
            
            // Verificar que la solución no sea nula
            assertNotNull(solution, "La solución no debe ser nula");
            
            // Verificar que la solución sea válida
            boolean isValid = Evaluator.isSolutionValid(solution);
            assertTrue(isValid, "La solución generada por SIH debe ser válida según el Evaluator");
            
            // Verificar que el costo sea finito
            double cost = solution.getCost();
            assertFalse(Double.isInfinite(cost), "El costo de la solución no debe ser infinito");
            assertTrue(cost >= 0, "El costo debe ser no negativo");
        }
    }
    
    /**
     * Test de SIHSolver con un problema vacío
     * 
     * GIVEN: Un entorno sin pedidos
     * PROCESS: Se ejecuta el solver SIH
     * AFTER: Debe generar una solución vacía válida
     */
    private static class EmptyProblemTest extends TestFramework.AbstractTest {
        
        public EmptyProblemTest() {
            super("Problema Vacío");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno sin pedidos
            List<Vehicle> vehicles = new ArrayList<>();
            List<Depot> auxDepots = new ArrayList<>();
            
            // Agregar un vehículo
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicles.add(vehicle);
            
            // Agregar un depósito principal
            Depot mainDepot = new Depot("D1", new Position(0, 0), 100, true, true);
            
            // Crear el entorno
            Environment environment = new Environment(vehicles, mainDepot, auxDepots, LocalDateTime.now());
            
            // Crear el solver
            SIHSolver solver = new SIHSolver();
            
            // Resolver el problema
            Solution solution = solver.solve(environment);
            
            // Verificar que la solución no sea nula
            assertNotNull(solution, "La solución no debe ser nula incluso para un problema vacío");
            
            // Verificar que no hay rutas
            assertTrue(solution.getRoutes().isEmpty(), "No debe haber rutas en la solución para un problema sin pedidos");
            
            // Verificar que el costo sea 0
            assertEquals(0.0, solution.getCost(), "El costo debe ser 0 para un problema sin pedidos");
        }
    }
    
    /**
     * Test de un problema sin solución factible
     * 
     * GIVEN: Un vehículo sin combustible y un pedido
     * PROCESS: Se crea una solución con SIHSolver
     * AFTER: La solución debe ser vacía (sin rutas)
     */
    private static class NoFeasibleSolutionTest extends TestFramework.AbstractTest {
        
        public NoFeasibleSolutionTest() {
            super("Sin Solución Factible");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear un vehículo sin combustible
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.setCurrentFuelGal(0.0); // Sin combustible
            
            // Crear un pedido lejos
            Order order = new Order(
                "O1", 
                LocalDateTime.now(), 
                LocalDateTime.now().plusHours(1), 
                5, 
                new Position(50, 0)
            );
            
            // Crear el entorno
            List<Vehicle> vehicles = Collections.singletonList(vehicle);
            Depot mainDepot = new Depot("D1", new Position(0, 0), 100, true, true);
            List<Depot> depots = Collections.emptyList();
            Environment env = new Environment(vehicles, mainDepot, depots, LocalDateTime.now());
            env.addOrder(order);
            
            // Crear el solver y resolver
            SIHSolver solver = new SIHSolver();
            Solution solution = solver.solve(env);
            
            // Verificar que no hay rutas (ya que no hay solución factible)
            assertTrue(solution.getRoutes().isEmpty(), 
                "No debe haber rutas en la solución cuando no hay solución factible");
        }
    }
    
    /**
     * Test de entregas parciales
     * 
     * GIVEN: Un vehículo con GLP limitado y un pedido que requiere más GLP
     * PROCESS: Se crea una solución con SIHSolver
     * AFTER: La solución debe incluir una entrega parcial
     */
    private static class PartialDeliveryTest extends TestFramework.AbstractTest {
        
        public PartialDeliveryTest() {
            super("Entregas Parciales");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Guardar configuración del evaluador
            boolean prevAllowPartial = Evaluator.isAllowPartialGlpDelivery();
            Evaluator.setAllowPartialGlpDelivery(true);
            
            try {
                // Crear un vehículo con GLP limitado
                Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
                vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal()); // Combustible completo
                vehicle.setCurrentGlpM3(15); // GLP completo
                
                // Crear un pedido que requiere más GLP del que tiene el vehículo
                Order order = new Order(
                    "O1", 
                    LocalDateTime.now(), 
                    LocalDateTime.now().plusHours(1), 
                    20, 
                    new Position(10, 0)
                );
                
                // Crear el entorno
                List<Vehicle> vehicles = Collections.singletonList(vehicle);
                Depot mainDepot = new Depot("D1", new Position(0, 0), 100, true, true);
                List<Depot> depots = Collections.emptyList();
                Environment env = new Environment(vehicles, mainDepot, depots, LocalDateTime.now());
                env.addOrder(order);
                
                // Crear el solver y resolver
                SIHSolver solver = new SIHSolver();
                Solution solution = solver.solve(env);
                
                // Verificar que hay al menos una ruta
                assertFalse(solution.getRoutes().isEmpty(),
                    "La solución debe tener al menos una ruta");
                
                // Verificar la entrega parcial
                Route route = solution.getRoutes().get(0);
                boolean foundPartialDelivery = false;
                double deliveryRatio = 0.0;
                
                for (RouteStop stop : route.getStops()) {
                    if (stop instanceof OrderStop) {
                        OrderStop orderStop = (OrderStop) stop;
                        if (orderStop.getEntityID().equals("O1")) {
                            int delivered = orderStop.getGlpDelivery();
                            int required = order.getRemainingGlpM3();
                            deliveryRatio = (double) delivered / required;
                            foundPartialDelivery = delivered > 0 && delivered < required;
                            break;
                        }
                    }
                }
                
                assertTrue(foundPartialDelivery,
                    "Se debe encontrar una entrega para el pedido O1");
                assertTrue(deliveryRatio > 0.0 && deliveryRatio < 1.0,
                    "La tasa de satisfacción de GLP debe ser mayor que 0 pero menor que 1.0");
            } finally {
                // Restaurar configuración del evaluador
                Evaluator.setAllowPartialGlpDelivery(prevAllowPartial);
            }
        }
    }
    
    /**
     * Test de SIHSolver con múltiples vehículos
     * 
     * GIVEN: Un entorno con múltiples pedidos y vehículos
     * PROCESS: Se ejecuta el solver SIH
     * AFTER: Debe generar una solución que utilice múltiples vehículos de manera eficiente
     */
    private static class MultiVehicleTest extends TestFramework.AbstractTest {
        
        public MultiVehicleTest() {
            super("Múltiples Vehículos");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno con múltiples vehículos y pedidos
            List<Vehicle> vehicles = new ArrayList<>();
            List<Depot> auxDepots = new ArrayList<>();
            
            // Agregar varios vehículos
            for (int i = 1; i <= 3; i++) {
                Vehicle vehicle = new Vehicle("V" + i, VehicleType.TB, new Position(i * 5, 0));
                vehicle.refill(vehicle.getGlpCapacityM3());
                vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal());
                vehicles.add(vehicle);
            }
            
            // Agregar un depósito principal
            Depot mainDepot = new Depot("D1", new Position(0, 0), 1000, true, true);
            
            // Crear el entorno
            Environment environment = new Environment(vehicles, mainDepot, auxDepots, LocalDateTime.now());
            
            // Agregar varios pedidos en diferentes ubicaciones
            for (int i = 1; i <= 10; i++) {
                Position position = new Position(i * 10, i * 5);
                Order order = createTestOrder("O" + i, position, 5, LocalDateTime.now().plusHours(3));
                environment.addOrder(order);
            }
            
            // Crear el solver
            SIHSolver solver = new SIHSolver();
            
            // Resolver el problema
            Solution solution = solver.solve(environment);
            
            // Verificar que la solución no sea nula
            assertNotNull(solution, "La solución no debe ser nula");
            
            // Verificar que hay rutas
            assertFalse(solution.getRoutes().isEmpty(), "Debe haber rutas en la solución");
            
            // Verificar que se utilizan múltiples vehículos
            int vehiclesUsed = solution.getRoutes().size();
            assertTrue(vehiclesUsed > 1, "Se deben utilizar múltiples vehículos para atender todos los pedidos");
            
            // Verificar que la solución sea válida
            boolean isValid = Evaluator.isSolutionValid(solution);
            assertTrue(isValid, "La solución debe ser válida");
            
            // Verificar que se atienden todos los pedidos
            double orderFulfillmentRate = Evaluator.calculateOrderFulfillmentRate(solution);
            assertTrue(orderFulfillmentRate > 0.5, "Al menos el 50% de los pedidos deben ser atendidos");
        }
    }
    
    /**
     * Test de SIHSolver con reabastecimiento en depósitos
     * 
     * GIVEN: Un entorno con pedidos que requieren reabastecimiento
     * PROCESS: Se ejecuta el solver SIH
     * AFTER: Debe generar una solución que incluya paradas en depósitos para reabastecimiento
     */
    private static class DepotRefuelingTest extends TestFramework.AbstractTest {
        
        public DepotRefuelingTest() {
            super("Reabastecimiento en Depósitos");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno que requiera reabastecimiento
            List<Vehicle> vehicles = new ArrayList<>();
            List<Depot> auxDepots = new ArrayList<>();
            
            // Agregar un vehículo con GLP limitado
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            int initialGlp = vehicle.getGlpCapacityM3() / 2; // Mitad de la capacidad
            vehicle.refill(initialGlp);
            vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal());
            vehicles.add(vehicle);
            
            // Agregar depósitos
            Depot mainDepot = new Depot("D1", new Position(0, 0), 1000, true, true);
            
            // Depósito auxiliar en medio de la ruta
            Depot auxDepot = new Depot("D2", new Position(50, 50), 1000, true, true);
            auxDepots.add(auxDepot);
            
            // Crear el entorno
            Environment environment = new Environment(vehicles, mainDepot, auxDepots, LocalDateTime.now());
            
            // Agregar pedidos que en total exceden la capacidad inicial
            int totalGlpRequest = 0;
            for (int i = 1; i <= 5; i++) {
                Position position = new Position(i * 20, i * 10);
                int glpRequest = 5;
                totalGlpRequest += glpRequest;
                Order order = createTestOrder("O" + i, position, glpRequest, LocalDateTime.now().plusHours(5));
                environment.addOrder(order);
            }
            
            // Asegurar que los pedidos requieren más GLP del inicial
            assertTrue(totalGlpRequest > initialGlp, "El total de GLP solicitado debe exceder el GLP inicial");
            
            // Crear el solver
            SIHSolver solver = new SIHSolver();
            
            // Resolver el problema
            Solution solution = solver.solve(environment);
            
            // Verificar que la solución no sea nula
            assertNotNull(solution, "La solución no debe ser nula");
            
            // Verificar que hay rutas
            assertFalse(solution.getRoutes().isEmpty(), "Debe haber rutas en la solución");
            
            // Verificar que la solución sea válida
            boolean isValid = Evaluator.isSolutionValid(solution);
            assertTrue(isValid, "La solución debe ser válida");
            
            // Buscar paradas en depósitos
            boolean hasDepotStop = false;
            for (Route route : solution.getRoutes()) {
                for (RouteStop stop : route.getStops()) {
                    if (stop instanceof DepotStop) {
                        hasDepotStop = true;
                        break;
                    }
                }
            }
            
            // Verificar que hay al menos una parada en depósito
            assertTrue(hasDepotStop, "La solución debe incluir al menos una parada en depósito para reabastecimiento");
        }
    }
    
    /**
     * Test de SIHSolver con restricciones de ventanas de tiempo
     * 
     * GIVEN: Un entorno con pedidos con ventanas de tiempo estrictas
     * PROCESS: Se ejecuta el solver SIH
     * AFTER: Debe generar una solución que respete las ventanas de tiempo
     */
    private static class TimeWindowConstraintTest extends TestFramework.AbstractTest {
        
        public TimeWindowConstraintTest() {
            super("Restricciones de Ventanas de Tiempo");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear un entorno con ventanas de tiempo estrictas
            List<Vehicle> vehicles = new ArrayList<>();
            List<Depot> auxDepots = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            // Agregar un vehículo
            Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
            vehicle.refill(vehicle.getGlpCapacityM3());
            vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal());
            vehicles.add(vehicle);
            
            // Agregar un depósito principal
            Depot mainDepot = new Depot("D1", new Position(0, 0), 100, true, true);
            
            // Crear el entorno
            Environment environment = new Environment(vehicles, mainDepot, auxDepots, now);
            
            // Agregar pedidos con ventanas de tiempo específicas
            // Pedido 1: Ventana de tiempo cercana
            Order order1 = new Order("O1", now, now.plusHours(1), 5, new Position(10, 0));
            environment.addOrder(order1);
            
            // Pedido 2: Ventana de tiempo intermedia
            Order order2 = new Order("O2", now.plusHours(2), now.plusHours(3), 5, new Position(20, 0));
            environment.addOrder(order2);
            
            // Pedido 3: Ventana de tiempo lejana
            Order order3 = new Order("O3", now.plusHours(4), now.plusHours(5), 5, new Position(30, 0));
            environment.addOrder(order3);
            
            // Crear el solver
            SIHSolver solver = new SIHSolver();
            
            // Resolver el problema
            Solution solution = solver.solve(environment);
            
            // Verificar que la solución no sea nula
            assertNotNull(solution, "La solución no debe ser nula");
            
            // Verificar que hay rutas
            assertFalse(solution.getRoutes().isEmpty(), "Debe haber rutas en la solución");
            
            // Verificar que la solución sea válida
            boolean isValid = Evaluator.isSolutionValid(solution);
            assertTrue(isValid, "La solución debe ser válida");
            
            // Verificar que el costo no incluye penalizaciones por entregas tardías
            List<Evaluator.CostComponent> costComponents = solution.getDetailedCostBreakdown();
            boolean hasLateDeliveryPenalty = false;
            
            for (Evaluator.CostComponent component : costComponents) {
                if (component.getDescription().toLowerCase().contains("tard") || 
                    component.getDescription().toLowerCase().contains("late")) {
                    hasLateDeliveryPenalty = component.getCost() > 0;
                }
            }
            
            assertFalse(hasLateDeliveryPenalty, "La solución no debe tener penalizaciones por entregas tardías");
        }
    }
    
    /**
     * Test del método estático generateInitialSolution
     * 
     * GIVEN: Un conjunto de pedidos, vehículos y depósitos
     * PROCESS: Se genera una solución inicial usando el método estático
     * AFTER: Debe generar una solución válida según el Evaluator
     */
    private static class GenerateInitialSolutionTest extends TestFramework.AbstractTest {
        
        public GenerateInitialSolutionTest() {
            super("Generación de Solución Inicial");
        }
        
        @Override
        protected void runTest() throws Throwable {
            // Crear pedidos
            Map<String, Order> orders = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                Position position = new Position(i * 10, i * 5);
                Order order = createTestOrder("O" + i, position, 5, LocalDateTime.now().plusHours(3));
                orders.put(order.getId(), order);
            }
            
            // Crear vehículos
            List<Vehicle> vehicles = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                Vehicle vehicle = new Vehicle("V" + i, VehicleType.TB, new Position(i * 5, 0));
                vehicle.refill(vehicle.getGlpCapacityM3());
                vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal());
                vehicles.add(vehicle);
            }
            
            // Crear depósitos
            List<Depot> depots = new ArrayList<>();
            Depot depot = new Depot("D1", new Position(0, 0), 100, true, true);
            depots.add(depot);
            
            // Generar solución inicial
            LocalDateTime startTime = LocalDateTime.now();
            Random random = new Random(42); // Semilla fija para reproducibilidad
            
            Solution solution = SIHSolver.generateInitialSolution(orders, vehicles, depots, startTime, random);
            
            // Verificar que la solución no sea nula
            assertNotNull(solution, "La solución inicial no debe ser nula");
            
            // Verificar que hay rutas
            assertFalse(solution.getRoutes().isEmpty(), "Debe haber rutas en la solución inicial");
            
            // Verificar que el costo es finito
            double cost = solution.getCost();
            assertFalse(Double.isInfinite(cost), "El costo de la solución inicial no debe ser infinito");
            
            // Verificar que se atienden algunos pedidos
            double orderFulfillmentRate = Evaluator.calculateOrderFulfillmentRate(solution);
            assertTrue(orderFulfillmentRate > 0, "La solución inicial debe atender al menos algunos pedidos");
        }
    }
    
    // Métodos auxiliares
    
    /**
     * Crea un entorno simple para pruebas
     */
    private static Environment createSimpleEnvironment() {
        // Crear un nuevo entorno
        List<Vehicle> vehicles = new ArrayList<>();
        List<Depot> auxDepots = new ArrayList<>();
        
        // Agregar un vehículo
        Vehicle vehicle = new Vehicle("V1", VehicleType.TB, new Position(0, 0));
        vehicle.refill(vehicle.getGlpCapacityM3());
        vehicle.setCurrentFuelGal(vehicle.getFuelCapacityGal());
        vehicles.add(vehicle);
        
        // Agregar un depósito principal
        Depot mainDepot = new Depot("D1", new Position(0, 0), 100, true, true);
        
        // Crear y configurar el entorno
        Environment environment = new Environment(vehicles, mainDepot, auxDepots, LocalDateTime.now());
        
        // Agregar un pedido
        Order order = createTestOrder("O1", new Position(10, 0), 10, LocalDateTime.now().plusHours(2));
        environment.addOrder(order);
        
        return environment;
    }
    
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
} 