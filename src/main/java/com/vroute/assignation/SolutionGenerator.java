package com.vroute.assignation;

import com.vroute.models.Environment;
import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.models.Depot;
import com.vroute.models.Position;
import com.vroute.models.Constants;
import com.vroute.operation.VehiclePlan;
import com.vroute.operation.Action;
import com.vroute.operation.DriveAction;
import com.vroute.operation.ServeAction;
import com.vroute.operation.ReloadAction;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Comparator;

/**
 * Se encarga de construir soluciones desde cero y de generar soluciones vecinas
 * a partir de una existente. Es el "constructor" que utiliza el algoritmo de
 * búsqueda Tabu Search.
 * Implementada con métodos estáticos para facilitar su uso.
 */
public class SolutionGenerator {

    private static final Random random = new Random();

    /**
     * Genera una solución inicial utilizando una heurística simple.
     * 
     * @param environment El estado actual del mundo (vehículos, pedidos
     *                    pendientes).
     * @return Una nueva solución completa.
     */
    public static Solution generateInitialSolution(Environment environment) {
        // Usar el DeliveryDistribuitor para crear una asignación inicial
        Map<Vehicle, List<DeliveryInstruction>> assignments = DeliveryDistribuitor
                .createInitialRandomAssignments(environment);

        // Convertir las asignaciones en planes de vehículos
        Map<Vehicle, VehiclePlan> vehiclePlans = new HashMap<>();
        List<Order> unassignedOrders = new ArrayList<>();

        // Para cada vehículo, crear un plan basado en sus instrucciones asignadas
        for (Vehicle vehicle : environment.getAvailableVehicles()) {
            List<DeliveryInstruction> vehicleInstructions = assignments.getOrDefault(vehicle, new ArrayList<>());
            if (!vehicleInstructions.isEmpty()) {
                VehiclePlan plan = buildVehiclePlan(vehicle, vehicleInstructions, environment);
                vehiclePlans.put(vehicle, plan);
            }
        }

        // Verificar qué órdenes no fueron asignadas completamente
        for (Order order : environment.getPendingOrders()) {
            if (!isOrderFullyAssigned(order, assignments)) {
                unassignedOrders.add(order);
            }
        }

        // Crear la solución inicial con un costo aproximado
        Solution initialSolution = new Solution(vehiclePlans, unassignedOrders, 0.0);

        // Evaluar la solución para calcular su costo real
        return SolutionEvaluator.evaluate(initialSolution, environment);
    }

    /**
     * Genera una "vecindad" de soluciones a partir de una solución existente.
     *
     * @param currentSolution  La solución base.
     * @param neighborhoodSize El número de vecinos a generar.
     * @param environment      El entorno actual para evaluar restricciones
     * @return Una lista de nuevas soluciones, cada una con una pequeña
     *         modificación.
     */
    public static List<Solution> generateNeighborhood(Solution currentSolution, int neighborhoodSize,
            Environment environment) {
        List<Solution> neighborhood = new ArrayList<>();

        for (int i = 0; i < neighborhoodSize; i++) {
            // Elegir un tipo de movimiento aleatorio
            MoveType moveType = getRandomMoveType();

            // Aplicar el movimiento para generar un vecino
            Solution neighbor = null;

            switch (moveType) {
                case TRANSFER:
                    neighbor = applyTransferMove(currentSolution, environment);
                    break;
                case SWAP:
                    neighbor = applySwapMove(currentSolution, environment);
                    break;
                case REORDER:
                    neighbor = applyReorderMove(currentSolution, environment);
                    break;
            }

            // Si se generó un vecino válido, añadirlo al vecindario
            if (neighbor != null) {
                neighborhood.add(SolutionEvaluator.evaluate(neighbor, environment));
            }
        }

        return neighborhood;
    }

