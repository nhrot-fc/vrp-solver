package com.vroute.setup;

import com.vroute.models.*;
import com.vroute.orchest.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase para inicializar el entorno de simulación y cargar datos iniciales
 */
public class EnvironmentSetup {

    private final DataReader dataReader;
    
    public EnvironmentSetup() {
        this.dataReader = new DataReader();
    }
    
    /**
     * Crea un nuevo entorno con la fecha de inicio especificada
     * 
     * @param startDate Fecha y hora de inicio de la simulación
     * @return Entorno inicializado
     */
    public Environment createEnvironment(LocalDateTime startDate) {
        // Crear el depósito principal en posición (12, 8)
        Depot mainDepot = new Depot("MAIN", new Position(12, 8), 1000, true);
        mainDepot.refillGLP(); // Llenar de GLP inicialmente
        
        // Crear depósitos auxiliares
        List<Depot> auxDepots = new ArrayList<>();
        
        Depot aux1 = new Depot("AUX1", new Position(25, 15), 500, true);
        aux1.refillGLP();
        auxDepots.add(aux1);
        
        Depot aux2 = new Depot("AUX2", new Position(40, 35), 500, false);
        aux2.refillGLP();
        auxDepots.add(aux2);
        
        Depot aux3 = new Depot("AUX3", new Position(60, 10), 500, true);
        aux3.refillGLP();
        auxDepots.add(aux3);
        
        // Cargar vehículos
        List<Vehicle> vehicles = dataReader.loadVehicles(""); // Ruta vacía porque los vehículos son fijos
        
        // Crear el entorno
        return new Environment(vehicles, mainDepot, auxDepots, startDate);
    }
    
    /**
     * Inicializa un orquestrador con los archivos de datos correspondientes al mes actual
     * 
     * @param environment Entorno de simulación
     * @return Orquestrador inicializado
     */
    public Orchestrator createOrchestrator(Environment environment) {
        Orchestrator orchestrator = new Orchestrator(environment);
        
        // Obtener el mes y año actuales
        LocalDateTime currentTime = environment.getCurrentTime();
        int year = currentTime.getYear();
        int month = currentTime.getMonthValue();
        
        // Formatear el mes y año para los nombres de archivo
        String monthYearFormat = String.format("%d%02d", year, month);
        
        String baseDataPath = "data/";
        
        // Archivos de pedidos para el mes actual
        String ordersPath = baseDataPath + "pedidos.20250419/ventas" + monthYearFormat + ".txt";
        
        // Archivos de bloqueos para el mes actual
        String blockagesPath = baseDataPath + "bloqueos.20250419/" + monthYearFormat + ".bloqueos.txt";
        
        // Archivo de mantenimiento programado
        String maintenancePath = baseDataPath + "mantpreventivo.txt";
        
        // Cargar eventos
        orchestrator.loadEvents(ordersPath, blockagesPath, maintenancePath);
        
        return orchestrator;
    }
}
