package com.vroute.solution.operations;

import com.vroute.solution.Parada;
import com.vroute.solution.Ruta;
import com.vroute.solution.RutaManager;
import com.vroute.models.Entorno;

public class AgregarParadaOperation implements RutaOperation {
    private final RutaManager rutaManager;
    private final Ruta ruta;
    private final Parada parada;
    private final int posicion;
    private final Entorno entorno;
    
    public AgregarParadaOperation(RutaManager rutaManager, Ruta ruta, Parada parada, int posicion, Entorno entorno) {
        this.rutaManager = rutaManager;
        this.ruta = ruta;
        this.parada = parada;
        this.posicion = posicion;
        this.entorno = entorno;
    }
    
    @Override
    public boolean execute() {
        return rutaManager.agregarParada(ruta, parada, posicion, entorno);
    }
    
    @Override
    public boolean undo() {
        // Para deshacer, necesitaríamos una operación de eliminar parada
        // Como ruta es inmutable, tendríamos que crear una nueva sin la parada
        // Esto requeriría una implementación adicional en RutaManager
        return false;
    }
    
    @Override
    public String getDescription() {
        return "Agregar parada a ruta del vehículo " + ruta.getVehiculo().getId();
    }
} 