    /**
     * Verifica si una orden está completamente asignada en la solución.
     */
    private static boolean isOrderFullyAssigned(Order order, Map<Vehicle, List<DeliveryInstruction>> assignments) {
        int totalAssigned = 0;

        for (List<DeliveryInstruction> instructions : assignments.values()) {
            for (DeliveryInstruction instruction : instructions) {
                if (instruction.getOrderId().equals(order.getId())) {
                    totalAssigned += instruction.getGlpAmountToDeliver();
                }
            }
        }

        return totalAssigned >= order.getRemainingGlpM3();
    }

    /**
     * Construye un plan de vehículo a partir de las instrucciones asignadas.
     * 
     * @param vehicle      El vehículo para el cual se construye el plan
     * @param instructions Las instrucciones de entrega asignadas al vehículo
     * @param environment  El entorno actual para acceder a depósitos y calcular
     *                     rutas
     * @return Un plan de vehículo completo con acciones detalladas
     */
    private static VehiclePlan buildVehiclePlan(Vehicle vehicle, List<DeliveryInstruction> instructions,
            Environment environment) {
        // Lista de acciones que compondrán el plan
        List<Action> actions = new ArrayList<>();
        Vehicle workingVehicle = vehicle.clone(); // Para mantener el estado actualizado del vehículo

        // Variables para rastrear estado
        double totalDistance = 0.0;
        double estimatedCost = 0.0;
        LocalDateTime currentTime = environment.getCurrentTime();
        Position currentPosition = workingVehicle.getCurrentPosition();

        // Ordenamos las entregas por fecha de vencimiento para priorizar pedidos más
        // urgentes
        instructions.sort(Comparator.comparing(DeliveryInstruction::getDueDate));

        // Depot principal para recargas
        Depot mainDepot = environment.getMainDepot();

        // Construir la secuencia de acciones
        for (DeliveryInstruction instruction : instructions) {
            Order order = instruction.getOriginalOrder();
            int glpAmount = instruction.getGlpAmountToDeliver();

            // Verificar si necesitamos recargar GLP antes de atender este pedido
            if (workingVehicle.getCurrentGlpM3() < glpAmount) {
                // Si estamos lejos del depósito, primero conducimos allí
                if (!currentPosition.equals(mainDepot.getPosition())) {
                    // Calcular ruta hacia el depósito
                    Position depotPosition = mainDepot.getPosition();
                    double distanceToDepot = currentPosition.distanceTo(depotPosition);
                    Duration travelTime = Duration.ofMinutes((long) (distanceToDepot * 2)); // 2 min por km

                    // Crear path simplificado (solo origen y destino)
                    List<Position> path = new ArrayList<>();
                    path.add(currentPosition);
                    path.add(depotPosition);

                    // Estimar consumo de combustible
                    double fuelConsumed = workingVehicle.calculateFuelNeeded(distanceToDepot);

                    // Crear acción de conducción
                    DriveAction driveToDepotAction = new DriveAction(
                            currentTime,
                            currentTime.plus(travelTime),
                            workingVehicle.clone(),
                            path,
                            mainDepot,
                            fuelConsumed);

                    actions.add(driveToDepotAction);

                    // Actualizar estado
                    totalDistance += distanceToDepot;
                    currentTime = currentTime.plus(travelTime);
                    currentPosition = depotPosition;
                    workingVehicle.setCurrentPosition(depotPosition);
                    workingVehicle.consumeFuel(distanceToDepot);
                }

                // Recargar GLP
                int glpNeeded = Math.min(
                        workingVehicle.getGlpCapacityM3() - workingVehicle.getCurrentGlpM3(),
                        mainDepot.getCurrentGlpM3());

                // Crear acción de recarga
                Duration reloadTime = Duration.ofMinutes(Constants.DEPOT_GLP_TRANSFER_TIME_MINUTES);
                ReloadAction reloadAction = new ReloadAction(
                        currentTime,
                        currentTime.plus(reloadTime),
                        workingVehicle.clone(),
                        mainDepot,
                        glpNeeded);

                actions.add(reloadAction);

                // Actualizar estado
                currentTime = currentTime.plus(reloadTime);
                workingVehicle.refill(glpNeeded);
            }

            // Conducir al cliente
            Position orderPosition = order.getPosition();
            double distanceToCustomer = currentPosition.distanceTo(orderPosition);
            Duration travelTimeToCustomer = Duration.ofMinutes((long) (distanceToCustomer * 2));

            // Crear path simplificado (solo origen y destino)
            List<Position> pathToCustomer = new ArrayList<>();
            pathToCustomer.add(currentPosition);
            pathToCustomer.add(orderPosition);

            // Estimar consumo de combustible
            double fuelConsumedToCustomer = workingVehicle.calculateFuelNeeded(distanceToCustomer);

            // Crear acción de conducción
            DriveAction driveToCustomerAction = new DriveAction(
                    currentTime,
                    currentTime.plus(travelTimeToCustomer),
                    workingVehicle.clone(),
                    pathToCustomer,
                    order,
                    fuelConsumedToCustomer);
            actions.add(driveToCustomerAction);

            // Actualizar estado
            totalDistance += distanceToCustomer;
            currentTime = currentTime.plus(travelTimeToCustomer);
            currentPosition = orderPosition;
            workingVehicle.setCurrentPosition(orderPosition);
            workingVehicle.consumeFuel(distanceToCustomer);

            // Servir al cliente
            Duration serveTime = Duration.ofMinutes(Constants.GLP_SERVE_DURATION_MINUTES);
            ServeAction serveAction = new ServeAction(
                    currentTime,
                    currentTime.plus(serveTime),
                    workingVehicle.clone(),
                    order,
                    glpAmount);
            actions.add(serveAction);

            // Actualizar estado
            currentTime = currentTime.plus(serveTime);
            workingVehicle.dispenseGlp(glpAmount);
        }

        // Si terminamos lejos del depósito, agregamos una acción final para volver
        if (!currentPosition.equals(mainDepot.getPosition())) {
            Position depotPosition = mainDepot.getPosition();
            double distanceToDepot = currentPosition.distanceTo(depotPosition);
            Duration travelTime = Duration.ofMinutes((long) (distanceToDepot * 2));

            // Crear path simplificado
            List<Position> pathBack = new ArrayList<>();
            pathBack.add(currentPosition);
            pathBack.add(depotPosition);

            // Estimar consumo de combustible
            double fuelConsumedBack = workingVehicle.calculateFuelNeeded(distanceToDepot);

            // Crear acción de retorno
            DriveAction returnToDepotAction = new DriveAction(
                    currentTime,
                    currentTime.plus(travelTime),
                    workingVehicle.clone(),
                    pathBack,
                    mainDepot,
                    fuelConsumedBack);
            actions.add(returnToDepotAction);

            // Actualizar estado
            totalDistance += distanceToDepot;
            currentTime = currentTime.plus(travelTime);
        }

        // Calcular costo total aproximado
        double costPerKm = 2.0;
        estimatedCost = totalDistance * costPerKm;

        // Tiempo total del plan
        Duration totalDuration = Duration.between(environment.getCurrentTime(), currentTime);

        // Crear el plan con todas las acciones
        return new VehiclePlan(
                vehicle,
                actions,
                environment.getCurrentTime(),
                true, // Asumimos que es factible si llegamos hasta aquí
                estimatedCost,
                totalDistance,
                totalDuration);
    }

