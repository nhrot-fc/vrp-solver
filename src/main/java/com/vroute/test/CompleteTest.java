package com.vroute.test;

import com.vroute.models.*;
import com.vroute.orchest.DataReader;
import com.vroute.solution.Evaluator;
import com.vroute.solution.Solution;
import com.vroute.taboo.TabuSearch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.vroute.test.TestFramework.Assertions.*;

/**
 * Suite de pruebas completa para evaluar la integración del algoritmo
 * TabuSearch
 * con el entorno de datos reales.
 */
public class CompleteTest {

    /**
     * Crea una suite de tests para evaluar el algoritmo TabuSearch
     * 
     * @return Suite de tests configurada
     */
    public static TestFramework.TestSuite createSuite() {
        TestFramework.TestSuite suite = new TestFramework.TestSuite("CompleteIntegration");

        // Añadir tests con diferentes cantidades de órdenes
        suite.addTest(new TabuSearchIntegrationTest(5));
        suite.addTest(new TabuSearchIntegrationTest(10));
        suite.addTest(new TabuSearchIntegrationTest(20));

        return suite;
    }

    /**
     * Test de integración para el algoritmo TabuSearch con datos reales
     */
    private static class TabuSearchIntegrationTest extends TestFramework.AbstractTest {
        private final int maxOrders;

        public TabuSearchIntegrationTest(int maxOrders) {
            super("TabuSearch Integration (" + maxOrders + " orders)");
            this.maxOrders = maxOrders;
        }

        @Override
        protected void runTest() throws Throwable {
            // Setup test environment
            Environment env = setupTestEnvironment(maxOrders);
            System.out.println("Environment created with:");
            System.out.println(" - " + env.getVehicles().size() + " vehicles");
            System.out.println(" - " + env.getOrderQueue().size() + " orders");
            System.out.println(" - " + env.getActiveBlockagesAt(env.getCurrentTime()).size() + " active blockages");
            System.out.println(" - " + (env.getDepots().size() - 1) + " auxiliary depots");

            // Create and run TabuSearch solver
            long startTime = System.currentTimeMillis();
            TabuSearch tabuSolver = new TabuSearch();
            Solution solution = tabuSolver.solve(env);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // Evaluate solution
            double solutionCost = Evaluator.evaluateSolution(env, solution);

            // Print results
            System.out.println("\n==== TabuSearch Results for " + maxOrders + " orders ====");
            System.out.println("Execution time: " + executionTime + "ms");
            System.out.println("Solution cost: " + solutionCost);
            System.out.println("Routes created: " + solution.getRoutes().size());
            System.out.println("Orders served: " + countServedOrders(solution) + "/" + env.getOrderQueue().size());

            // Assertions
            assertNotNull(solution, "Solution should not be null");
            assertFalse(solution.getRoutes().isEmpty(), "Solution should have at least one route");
            assertFalse(Double.isInfinite(solutionCost), "Solution cost should be finite");
            assertTrue(countServedOrders(solution) > 0, "At least one order should be served");
        }
    }

    /**
     * Setup a test environment with vehicles, orders, and blockages
     * 
     * @param maxOrders Maximum number of orders to load
     * @return Configured Environment
     */
    private static Environment setupTestEnvironment(int maxOrders) {
        // Initial date time
        LocalDateTime startDateTime = LocalDateTime.of(2025, 1, 1, 0, 0);

        // Initialize data reader
        DataReader dataReader = new DataReader();

        // Main depot
        Depot mainDepot = new Depot(
                Constants.MAIN_PLANT_ID,
                Constants.CENTRAL_STORAGE_LOCATION,
                500, true, true);
        mainDepot.refillGLP();

        // Aux depots
        List<Depot> auxDepots = new ArrayList<>();
        Depot northDepot = new Depot("NORTH_DEPOT", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true, false);
        northDepot.refillGLP();
        auxDepots.add(northDepot);

        Depot eastDepot = new Depot("EAST_DEPOT", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true, false);
        eastDepot.refillGLP();
        auxDepots.add(eastDepot);

        List<Vehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            vehicles.add(new Vehicle(String.format("TA%02d", i), VehicleType.TA, Constants.CENTRAL_STORAGE_LOCATION));
        }
        for (int i = 0; i < 4; i++) {
            vehicles.add(new Vehicle(String.format("TB%02d", i), VehicleType.TB, Constants.CENTRAL_STORAGE_LOCATION));
        }
        for (int i = 0; i < 4; i++) {
            vehicles.add(new Vehicle(String.format("TC%02d", i), VehicleType.TC, Constants.CENTRAL_STORAGE_LOCATION));
        }
        for (int i = 0; i < 10; i++) {
            vehicles.add(new Vehicle(String.format("TD%02d", i), VehicleType.TD, Constants.CENTRAL_STORAGE_LOCATION));
        }

        Environment environment = new Environment(vehicles, mainDepot, auxDepots, startDateTime);
        String ordersFilePath = String.format("data/pedidos.20250419/ventas%s.txt",
                startDateTime.format(DateTimeFormatter.ofPattern("yyyyMM")));
        List<Order> orders = dataReader.loadOrders(ordersFilePath, startDateTime, 200, maxOrders);
        environment.addOrders(orders);
        String blockagesFilePath = String.format("data/bloqueos.20250419/%s.bloqueos.txt",
                startDateTime.format(DateTimeFormatter.ofPattern("yyyyMM")));
        List<Blockage> blockages = dataReader.loadBlockages(blockagesFilePath, startDateTime, 200, 0);
        environment.addBlockages(blockages);

        System.out.println(environment);

        return environment;
    }

    /**
     * Count the number of orders served in a solution
     * 
     * @param solution The solution to analyze
     * @return Number of orders served
     */
    private static int countServedOrders(Solution solution) {
        int count = 0;
        for (Order order : solution.getOrders().values()) {
            if (order.isDelivered()) {
                count++;
            }
        }
        return count;
    }
}
