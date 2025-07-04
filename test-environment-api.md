# Prueba del Endpoint `/environment` - Solo Estado Actual

## Cambios Realizados

He modificado el método `getEnvironmentSnapshot()` en `StatusController.java` para que funcione de manera similar al `SwingMapRenderer`, mostrando únicamente el estado actual del entorno en lugar de toda la información completa de los planes.

## Características Implementadas

### 1. Vehículos
- **Estado actual**: Solo posición actual, no todo el plan de rutas
- **Path actual**: Solo el path que está siguiendo actualmente (acción DRIVE en curso)
- **Información detallada**: 
  - Combustible actual vs capacidad
  - GLP actual vs capacidad
  - Porcentajes de utilización

### 2. Órdenes
- **Solo órdenes activas**: Filtra órdenes ya entregadas
- **Estado actual**: Si está vencida basado en el tiempo actual de simulación
- **Información de GLP**: Solicitado vs restante

### 3. Bloqueos
- **Solo bloqueos activos**: Usa `getActiveBlockagesAt(currentTime)` en lugar de todos los bloqueos
- **ID generado**: Crea un ID único basado en la fecha/hora de inicio

### 4. Depósitos
- **Estado actual**: GLP actual vs capacidad
- **Capacidad de reabastecimiento**: Indica si puede reabastecer combustible

## Estructura JSON del Endpoint

```json
{
  "timestamp": "2025-07-03 14:30:00",
  "simulationTime": "2025-01-01 08:30:00",
  "simulationRunning": true,
  "vehicles": [
    {
      "id": "TA01",
      "type": "TA",
      "status": "DRIVING",
      "position": {"x": 25, "y": 30},
      "fuel": {
        "current": 85.5,
        "capacity": 100.0,
        "percentage": 85.5
      },
      "glp": {
        "current": 8,
        "capacity": 10,
        "percentage": 80.0
      },
      "currentPath": {
        "actionType": "DRIVE",
        "startTime": "2025-01-01 08:25:00",
        "endTime": "2025-01-01 08:45:00",
        "path": [
          {"x": 20, "y": 25},
          {"x": 22, "y": 27},
          {"x": 25, "y": 30}
        ]
      }
    }
  ],
  "orders": [
    {
      "id": "ORD001",
      "position": {"x": 45, "y": 50},
      "arriveTime": "2025-01-01 08:00:00",
      "dueTime": "2025-01-01 10:00:00",
      "isOverdue": false,
      "glp": {
        "requested": 5,
        "remaining": 5
      }
    }
  ],
  "blockages": [
    {
      "id": "BLOCKAGE_01010830",
      "startTime": "2025-01-01 08:30:00",
      "endTime": "2025-01-01 12:00:00",
      "positions": [
        {"x": 10, "y": 15},
        {"x": 12, "y": 15},
        {"x": 14, "y": 15}
      ]
    }
  ],
  "depots": [
    {
      "id": "MAIN_PLANT",
      "position": {"x": 0, "y": 0},
      "isMain": true,
      "canRefuel": true,
      "glp": {
        "current": 95000,
        "capacity": 100000
      }
    }
  ]
}
```

## Comparación con la Versión Anterior

### Antes
- Incluía **todo el plan completo** de cada vehículo
- Mostraba **todas las órdenes** (incluidas las entregadas)
- Incluía **todos los bloqueos** (incluso los inactivos)
- Menos información detallada sobre el estado actual

### Ahora
- Solo muestra el **path actual** que está siguiendo cada vehículo
- Solo **órdenes pendientes** (no entregadas)
- Solo **bloqueos activos** en el tiempo actual
- Información más detallada sobre combustible, GLP y capacidades

## Beneficios

1. **Menor transferencia de datos**: Solo información relevante actual
2. **Mejor rendimiento**: Menos procesamiento de datos innecesarios
3. **Más útil para visualización**: Similar al enfoque del SwingMapRenderer
4. **Información más precisa**: Estado actual real vs planes futuros

## Cómo Probar

```bash
# Iniciar el servidor
cd /home/nhrot/Programming/DP1/V-Route
make run-api

# Probar el endpoint (en otra terminal)
curl -X GET http://localhost:8080/environment | jq .

# O abrir en el navegador
http://localhost:8080/environment
```

Los datos mostrados reflejarán exactamente el estado actual de la simulación, igual que como se ve en el mapa visual del SwingMapRenderer.