    /**
     * Aplica un movimiento de tipo TRANSFER que mueve una entrega de un vehículo a
     * otro.
     * 
     * @param solution    La solución original
     * @param environment El entorno actual para evaluar restricciones
     * @return Una nueva solución con el movimiento aplicado, o null si no se puede
     *         aplicar
     */
    private static Solution applyTransferMove(Solution solution, Environment environment) {
        Map<Vehicle, VehiclePlan> originalPlans = solution.getVehiclePlans();
        if (originalPlans.size() < 2) {
            return null; // No se puede hacer transferencias con menos de 2 vehículos
        }

        // Convertir planes a instrucciones de entrega
        Map<Vehicle, List<DeliveryInstruction>> instructionsMap = extractDeliveryInstructions(originalPlans);

        // Seleccionar aleatoriamente un vehículo de origen con al menos una instrucción
        Vehicle sourceVehicle = null;
        List<DeliveryInstruction> sourceInstructions = null;

        List<Vehicle> vehiclesWithOrders = new ArrayList<>();
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : instructionsMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                vehiclesWithOrders.add(entry.getKey());
            }
        }

        if (vehiclesWithOrders.isEmpty()) {
            return null; // No hay vehículos con instrucciones para transferir
        }

        sourceVehicle = vehiclesWithOrders.get(random.nextInt(vehiclesWithOrders.size()));
        sourceInstructions = instructionsMap.get(sourceVehicle);

        if (sourceInstructions.isEmpty()) {
            return null; // No hay instrucciones para transferir
        }

        // Seleccionar una instrucción al azar para transferir
        int sourceIndex = random.nextInt(sourceInstructions.size());
        DeliveryInstruction instructionToTransfer = sourceInstructions.get(sourceIndex);

        // Seleccionar un vehículo destino que no sea el de origen
        List<Vehicle> possibleDestinations = new ArrayList<>(originalPlans.keySet());
        possibleDestinations.remove(sourceVehicle);

        if (possibleDestinations.isEmpty()) {
            return null; // No hay otros vehículos disponibles
        }

        Vehicle destinationVehicle = possibleDestinations.get(random.nextInt(possibleDestinations.size()));

        // Verificar si el vehículo destino puede aceptar esta instrucción (capacidad
        // GLP)
        if (destinationVehicle.getGlpCapacityM3() < instructionToTransfer.getGlpAmountToDeliver()) {
            return null; // El vehículo destino no tiene capacidad suficiente
        }

        // Realizar el movimiento: quitar del origen y añadir al destino
        List<DeliveryInstruction> newSourceInstructions = new ArrayList<>(sourceInstructions);
        newSourceInstructions.remove(sourceIndex);

        List<DeliveryInstruction> destInstructions = instructionsMap.getOrDefault(destinationVehicle,
                new ArrayList<>());
        List<DeliveryInstruction> newDestInstructions = new ArrayList<>(destInstructions);
        newDestInstructions.add(instructionToTransfer);

        // Actualizar el mapa de instrucciones
        instructionsMap.put(sourceVehicle, newSourceInstructions);
        instructionsMap.put(destinationVehicle, newDestInstructions);

        // Regenerar los planes de vehículo con las nuevas instrucciones
        Map<Vehicle, VehiclePlan> newPlans = generatePlansFromInstructions(instructionsMap, environment);

        // Crear la nueva solución con las mismas órdenes no asignadas
        return new Solution(newPlans, solution.getUnassignedOrders(), 0.0);
    }

    /**
     * Extrae las instrucciones de entrega de los planes de vehículo.
     * Método auxiliar para los movimientos.
     */
    private static Map<Vehicle, List<DeliveryInstruction>> extractDeliveryInstructions(
            Map<Vehicle, VehiclePlan> vehiclePlans) {
        Map<Vehicle, List<DeliveryInstruction>> instructionsMap = new HashMap<>();

        for (Map.Entry<Vehicle, VehiclePlan> entry : vehiclePlans.entrySet()) {
            Vehicle vehicle = entry.getKey();
            VehiclePlan plan = entry.getValue();
            List<DeliveryInstruction> instructions = new ArrayList<>();

            // Extraer las instrucciones de entrega de las acciones de servicio
            for (Action action : plan.getActions()) {
                if (action instanceof ServeAction) {
                    ServeAction serveAction = (ServeAction) action;
                    Order order = serveAction.getOrder();
                    int glpAmount = serveAction.getGlpDischargedM3();

                    DeliveryInstruction instruction = new DeliveryInstruction(order, glpAmount);
                    instructions.add(instruction);
                }
            }

            instructionsMap.put(vehicle, instructions);
        }

        return instructionsMap;
    }

    /**
     * Genera nuevos planes de vehículo a partir de un mapa de instrucciones.
     * Método auxiliar para los movimientos.
     */
    private static Map<Vehicle, VehiclePlan> generatePlansFromInstructions(
            Map<Vehicle, List<DeliveryInstruction>> instructionsMap, Environment environment) {
        Map<Vehicle, VehiclePlan> newPlans = new HashMap<>();

        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : instructionsMap.entrySet()) {
            Vehicle vehicle = entry.getKey();
            List<DeliveryInstruction> instructions = entry.getValue();

            if (!instructions.isEmpty()) {
                VehiclePlan plan = buildVehiclePlan(vehicle, instructions, environment);
                newPlans.put(vehicle, plan);
            }
        }

        return newPlans;
    }

    /**
     * Aplica un movimiento de tipo SWAP que intercambia entregas entre dos
     * vehículos.
     * 
     * @param solution    La solución original
     * @param environment El entorno actual para evaluar restricciones
     * @return Una nueva solución con el movimiento aplicado, o null si no se puede
     *         aplicar
     */
    private static Solution applySwapMove(Solution solution, Environment environment) {
        Map<Vehicle, VehiclePlan> originalPlans = solution.getVehiclePlans();
        if (originalPlans.size() < 2) {
            return null; // No se puede hacer intercambios con menos de 2 vehículos
        }

        // Convertir planes a instrucciones de entrega
        Map<Vehicle, List<DeliveryInstruction>> instructionsMap = extractDeliveryInstructions(originalPlans);

        // Encontrar dos vehículos que tengan al menos una instrucción cada uno
        List<Vehicle> vehiclesWithOrders = new ArrayList<>();
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : instructionsMap.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                vehiclesWithOrders.add(entry.getKey());
            }
        }

        if (vehiclesWithOrders.size() < 2) {
            return null; // No tenemos suficientes vehículos con órdenes para hacer un intercambio
        }

        // Seleccionar dos vehículos aleatorios distintos
        int firstIndex = random.nextInt(vehiclesWithOrders.size());
        int secondIndex;
        do {
            secondIndex = random.nextInt(vehiclesWithOrders.size());
        } while (firstIndex == secondIndex);

        Vehicle firstVehicle = vehiclesWithOrders.get(firstIndex);
        Vehicle secondVehicle = vehiclesWithOrders.get(secondIndex);

        List<DeliveryInstruction> firstInstructions = instructionsMap.get(firstVehicle);
        List<DeliveryInstruction> secondInstructions = instructionsMap.get(secondVehicle);

        if (firstInstructions.isEmpty() || secondInstructions.isEmpty()) {
            return null; // Por alguna razón no hay instrucciones (debería ser imposible por la
                         // verificación anterior)
        }

        // Seleccionar una instrucción aleatoria de cada vehículo
        int firstInstrIndex = random.nextInt(firstInstructions.size());
        int secondInstrIndex = random.nextInt(secondInstructions.size());

        DeliveryInstruction firstInstruction = firstInstructions.get(firstInstrIndex);
        DeliveryInstruction secondInstruction = secondInstructions.get(secondInstrIndex);

        // Verificar si los vehículos pueden manejar las instrucciones intercambiadas
        // (capacidad GLP)
        if (firstVehicle.getGlpCapacityM3() < secondInstruction.getGlpAmountToDeliver() ||
                secondVehicle.getGlpCapacityM3() < firstInstruction.getGlpAmountToDeliver()) {
            return null; // Algún vehículo no tiene capacidad suficiente para la instrucción
                         // intercambiada
        }

        // Realizar el intercambio
        List<DeliveryInstruction> newFirstInstructions = new ArrayList<>(firstInstructions);
        newFirstInstructions.set(firstInstrIndex, secondInstruction);

        List<DeliveryInstruction> newSecondInstructions = new ArrayList<>(secondInstructions);
        newSecondInstructions.set(secondInstrIndex, firstInstruction);

        // Actualizar el mapa de instrucciones
        instructionsMap.put(firstVehicle, newFirstInstructions);
        instructionsMap.put(secondVehicle, newSecondInstructions);

        // Regenerar los planes de vehículo con las nuevas instrucciones
        Map<Vehicle, VehiclePlan> newPlans = generatePlansFromInstructions(instructionsMap, environment);

        // Devolver la nueva solución con las mismas órdenes no asignadas
        return new Solution(newPlans, solution.getUnassignedOrders(), 0.0);
    }

    /**
     * Aplica un movimiento de tipo REORDER que cambia el orden de las entregas en
     * un vehículo.
     * 
     * @param solution    La solución original
     * @param environment El entorno actual para evaluar restricciones
     * @return Una nueva solución con el movimiento aplicado, o null si no se puede
     *         aplicar
     */
    private static Solution applyReorderMove(Solution solution, Environment environment) {
        Map<Vehicle, VehiclePlan> originalPlans = solution.getVehiclePlans();
        if (originalPlans.isEmpty()) {
            return null; // No hay planes para reordenar
        }

        // Convertir planes a instrucciones de entrega
        Map<Vehicle, List<DeliveryInstruction>> instructionsMap = extractDeliveryInstructions(originalPlans);

        // Encontrar vehículos que tengan al menos 2 instrucciones (para poder
        // reordenar)
        List<Vehicle> vehiclesWithMultipleOrders = new ArrayList<>();
        for (Map.Entry<Vehicle, List<DeliveryInstruction>> entry : instructionsMap.entrySet()) {
            if (entry.getValue().size() >= 2) {
                vehiclesWithMultipleOrders.add(entry.getKey());
            }
        }

        if (vehiclesWithMultipleOrders.isEmpty()) {
            return null; // No hay vehículos con suficientes instrucciones para reordenar
        }

        // Seleccionar un vehículo aleatorio para reordenar sus instrucciones
        Vehicle targetVehicle = vehiclesWithMultipleOrders.get(random.nextInt(vehiclesWithMultipleOrders.size()));
        List<DeliveryInstruction> instructions = instructionsMap.get(targetVehicle);

        // Seleccionar dos posiciones aleatorias distintas para intercambiar
        int size = instructions.size();
        int firstPos = random.nextInt(size);
        int secondPos;
        do {
            secondPos = random.nextInt(size);
        } while (firstPos == secondPos);

        // Reordenar las instrucciones
        List<DeliveryInstruction> newInstructions = new ArrayList<>(instructions);
        DeliveryInstruction temp = newInstructions.get(firstPos);
        newInstructions.set(firstPos, newInstructions.get(secondPos));
        newInstructions.set(secondPos, temp);

        // Actualizar el mapa de instrucciones
        instructionsMap.put(targetVehicle, newInstructions);

        // Regenerar los planes de vehículo con las nuevas instrucciones
        Map<Vehicle, VehiclePlan> newPlans = generatePlansFromInstructions(instructionsMap, environment);

        // Devolver la nueva solución con las mismas órdenes no asignadas
        return new Solution(newPlans, solution.getUnassignedOrders(), 0.0);
    }

    /**
     * Selecciona un tipo de movimiento aleatorio.
     */
    private static MoveType getRandomMoveType() {
        MoveType[] moveTypes = MoveType.values();
        return moveTypes[random.nextInt(moveTypes.length)];
    }
}