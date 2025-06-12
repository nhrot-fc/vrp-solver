package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Vehicle;
import com.vroute.models.Order;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Implementación del algoritmo Tabu Search para optimización de rutas de
 * entrega.
 * 
 * El algoritmo sigue estos pasos:
 * 1. Generar una solución inicial
 * 2. Explorar el vecindario de soluciones (posibles movimientos)
 * 3. Mantener una lista tabú para evitar ciclos
 * 4. Usar criterio de aspiración para permitir movimientos tabú si mejoran la
 * mejor solución
 * 5. Moverse a la mejor solución no tabú (o aspirada)
 */
public class TabuSearchOptimizer implements Optimizer {

    private static final int DEFAULT_MAX_ITERATIONS = 5000;
    private static final int DEFAULT_TABU_LIST_SIZE = 30;
    private static final int DEFAULT_NUM_NEIGHBORS = 200;
    private static final int REPORT_INTERVAL = 100;

    private final int maxIterations;
    private final int tabuListSize;
    private final int numNeighbors;

    public TabuSearchOptimizer() {
        this.maxIterations = DEFAULT_MAX_ITERATIONS;
        this.tabuListSize = DEFAULT_TABU_LIST_SIZE;
        this.numNeighbors = DEFAULT_NUM_NEIGHBORS;
    }

    public TabuSearchOptimizer(int maxIterations, int tabuListSize, int numNeighbors) {
        this.maxIterations = maxIterations;
        this.tabuListSize = tabuListSize;
        this.numNeighbors = numNeighbors;
    }

    @Override
    public Solution solve(Environment env) {
        // 1. Inicialización
        Solution currentSolution = SolutionGenerator.generateInitialSolution(env);
        Solution bestSolution = currentSolution;
        Queue<TabuMove> tabuList = new LinkedList<>();

        System.out.println("Starting Tabu Search optimization with " + maxIterations + " iterations");
        System.out.println("Initial solution cost: " + currentSolution.getTotalCost());

        // 2. Bucle Principal
        for (int i = 1; i <= maxIterations; i++) {
            // Generar vecindad
            List<Solution> neighborhood = SolutionGenerator.generateNeighborhood(currentSolution, numNeighbors, env);

            Solution bestNeighbor = null;
            TabuMove bestMove = null;

            // 3. Evaluar la Vecindad
            for (Solution neighbor : neighborhood) {
                TabuMove move = getMove(currentSolution, neighbor);
                boolean isTabu = isTabu(tabuList, move);

                // 4. Criterio de Aspiración
                boolean isAspirated = (neighbor.getTotalCost() < bestSolution.getTotalCost());

                // 5. Selección del Mejor Vecino
                if (!isTabu || isAspirated) {
                    if (bestNeighbor == null || neighbor.getTotalCost() < bestNeighbor.getTotalCost()) {
                        bestNeighbor = neighbor;
                        bestMove = move;
                    }
                }
            }

            // Si no se encontró ningún vecino válido
            if (bestNeighbor == null) {
                System.out.println("Iteration " + i + ": No valid neighbor found, continuing");
                continue;
            }

            // 6. Moverse a la siguiente solución
            currentSolution = bestNeighbor;

            // 7. Actualizar la Lista Tabú
            updateTabuList(tabuList, bestMove, tabuListSize);

            // 8. Actualizar la Mejor Solución Global
            if (currentSolution.getTotalCost() < bestSolution.getTotalCost()) {
                bestSolution = currentSolution;
                System.out.println(
                        "Iteration " + i + ": New best solution found with cost " + bestSolution.getTotalCost());
            }

            // Reportar progreso periódicamente
            if (i % REPORT_INTERVAL == 0) {
                System.out.println("Iteration " + i + "/" + maxIterations +
                        " - Current cost: " + currentSolution.getTotalCost() +
                        " - Best cost: " + bestSolution.getTotalCost() +
                        " - Unassigned: " + bestSolution.getUnassignedOrders().size());
            }
        }

        // 9. Resultado
        System.out.println("Tabu Search completed after " + maxIterations + " iterations.");
        System.out.println("Best solution cost: " + bestSolution.getTotalCost());
        System.out.println("Unassigned orders: " + bestSolution.getUnassignedOrders().size());

        if (bestSolution.getUnassignedOrders().isEmpty()) {
            System.out.println("All orders were successfully assigned!");
        } else {
            System.out.println("Warning: " + bestSolution.getUnassignedOrders().size() +
                    " orders could not be assigned in the best solution found.");
        }

        return bestSolution;
    }

    /**
     * Determina el movimiento realizado para pasar de la solución actual a la nueva
     */
    private TabuMove getMove(Solution currentSolution, Solution newSolution) {
        // En una implementación real, este método detectaría qué cambio específico
        // se realizó entre las dos soluciones para obtener el movimiento tabú
        // correspondiente.

        // Ejemplo simplificado: identificamos cambios entre soluciones
        if (!currentSolution.getVehiclePlans().isEmpty() && !newSolution.getVehiclePlans().isEmpty()) {
            // Tomamos vehículos de ejemplo para el movimiento
            Vehicle anySourceVehicle = currentSolution.getVehiclePlans().keySet().iterator().next();
            Vehicle anyTargetVehicle = newSolution.getVehiclePlans().keySet().iterator().next();

            // Tomamos una orden de ejemplo (la primera no asignada, si hay alguna)
            Order anyOrder = !currentSolution.getUnassignedOrders().isEmpty()
                    ? currentSolution.getUnassignedOrders().get(0)
                    : (!newSolution.getUnassignedOrders().isEmpty()
                            ? newSolution.getUnassignedOrders().get(0)
                            : null);

            if (anyOrder != null) {
                return new TabuMove(
                        anySourceVehicle,
                        anyTargetVehicle,
                        anyOrder,
                        MoveType.TRANSFER,
                        1 // Cantidad de ejemplo
                );
            }
        }

        // Si no podemos determinar el movimiento específico
        return null;
    }

    /**
     * Verifica si un movimiento está en la lista tabú
     */
    private boolean isTabu(Queue<TabuMove> tabuList, TabuMove move) {
        if (move == null || tabuList.isEmpty())
            return false;

        for (TabuMove tabuMove : tabuList) {
            if (tabuMove.equals(move)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Actualiza la lista tabú, añadiendo el nuevo movimiento y
     * eliminando los más antiguos si se supera el tamaño máximo
     */
    private void updateTabuList(Queue<TabuMove> tabuList, TabuMove move, int maxSize) {
        if (move == null)
            return;

        // Añadir el inverso del movimiento realizado a la lista tabú para evitar ciclos
        tabuList.add(move.getInverseMove());

        // Mantener la lista tabú con un tamaño máximo
        while (tabuList.size() > maxSize) {
            tabuList.poll(); // Eliminar el movimiento más antiguo
        }
    }
}
