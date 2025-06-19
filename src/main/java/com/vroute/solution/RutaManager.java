package com.vroute.solution;

import java.util.ArrayList;
import java.util.List;
import com.vroute.models.Vehiculo;
import com.vroute.models.Entorno;
import com.vroute.pathfinding.Pathfinder;
import com.vroute.pathfinding.PathResult;
import java.time.LocalDateTime;

public class RutaManager {
    private List<Ruta> rutas;
    
    public RutaManager() {
        this.rutas = new ArrayList<>();
    }
    
    public RutaManager(List<Ruta> rutas) {
        this.rutas = new ArrayList<>(rutas);
    }
    
    public List<Ruta> getRutas() {
        return new ArrayList<>(rutas);
    }
    
    // Add a new route
    public void addRuta(Ruta ruta) {
        rutas.add(ruta);
    }
    
    // Exchange vehicles between routes
    public boolean intercambiarVehiculos(Ruta ruta1, Ruta ruta2, Entorno entorno) {
        if (!esIntercambioFactible(ruta1, ruta2)) {
            return false;
        }
        
        Vehiculo v1 = ruta1.getVehiculo();
        Vehiculo v2 = ruta2.getVehiculo();
        
        List<Parada> paradasRuta1 = new ArrayList<>(ruta1.getParadas());
        List<Parada> paradasRuta2 = new ArrayList<>(ruta2.getParadas());
        
        // Recalculate routes with new vehicles
        recalcularRuta(paradasRuta1, v2, entorno);
        recalcularRuta(paradasRuta2, v1, entorno);
        
        Ruta nuevaRuta1 = new Ruta(v2, paradasRuta1);
        Ruta nuevaRuta2 = new Ruta(v1, paradasRuta2);
        
        // Replace routes
        rutas.set(rutas.indexOf(ruta1), nuevaRuta1);
        rutas.set(rutas.indexOf(ruta2), nuevaRuta2);
        
        return true;
    }
    
    // Check if vehicle exchange is feasible
    private boolean esIntercambioFactible(Ruta ruta1, Ruta ruta2) {
        Vehiculo v1 = ruta1.getVehiculo();
        Vehiculo v2 = ruta2.getVehiculo();
        
        // Check if vehicles are available
        if (v1.getEstado() != Vehiculo.Estado.DISPONIBLE || 
            v2.getEstado() != Vehiculo.Estado.DISPONIBLE) {
            return false;
        }
        
        // Check GLP capacities
        if (v1.getTipo().getCapacidadGLP() < ruta2.getGLPTotalEntregado() || 
            v2.getTipo().getCapacidadGLP() < ruta1.getGLPTotalEntregado()) {
            return false;
        }
        
        return true;
    }
    
    // Add a stop to a route
    public boolean agregarParada(Ruta ruta, Parada nuevaParada, int posicion, Entorno entorno) {
        if (!esAgregacionFactible(ruta, nuevaParada)) {
            return false;
        }
        
        List<Parada> nuevasParadas = new ArrayList<>(ruta.getParadas());
        nuevasParadas.add(posicion, nuevaParada);
        
        // Recalculate distances and ETAs
        recalcularRuta(nuevasParadas, ruta.getVehiculo(), entorno);
        
        Ruta nuevaRuta = new Ruta(ruta.getVehiculo(), nuevasParadas);
        rutas.set(rutas.indexOf(ruta), nuevaRuta);
        
        return true;
    }
    
    private boolean esAgregacionFactible(Ruta ruta, Parada nuevaParada) {
        Vehiculo vehiculo = ruta.getVehiculo();
        
        // Check vehicle capacity
        int glpTotal = ruta.getGLPTotalEntregado() + 
                      (nuevaParada.getTipo() == Parada.Tipo.DELIVERY ? 
                       nuevaParada.getCantidadGLPEntregada() : 0);
        
        return vehiculo.getTipo().getCapacidadGLP() >= glpTotal;
    }
    
