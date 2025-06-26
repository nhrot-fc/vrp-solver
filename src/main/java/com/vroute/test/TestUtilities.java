package com.vroute.test;

import com.vroute.models.Blockage;
import com.vroute.models.Constants;
import com.vroute.models.Depot;
import com.vroute.models.Environment;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleType;
import com.vroute.models.Position;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        return vehicles;
    }

    public static Environment createSampleEnvironment() {
        List<Depot> auxDepots = new ArrayList<>();
        auxDepots.add(new Depot("NORTH", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true, false));
        auxDepots.add(new Depot("EAST", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true, false));

        Depot mainDepot = new Depot("MAIN", Constants.CENTRAL_STORAGE_LOCATION, 160, true, true);

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

        // Add blockages in a straight line from (0,0) to (15,15)
        // This will force the pathfinder to find an alternative route
        List<Position> blockagePositions = new ArrayList<>();
        for (int i = 5; i < 12; i++) {
            blockagePositions.add(new Position(i, i));
        }

        LocalDateTime blockageStart = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime blockageEnd = blockageStart.plus(1, ChronoUnit.DAYS);

        Blockage blockage = new Blockage(blockageStart, blockageEnd, blockagePositions);
        env.addBlockage(blockage);

        return env;
    }
}
