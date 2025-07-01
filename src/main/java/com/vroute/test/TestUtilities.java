package com.vroute.test;

import com.vroute.models.Blockage;
import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleType;
import com.vroute.models.Position;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestUtilities {

    /**
     * Creates standard test vehicles at origin (0,0)
     */
    public static List<Vehicle> createStandardVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(new Vehicle("TA01", VehicleType.TA, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TA02", VehicleType.TA, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TB03", VehicleType.TB, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TB04", VehicleType.TB, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TC05", VehicleType.TC, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TC06", VehicleType.TC, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TD07", VehicleType.TD, Constants.CENTRAL_STORAGE_LOCATION));
        vehicles.add(new Vehicle("TD08", VehicleType.TD, Constants.CENTRAL_STORAGE_LOCATION));

        // Initialize all vehicles with fuel for testing
        for (Vehicle vehicle : vehicles) {
            vehicle.refuel();
        }

        return vehicles;
    }

    public static Environment createSampleEnvironment() {
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("NORTH", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true, false));
        auxDepots.add(new Depot("EAST", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true, false));

        Depot mainDepot = new Depot("MAIN", Constants.CENTRAL_STORAGE_LOCATION, 100000, true, true);

        return new Environment(createStandardVehicles(), mainDepot, auxDepots, LocalDateTime.of(2025, 1, 1, 0, 0));
    }

    /**
     * Creates an environment with blockages
     */
    public static Environment createEnvironmentWithBlockages() {
        Environment env = createSampleEnvironment();

        // Fill depots with GLP
        env.getMainDepot().refillGLP();
        for (Depot depot : env.getAuxDepots()) {
            depot.refillGLP();
        }

        // Add blockages in a straight line from (0,0) -> (15,0) -> (15,20)
        List<Position> blockagePositions = new ArrayList<>();
        blockagePositions.add(new Position(0, 0));
        blockagePositions.add(new Position(15, 0));
        blockagePositions.add(new Position(15, 20));

        LocalDateTime blockageStart = env.getCurrentTime().minusDays(1);
        LocalDateTime blockageEnd = blockageStart.plusDays(50);

        Blockage blockage = new Blockage(blockageStart, blockageEnd, blockagePositions);
        env.addBlockage(blockage);

        return env;
    }

    /**
     * Creates a small set of sample orders
     */
    public static List<Order> createSmallOrders(LocalDateTime baseTime) {
        List<Order> orders = new ArrayList<>();

        // Create 5 small orders with different positions
        orders.add(new Order("O1", baseTime, baseTime.plusHours(4), 5, new Position(5, 5)));
        orders.add(new Order("O2", baseTime, baseTime.plusHours(5), 10, new Position(10, 10)));
        orders.add(new Order("O3", baseTime, baseTime.plusHours(6), 11, new Position(15, 15)));
        orders.add(new Order("O4", baseTime, baseTime.plusHours(7), 8, new Position(20, 20)));
        orders.add(new Order("O5", baseTime, baseTime.plusHours(8), 2, new Position(25, 25)));

        return orders;
    }

    /**
     * Creates a larger set of sample orders
     */
    public static List<Order> createLargeOrders(LocalDateTime baseTime) {
        List<Order> orders = new ArrayList<>();

        // Create 20 orders at various positions
        for (int i = 0; i < 20; i++) {
            String id = "O" + (i + 1);
            int x = 5 + (i * 3) % 50; // Distribute in a grid pattern
            int y = 5 + (i / 5) * 10;
            int glpRequest = 3 + (i % 10); // Vary GLP requests between 3 and 12

            orders.add(new Order(id, baseTime, baseTime.plusHours(4 + (i % 10)),
                    glpRequest, new Position(x, y)));
        }

        return orders;
    }
}