    // Intercambiar paradas entre rutas
    public boolean intercambiarParadas(Ruta ruta1, int posicionParada1, Ruta ruta2, int posicionParada2, Entorno entorno) {
        if (posicionParada1 >= ruta1.getParadas().size() || posicionParada2 >= ruta2.getParadas().size()) {
            return false;
        }
        
        Parada parada1 = ruta1.getParadas().get(posicionParada1);
        Parada parada2 = ruta2.getParadas().get(posicionParada2);
        
        if (!esIntercambioParadasFactible(ruta1, parada2, ruta2, parada1)) {
            return false;
        }
        
        List<Parada> nuevasParadas1 = new ArrayList<>(ruta1.getParadas());
        List<Parada> nuevasParadas2 = new ArrayList<>(ruta2.getParadas());
        
        nuevasParadas1.set(posicionParada1, parada2);
        nuevasParadas2.set(posicionParada2, parada1);
        
        recalcularRuta(nuevasParadas1, ruta1.getVehiculo(), entorno);
        recalcularRuta(nuevasParadas2, ruta2.getVehiculo(), entorno);
        
        Ruta nuevaRuta1 = new Ruta(ruta1.getVehiculo(), nuevasParadas1);
        Ruta nuevaRuta2 = new Ruta(ruta2.getVehiculo(), nuevasParadas2);
        
        rutas.set(rutas.indexOf(ruta1), nuevaRuta1);
        rutas.set(rutas.indexOf(ruta2), nuevaRuta2);
        
        return true;
    }
    
    private boolean esIntercambioParadasFactible(Ruta ruta1, Parada nuevaParada1, Ruta ruta2, Parada nuevaParada2) {
        // Check vehicle capacities for both routes after exchange
        int glpRuta1 = ruta1.getGLPTotalEntregado() - 
                      (nuevaParada2.getTipo() == Parada.Tipo.DELIVERY ? nuevaParada2.getCantidadGLPEntregada() : 0) +
                      (nuevaParada1.getTipo() == Parada.Tipo.DELIVERY ? nuevaParada1.getCantidadGLPEntregada() : 0);
                      
        int glpRuta2 = ruta2.getGLPTotalEntregado() - 
                      (nuevaParada1.getTipo() == Parada.Tipo.DELIVERY ? nuevaParada1.getCantidadGLPEntregada() : 0) +
                      (nuevaParada2.getTipo() == Parada.Tipo.DELIVERY ? nuevaParada2.getCantidadGLPEntregada() : 0);
        
        return ruta1.getVehiculo().getTipo().getCapacidadGLP() >= glpRuta1 &&
               ruta2.getVehiculo().getTipo().getCapacidadGLP() >= glpRuta2;
    }
    
    // Helper method to recalculate distances and ETAs after route changes
    private void recalcularRuta(List<Parada> paradas, Vehiculo vehiculo, Entorno entorno) {
        if (paradas.isEmpty()) {
            return;
        }
        
        LocalDateTime horaActual = entorno.getHoraActual();
        double distanciaAcumulada = 0.0;
        
        for (int i = 0; i < paradas.size(); i++) {
            Parada paradaActual = paradas.get(i);
            PathResult pathResult;
            
            // Para la primera parada, calcula desde la posición actual del vehículo
            if (i == 0) {
                pathResult = Pathfinder.getPath(
                    vehiculo.getPosicionActual(),
                    paradaActual.getPosicion(),
                    horaActual,
                    entorno
                );
            } 
            // Para las demás paradas, calcula desde la parada anterior
            else {
                Parada paradaAnterior = paradas.get(i - 1);
                pathResult = Pathfinder.getPath(
                    paradaAnterior.getPosicion(),
                    paradaActual.getPosicion(),
                    horaActual,
                    entorno
                );
            }
            
            // Si no se pudo encontrar un camino, no podemos continuar
            if (pathResult.getNodosDelCamino().isEmpty()) {
                throw new RuntimeException("No se pudo encontrar un camino entre las paradas");
            }
            
            // Actualizar distancia acumulada y hora actual
            distanciaAcumulada += pathResult.getDistancia();
            horaActual = horaActual.plusSeconds((long)(pathResult.getDuracion() * 3600));
            
            // Actualizar la parada con la información recalculada
            Parada paradaActualizada;
            if (paradaActual.getTipo() == Parada.Tipo.DELIVERY) {
                paradaActualizada = ParadaFactory.actualizarDelivery(
                    (Delivery) paradaActual,
                    distanciaAcumulada,
                    horaActual
                );
            } else { // RECARGA
                paradaActualizada = ParadaFactory.actualizarRecarga(
                    (Recarga) paradaActual,
                    distanciaAcumulada,
                    horaActual
                );
            }
            
            // Reemplazar la parada en la lista
            paradas.set(i, paradaActualizada);
        }
    }
} 