package com.vroute.test;

import com.vroute.models.*;
import com.vroute.solution.*;
import com.vroute.taboo.*;

import java.time.LocalDateTime;
import java.util.*;

public class TabuSearchTestSuite {

    /**
     * Crea la suite de pruebas para las operaciones de búsqueda tabú
     * @return Suite de pruebas
     */
    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("Tabu Search Operations Tests");
        
        suite.addTest(suite.createTest("testInterRouteRelocateMove", TabuSearchTestSuite::testInterRouteRelocateMove));
        suite.addTest(suite.createTest("testInterRouteSwapMove", TabuSearchTestSuite::testInterRouteSwapMove));
        suite.addTest(suite.createTest("testInterRouteRelocateMoveInvalidCapacity", TabuSearchTestSuite::testInterRouteRelocateMoveInvalidCapacity));
        
        return suite;
    }
    
    /**
     * Prueba para verificar que InterRouteRelocateMove mueve correctamente un pedido entre rutas
     */
    private static TestFramework.TestResult testInterRouteRelocateMove() {
        try {
            // Crear entorno de prueba
            TestEnvironment env = createTestEnvironment();
            
            // Crear una solución con dos rutas
            Solution solution = createTwoRouteSolution(env);
            
            // Verificar el número original de stops en cada ruta
            int originalRoute1Stops = solution.getRoutes().get(0).getStops().size();
            int originalRoute2Stops = solution.getRoutes().get(1).getStops().size();
            
            // Crear un movimiento para mover el primer OrderStop de la ruta 0 a la posición 1 de la ruta 1
            int fromRouteIndex = 0;
            int fromStopIndex = 1; // Primer OrderStop (después del DepotStop inicial)
            int toRouteIndex = 1;
            int toStopIndex = 1; // Después del DepotStop inicial de la ruta 2
            
            InterRouteRelocateMove move = new InterRouteRelocateMove(
                fromRouteIndex, fromStopIndex, toRouteIndex, toStopIndex);
            
            // Guardar el ID del orden que se moverá para verificar después
            String orderIdToMove = ((OrderStop)solution.getRoutes().get(0).getStops().get(fromStopIndex)).getEntityID();
            
            // Aplicar el movimiento
            boolean result = move.apply(solution);
            
            // Verificar que el movimiento fue exitoso
            TestFramework.Assertions.assertTrue(result, "El movimiento debería ser exitoso");
            
            // Verificar que el número de paradas ha cambiado correctamente
            TestFramework.Assertions.assertEquals(originalRoute1Stops - 1, 
                    solution.getRoutes().get(0).getStops().size(), 
                    "La ruta origen debería tener una parada menos");
                    
            TestFramework.Assertions.assertEquals(originalRoute2Stops + 1, 
                    solution.getRoutes().get(1).getStops().size(), 
                    "La ruta destino debería tener una parada más");
            
            // Verificar que el pedido está ahora en la ruta destino
            boolean foundInDestination = false;
            for (RouteStop stop : solution.getRoutes().get(1).getStops()) {
                if (stop instanceof OrderStop && ((OrderStop) stop).getEntityID().equals(orderIdToMove)) {
                    foundInDestination = true;
                    break;
                }
            }
            TestFramework.Assertions.assertTrue(foundInDestination, 
                "El pedido debería estar en la ruta destino");
            
            // Verificar que el orden ya no está en la ruta origen
            boolean foundInSource = false;
            for (RouteStop stop : solution.getRoutes().get(0).getStops()) {
                if (stop instanceof OrderStop && ((OrderStop) stop).getEntityID().equals(orderIdToMove)) {
                    foundInSource = true;
                    break;
                }
            }
            TestFramework.Assertions.assertFalse(foundInSource, 
                "El pedido no debería estar en la ruta origen");
            
            return TestFramework.TestResult.success("testInterRouteRelocateMove", 0);
        } catch (AssertionError e) {
            return TestFramework.TestResult.failure("testInterRouteRelocateMove", e.getMessage(), 0, null, null);
        } catch (Throwable t) {
            return TestFramework.TestResult.error("testInterRouteRelocateMove", t, 0);
        }
    }
    
    /**
     * Prueba para verificar que InterRouteSwapMove intercambia correctamente pedidos entre rutas
     */
    private static TestFramework.TestResult testInterRouteSwapMove() {
        try {
            // Crear entorno de prueba
            TestEnvironment env = createTestEnvironment();
            
            // Crear una solución con dos rutas
            Solution solution = createTwoRouteSolution(env);
            
            // Crear un movimiento para intercambiar el primer OrderStop de cada ruta
            int firstRouteIndex = 0;
            int firstStopIndex = 1; // Primer OrderStop (después del DepotStop inicial)
            int secondRouteIndex = 1;
            int secondStopIndex = 1; // Primer OrderStop (después del DepotStop inicial)
            
            InterRouteSwapMove move = new InterRouteSwapMove(
                firstRouteIndex, firstStopIndex, secondRouteIndex, secondStopIndex);
            
            // Guardar los IDs de los órdenes que se intercambiarán
            String firstOrderId = ((OrderStop)solution.getRoutes().get(0).getStops().get(firstStopIndex)).getEntityID();
            String secondOrderId = ((OrderStop)solution.getRoutes().get(1).getStops().get(secondStopIndex)).getEntityID();
            
            // Aplicar el movimiento
            boolean result = move.apply(solution);
            
            // Verificar que el movimiento fue exitoso
            TestFramework.Assertions.assertTrue(result, "El movimiento debería ser exitoso");
            
            // Verificar que el primer orden ahora está en la segunda ruta
            boolean firstOrderInSecondRoute = false;
            for (RouteStop stop : solution.getRoutes().get(1).getStops()) {
                if (stop instanceof OrderStop && ((OrderStop) stop).getEntityID().equals(firstOrderId)) {
                    firstOrderInSecondRoute = true;
                    break;
                }
            }
            TestFramework.Assertions.assertTrue(firstOrderInSecondRoute, 
                "El primer pedido debería estar en la segunda ruta");
            
            // Verificar que el segundo orden ahora está en la primera ruta
            boolean secondOrderInFirstRoute = false;
            for (RouteStop stop : solution.getRoutes().get(0).getStops()) {
                if (stop instanceof OrderStop && ((OrderStop) stop).getEntityID().equals(secondOrderId)) {
                    secondOrderInFirstRoute = true;
                    break;
                }
            }
            TestFramework.Assertions.assertTrue(secondOrderInFirstRoute, 
                "El segundo pedido debería estar en la primera ruta");
            
            return TestFramework.TestResult.success("testInterRouteSwapMove", 0);
        } catch (AssertionError e) {
            return TestFramework.TestResult.failure("testInterRouteSwapMove", e.getMessage(), 0, null, null);
        } catch (Throwable t) {
            return TestFramework.TestResult.error("testInterRouteSwapMove", t, 0);
        }
    }
    
    /**
     * Prueba para verificar que InterRouteRelocateMove falla cuando la capacidad del vehículo destino
     * no es suficiente para el pedido
     */
    private static TestFramework.TestResult testInterRouteRelocateMoveInvalidCapacity() {
        try {
            // Crear entorno de prueba
            TestEnvironment env = createTestEnvironment();
            
            // Crear una solución con dos rutas, donde la segunda tiene un vehículo pequeño
            Solution solution = createSolutionWithDifferentVehicles(env, VehicleType.TA, VehicleType.TD);
            
            // Crear un movimiento para mover un pedido grande de la primera ruta a la segunda
            int fromRouteIndex = 0;
            int fromStopIndex = 1; // Primer OrderStop (después del DepotStop inicial)
            int toRouteIndex = 1;
            int toStopIndex = 1; // Después del DepotStop inicial
            
            // Asegurarse de que el pedido a mover es demasiado grande para el vehículo destino
            Route fromRoute = solution.getRoutes().get(fromRouteIndex);
            OrderStop orderStop = (OrderStop) fromRoute.getStops().get(fromStopIndex);
            int glpDelivery = 10; // Mayor que la capacidad de un vehículo TD (5 m3)
            
            // Reemplazar el OrderStop con uno que tenga una entrega de GLP más grande
            OrderStop newOrderStop = new OrderStop(
                orderStop.getEntityID(), 
                orderStop.getPosition(), 
                orderStop.getArrivalTime(), 
                glpDelivery);
            
            fromRoute.getStops().set(fromStopIndex, newOrderStop);
            
            // Crear y aplicar el movimiento
            InterRouteRelocateMove move = new InterRouteRelocateMove(
                fromRouteIndex, fromStopIndex, toRouteIndex, toStopIndex);
                
            boolean result = move.apply(solution);
            
            // Verificar que el movimiento falló debido a la restricción de capacidad
            TestFramework.Assertions.assertFalse(result, 
                "El movimiento debería fallar debido a la restricción de capacidad");
            
            // Verificar que el pedido sigue en la ruta original
            boolean orderInOriginalRoute = false;
            for (RouteStop stop : solution.getRoutes().get(fromRouteIndex).getStops()) {
                if (stop instanceof OrderStop && ((OrderStop) stop).getEntityID().equals(orderStop.getEntityID())) {
                    orderInOriginalRoute = true;
                    break;
                }
            }
            TestFramework.Assertions.assertTrue(orderInOriginalRoute, 
                "El pedido debería permanecer en la ruta original");
            
            return TestFramework.TestResult.success("testInterRouteRelocateMoveInvalidCapacity", 0);
        } catch (AssertionError e) {
            return TestFramework.TestResult.failure("testInterRouteRelocateMoveInvalidCapacity", e.getMessage(), 0, null, null);
        } catch (Throwable t) {
            return TestFramework.TestResult.error("testInterRouteRelocateMoveInvalidCapacity", t, 0);
        }
    }
    
    /**
     * Clase auxiliar para crear un entorno de prueba
     */
    private static class TestEnvironment {
        Environment environment;
        Map<String, Order> orders;
        List<Vehicle> vehicles;
        Depot mainDepot;
        
        public TestEnvironment(Environment environment, Map<String, Order> orders, 
                              List<Vehicle> vehicles, Depot mainDepot) {
            this.environment = environment;
            this.orders = orders;
            this.vehicles = vehicles;
            this.mainDepot = mainDepot;
        }
    }
    
    /**
     * Crea un entorno de prueba estándar con vehículos, depósitos y órdenes
     */
    private static TestEnvironment createTestEnvironment() {
        LocalDateTime now = LocalDateTime.now();
        
        // Crear depósito principal
        Depot mainDepot = new Depot("D1", new Position(50, 50), 1000, true, true);
        mainDepot.refillGLP();
        
        // Crear vehículos
        List<Vehicle> vehicles = new ArrayList<>();
        Vehicle v1 = new Vehicle("V1", VehicleType.TA, mainDepot.getPosition());
        v1.refill(v1.getGlpCapacityM3()); // Llenar el tanque de GLP
        Vehicle v2 = new Vehicle("V2", VehicleType.TB, mainDepot.getPosition());
        v2.refill(v2.getGlpCapacityM3()); // Llenar el tanque de GLP
        vehicles.add(v1);
        vehicles.add(v2);
        
        // Crear órdenes
        Map<String, Order> orders = new HashMap<>();
        Order o1 = new Order("O1", now, now.plusHours(5), 10, new Position(30, 30));
        Order o2 = new Order("O2", now, now.plusHours(5), 15, new Position(70, 30));
        Order o3 = new Order("O3", now, now.plusHours(5), 5, new Position(30, 70));
        Order o4 = new Order("O4", now, now.plusHours(5), 8, new Position(70, 70));
        
        orders.put(o1.getId(), o1);
        orders.put(o2.getId(), o2);
        orders.put(o3.getId(), o3);
        orders.put(o4.getId(), o4);
        
        // Crear entorno
        Environment environment = new Environment(vehicles, mainDepot, new ArrayList<>(), now);
        environment.addOrders(List.of(o1, o2, o3, o4));
        
        return new TestEnvironment(environment, orders, vehicles, mainDepot);
    }
    
    /**
     * Crea una solución con dos rutas, cada una con dos paradas de órdenes
     */
    private static Solution createTwoRouteSolution(TestEnvironment env) {
        List<Route> routes = new ArrayList<>();
        Map<String, Order> orders = new HashMap<>(env.orders);
        
        // Ruta 1: Depósito -> O1 -> O2 -> Depósito
        List<RouteStop> stops1 = new ArrayList<>();
        LocalDateTime time = env.environment.getCurrentTime();
        
        // Depósito inicial
        stops1.add(new DepotStop(env.mainDepot, time, 0));
        
        // Primer pedido
        Order o1 = orders.get("O1");
        time = time.plusMinutes(30); // Tiempo estimado de viaje
        stops1.add(new OrderStop(o1.getId(), o1.getPosition(), time, 10));
        
        // Segundo pedido
        Order o2 = orders.get("O2");
        time = time.plusMinutes(45); // Tiempo estimado de viaje
        stops1.add(new OrderStop(o2.getId(), o2.getPosition(), time, 15));
        
        // Depósito final
        time = time.plusMinutes(40); // Tiempo estimado de viaje de vuelta
        stops1.add(new DepotStop(env.mainDepot, time, 0));
        
        // Ruta 2: Depósito -> O3 -> O4 -> Depósito
        List<RouteStop> stops2 = new ArrayList<>();
        time = env.environment.getCurrentTime();
        
        // Depósito inicial
        stops2.add(new DepotStop(env.mainDepot, time, 0));
        
        // Tercer pedido
        Order o3 = orders.get("O3");
        time = time.plusMinutes(35); // Tiempo estimado de viaje
        stops2.add(new OrderStop(o3.getId(), o3.getPosition(), time, 5));
        
        // Cuarto pedido
        Order o4 = orders.get("O4");
        time = time.plusMinutes(50); // Tiempo estimado de viaje
        stops2.add(new OrderStop(o4.getId(), o4.getPosition(), time, 8));
        
        // Depósito final
        time = time.plusMinutes(45); // Tiempo estimado de viaje de vuelta
        stops2.add(new DepotStop(env.mainDepot, time, 0));
        
        // Crear rutas
        routes.add(new Route("R1", env.vehicles.get(0), stops1));
        routes.add(new Route("R2", env.vehicles.get(1), stops2));
        
        return new Solution(orders, routes);
    }
    
    /**
     * Crea una solución con dos rutas, usando vehículos de diferentes tipos
     */
    private static Solution createSolutionWithDifferentVehicles(TestEnvironment env, 
                                                              VehicleType type1, VehicleType type2) {
        // Crear vehículos con los tipos especificados
        Vehicle v1 = new Vehicle("V1-CUSTOM", type1, env.mainDepot.getPosition());
        v1.refill(v1.getGlpCapacityM3());
        Vehicle v2 = new Vehicle("V2-CUSTOM", type2, env.mainDepot.getPosition());
        v2.refill(v2.getGlpCapacityM3());
        
        List<Route> routes = new ArrayList<>();
        Map<String, Order> orders = new HashMap<>(env.orders);
        
        // Ruta 1: Depósito -> O1 -> O2 -> Depósito
        List<RouteStop> stops1 = new ArrayList<>();
        LocalDateTime time = env.environment.getCurrentTime();
        
        // Depósito inicial
        stops1.add(new DepotStop(env.mainDepot, time, 0));
        
        // Primer pedido
        Order o1 = orders.get("O1");
        time = time.plusMinutes(30); // Tiempo estimado de viaje
        stops1.add(new OrderStop(o1.getId(), o1.getPosition(), time, 10));
        
        // Segundo pedido
        Order o2 = orders.get("O2");
        time = time.plusMinutes(45); // Tiempo estimado de viaje
        stops1.add(new OrderStop(o2.getId(), o2.getPosition(), time, 15));
        
        // Depósito final
        time = time.plusMinutes(40); // Tiempo estimado de viaje de vuelta
        stops1.add(new DepotStop(env.mainDepot, time, 0));
        
        // Ruta 2: Depósito -> O3 -> O4 -> Depósito
        List<RouteStop> stops2 = new ArrayList<>();
        time = env.environment.getCurrentTime();
        
        // Depósito inicial
        stops2.add(new DepotStop(env.mainDepot, time, 0));
        
        // Tercer pedido
        Order o3 = orders.get("O3");
        time = time.plusMinutes(35); // Tiempo estimado de viaje
        stops2.add(new OrderStop(o3.getId(), o3.getPosition(), time, 5));
        
        // Cuarto pedido
        Order o4 = orders.get("O4");
        time = time.plusMinutes(50); // Tiempo estimado de viaje
        stops2.add(new OrderStop(o4.getId(), o4.getPosition(), time, 8));
        
        // Depósito final
        time = time.plusMinutes(45); // Tiempo estimado de viaje de vuelta
        stops2.add(new DepotStop(env.mainDepot, time, 0));
        
        // Crear rutas con los vehículos personalizados
        routes.add(new Route("R1", v1, stops1));
        routes.add(new Route("R2", v2, stops2));
        
        return new Solution(orders, routes);
    }
} 