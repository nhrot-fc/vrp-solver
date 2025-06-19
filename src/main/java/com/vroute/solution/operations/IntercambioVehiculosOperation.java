package com.vroute.solution.operations;

import com.vroute.solution.Ruta;
import com.vroute.solution.RutaManager;
import com.vroute.models.Entorno;

public class IntercambioVehiculosOperation implements RutaOperation {
    private final RutaManager rutaManager;
    private final Ruta ruta1;
    private final Ruta ruta2;
    private final Entorno entorno;
    private Ruta nuevaRuta1;
    private Ruta nuevaRuta2;
    
    public IntercambioVehiculosOperation(RutaManager rutaManager, Ruta ruta1, Ruta ruta2, Entorno entorno) {
        this.rutaManager = rutaManager;
        this.ruta1 = ruta1;
        this.ruta2 = ruta2;
        this.entorno = entorno;
    }
    
    @Override
    public boolean execute() {
        return rutaManager.intercambiarVehiculos(ruta1, ruta2, entorno);
    }
    
    @Override
    public boolean undo() {
        return rutaManager.intercambiarVehiculos(nuevaRuta1, nuevaRuta2, entorno);
    }
    
    @Override
    public String getDescription() {
        return "Intercambio de vehículos entre rutas " + ruta1.getVehiculo().getId() + " y " + ruta2.getVehiculo().getId();
    }
} 