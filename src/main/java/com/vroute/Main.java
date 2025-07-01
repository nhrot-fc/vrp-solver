package com.vroute;

import com.vroute.models.*;
import com.vroute.orchest.*;
import com.vroute.ui.SimulationApp;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Crear el entorno inicial
        Environment environment = createEnvironment();
        
        // Crear el Orchestrator
        Orchestrator orchestrator = new Orchestrator(environment);
        
        // Cargar todos los eventos
        loadEvents(orchestrator);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Pasar el orchestrator en lugar del environment directamente
            SimulationApp app = new SimulationApp(orchestrator.getEnvironment());
            app.setVisible(true);
        });
    }
    
    private static void loadEvents(Orchestrator orchestrator) {
        // Usar DataReader para cargar y crear eventos
        DataReader dataReader = new DataReader();
        
        // Definir fecha de inicio y duración de la simulación
        LocalDateTime startDate = orchestrator.getEnvironment().getCurrentTime();
        int simulationDays = 7; // Simular una semana
        LocalDateTime endDate = startDate.plusDays(simulationDays);
        
        // Cargar todos los eventos
        List<Event> events = dataReader.loadAllEvents(startDate, endDate);
        
        // Agregar eventos al orchestrator
        orchestrator.addEvents(events);
        
        System.out.println("Cargados " + events.size() + " eventos para la simulación");
    }

    private static Environment createEnvironment() {
        LocalDateTime referenceDateTime = LocalDateTime.of(2025, 3, 1, 8, 0); // 1 de marzo 2025 a las 8:00
        
        // Cargar vehículos desde el DataReader
        DataReader dataReader = new DataReader();
        List<Vehicle> vehicles = dataReader.loadVehicles(null); // No se necesita un archivo para vehículos

        // Crear depósitos
        Depot mainDepot = new Depot("MAIN", Constants.CENTRAL_STORAGE_LOCATION, 10000, true, true);
        mainDepot.refillGLP(); // Asegurar que el depósito principal está lleno
        
        List<Depot> auxDepots = new ArrayList<>();
        Depot northDepot = new Depot("NORTH", Constants.NORTH_INTERMEDIATE_STORAGE_LOCATION, 1000, true, false);
        Depot eastDepot = new Depot("EAST", Constants.EAST_INTERMEDIATE_STORAGE_LOCATION, 1000, true, false);
        northDepot.refillGLP();
        eastDepot.refillGLP();
        
        auxDepots.add(northDepot);
        auxDepots.add(eastDepot);

        // Crear y retornar el entorno
        Environment env = new Environment(vehicles, mainDepot, auxDepots, referenceDateTime);
        
        // Asegurar que todos los vehículos tengan combustible
        for (Vehicle vehicle : vehicles) {
            vehicle.refuel();
        }
        
        return env;
    }
}
