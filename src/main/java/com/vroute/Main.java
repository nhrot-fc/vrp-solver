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
        
        // Launch enhanced UI
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
            500, true, true
        );
        mainDepot.refillGLP();
        
        // Aux depots
        List<Depot> auxDepots = new ArrayList<>();
        Depot northDepot = new Depot("NORTH_DEPOT", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 160, true, false);
        northDepot.refillGLP();
        auxDepots.add(northDepot);
        
        Depot eastDepot = new Depot("EAST_DEPOT", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 160, true, false);
        eastDepot.refillGLP();
        auxDepots.add(eastDepot);
        
        // Vehicles - spread them around the map for better visualization
        List<Vehicle> vehicles = new ArrayList<>();
        
        // Create TA vehicles
        for (int i = 1; i <= Constants.TA_UNITS; i++) {
            Position randomPos = new Position(
                Constants.CENTRAL_STORAGE_LOCATION.getX() + (i * 3),
                Constants.CENTRAL_STORAGE_LOCATION.getY() + (i * 2)
            );
            vehicles.add(new Vehicle("TA" + String.format("%02d", i), VehicleType.TA, randomPos));
        }
        
        // Create TB vehicles
        for (int i = 1; i <= Constants.TB_UNITS; i++) {
            Position randomPos = new Position(
                Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION.getX() - (i * 3),
                Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION.getY() - (i * 2)
            );
            vehicles.add(new Vehicle("TB" + String.format("%02d", i), VehicleType.TB, randomPos));
        }
        
        // Create TC vehicles
        for (int i = 1; i <= Constants.TC_UNITS; i++) {
            Position randomPos = new Position(
                Constants.EAST_INTERMEDIATE_STORAGE_LOCATION.getX() - (i * 2),
                Constants.EAST_INTERMEDIATE_STORAGE_LOCATION.getY() + (i * 3)
            );
            vehicles.add(new Vehicle("TC" + String.format("%02d", i), VehicleType.TC, randomPos));
        }
        
        // Create TD vehicles
        for (int i = 1; i <= Constants.TD_UNITS; i++) {
            Position randomPos = new Position(
                30 + (i % 5) * 5,
                20 + (i / 5) * 5
            );
            vehicles.add(new Vehicle("TD" + String.format("%02d", i), VehicleType.TD, randomPos));
        }
        
        // Create environment
        Environment environment = new Environment(vehicles, mainDepot, auxDepots, startDateTime);
        
        // Add some test orders spread across the map
        for (int i = 1; i <= 15; i++) {
            Position randomPosition = new Position(
                (int)(Math.random() * Constants.CITY_LENGTH_X),
                (int)(Math.random() * Constants.CITY_WIDTH_Y)
            );
            
            environment.addOrder(
                new Order(
                    "ORDER" + i,
                    startDateTime,
                    startDateTime.plusHours(4 + i % 10), // Some orders will be due sooner
                    5 + (int)(Math.random() * 20),
                    randomPosition
                )
            );
        }
        
        // Add some blockages
        // A horizontal blockage
        List<Position> blockage1 = new ArrayList<>();
        blockage1.add(new Position(20, 30));
        blockage1.add(new Position(35, 30));
        environment.addBlockage(new Blockage(startDateTime, startDateTime.plusHours(12), blockage1));
        
        // A vertical blockage
        List<Position> blockage2 = new ArrayList<>();
        blockage2.add(new Position(50, 10));
        blockage2.add(new Position(50, 25));
        environment.addBlockage(new Blockage(startDateTime, startDateTime.plusHours(6), blockage2));
        
        return environment;
    }
}
