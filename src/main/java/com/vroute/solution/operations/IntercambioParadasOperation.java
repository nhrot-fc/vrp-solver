package com.vroute.solution.operations;

import com.vroute.solution.Ruta;
import com.vroute.solution.RutaManager;
import com.vroute.models.Entorno;

public class IntercambioParadasOperation implements RutaOperation {
    private final RutaManager rutaManager;
    private final Ruta ruta1;
    private final int posicionParada1;
    private final Ruta ruta2;
    private final int posicionParada2;
    private final Entorno entorno;
    
    public IntercambioParadasOperation(RutaManager rutaManager, Ruta ruta1, int posicionParada1, 
                                      Ruta ruta2, int posicionParada2, Entorno entorno) {
        this.rutaManager = rutaManager;
        this.ruta1 = ruta1;
        this.posicionParada1 = posicionParada1;
        this.ruta2 = ruta2;
        this.posicionParada2 = posicionParada2;
        this.entorno = entorno;
    }
    
    @Override
    public boolean execute() {
        return rutaManager.intercambiarParadas(ruta1, posicionParada1, ruta2, posicionParada2, entorno);
    }
    
    @Override
    public boolean undo() {
        // Para deshacer, intercambiamos otra vez las paradas
        return rutaManager.intercambiarParadas(ruta1, posicionParada1, ruta2, posicionParada2, entorno);
    }
    
    @Override
    public String getDescription() {
        return "Intercambio de paradas entre rutas de vehículos " + 
               ruta1.getVehiculo().getId() + " y " + ruta2.getVehiculo().getId();
    }
} 