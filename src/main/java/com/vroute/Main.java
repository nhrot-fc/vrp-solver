package com.vroute;

import com.vroute.models.*;
import com.vroute.ui.SimulationApp;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a simple test environment
        Environment environment = createTestEnvironment();
        
        // Launch UI
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            SimulationApp app = new SimulationApp();
            app.setEnvironment(environment);
        });
    }
    
    private static Environment createTestEnvironment() {
        // Initial date time
        LocalDateTime startDateTime = LocalDateTime.now();
        
        // Main depot
        Depot mainDepot = new Depot(
            Constants.MAIN_PLANT_ID, 
            Constants.CENTRAL_STORAGE_LOCATION,
            500, true
        );
        mainDepot.refillGLP();
        
        // Aux depots
        List<Depot> auxDepots = new ArrayList<>();
        Depot northDepot = new Depot("NORTH_DEPOT", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true);
        northDepot.refillGLP();
        auxDepots.add(northDepot);
        
        Depot eastDepot = new Depot("EAST_DEPOT", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true);
        eastDepot.refillGLP();
        auxDepots.add(eastDepot);
        
        // Vehicles
        List<Vehicle> vehicles = new ArrayList<>();
        
        // Create all vehicle types
        for (int i = 1; i <= Constants.TA_UNITS; i++) {
            vehicles.add(new Vehicle("TA" + String.format("%02d", i), VehicleType.TA, Constants.CENTRAL_STORAGE_LOCATION));
        }
        
        for (int i = 1; i <= Constants.TB_UNITS; i++) {
            vehicles.add(new Vehicle("TB" + String.format("%02d", i), VehicleType.TB, Constants.CENTRAL_STORAGE_LOCATION));
        }
        
        for (int i = 1; i <= Constants.TC_UNITS; i++) {
            vehicles.add(new Vehicle("TC" + String.format("%02d", i), VehicleType.TC, Constants.CENTRAL_STORAGE_LOCATION));
        }
        
        for (int i = 1; i <= Constants.TD_UNITS; i++) {
            vehicles.add(new Vehicle("TD" + String.format("%02d", i), VehicleType.TD, Constants.CENTRAL_STORAGE_LOCATION));
        }
        
        // Create environment
        Environment environment = new Environment(vehicles, mainDepot, auxDepots, startDateTime);
        
        // Add some test orders
        for (int i = 1; i <= 10; i++) {
            Position randomPosition = new Position(
                (int)(Math.random() * Constants.CITY_LENGTH_X),
                (int)(Math.random() * Constants.CITY_WIDTH_Y)
            );
            
            environment.addOrder(
                new Order(
                    "ORDER" + i,
                    startDateTime,
                    startDateTime.plusHours(24),
                    5 + (int)(Math.random() * 20),
                    randomPosition
                )
            );
        }
        
        return environment;
    }
}
