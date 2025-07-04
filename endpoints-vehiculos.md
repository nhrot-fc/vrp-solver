# Endpoints de Gestión de Vehículos - Averías y Reparaciones

## Nuevos Endpoints Agregados

Se han agregado dos nuevos endpoints para simular averías y reparaciones de vehículos en tiempo real durante la simulación.

### 📋 **Endpoints Disponibles:**

#### 1. **POST /vehicle/breakdown** - Averiar Vehículo
Marca un vehículo como averiado y lo saca de servicio.

**URL:** `http://localhost:8080/vehicle/breakdown`

**Método:** `POST`

**Content-Type:** `application/json`

**Body JSON:**
```json
{
  "vehicleId": "TA01",
  "reason": "Engine failure", 
  "estimatedRepairHours": 4
}
```

**Parámetros:**
- `vehicleId` (requerido): ID del vehículo a averiar (ej: "TA01", "TB02", "TC01", "TD05")
- `reason` (opcional): Razón de la avería (default: "Mechanical failure")
- `estimatedRepairHours` (opcional): Horas estimadas para la reparación (default: 2)

**Respuesta Exitosa:**
```json
{
  "status": "success",
  "message": "Vehicle TA01 has been marked as broken down",
  "vehicleId": "TA01",
  "reason": "Engine failure",
  "incidentType": "TI2",
  "breakdownTime": "2025-01-01 10:30:00",
  "estimatedRepairTime": "2025-01-01 18:30:00",
  "vehicleStatus": "UNAVAILABLE"
}
```

**Tipos de Incidente Automáticos:**
- **TI1**: Para reparaciones ≤ 2 horas (reparable en sitio)
- **TI2**: Para reparaciones 3-24 horas (requiere ir al depósito)
- **TI3**: Para reparaciones > 24 horas (incidente grave)

#### 2. **POST /vehicle/repair** - Reparar Vehículo
Repara un vehículo averiado y lo vuelve a poner en servicio.

**URL:** `http://localhost:8080/vehicle/repair`

**Método:** `POST`

**Content-Type:** `application/json`

**Body JSON:**
```json
{
  "vehicleId": "TA01"
}
```

**Parámetros:**
- `vehicleId` (requerido): ID del vehículo a reparar

**Respuesta Exitosa:**
```json
{
  "status": "success",
  "message": "Vehicle TA01 has been repaired and is now available",
  "vehicleId": "TA01",
  "repairTime": "2025-01-01 14:45:00",
  "resolvedIncidents": 1,
  "vehicleStatus": "AVAILABLE"
}
```

### 🧪 **Ejemplos de Uso**

#### Ejemplo 1: Averiar un vehículo con cURL
```bash
curl -X POST http://localhost:8080/vehicle/breakdown \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": "TA01",
    "reason": "Flat tire",
    "estimatedRepairHours": 2
  }'
```

#### Ejemplo 2: Reparar un vehículo con cURL
```bash
curl -X POST http://localhost:8080/vehicle/repair \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": "TA01"
  }'
```

#### Ejemplo 3: Averiar vehículo con JavaScript (fetch)
```javascript
fetch('http://localhost:8080/vehicle/breakdown', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    vehicleId: 'TB02',
    reason: 'Engine overheating',
    estimatedRepairHours: 6
  })
})
.then(response => response.json())
.then(data => console.log(data));
```

### 🛡️ **Validaciones y Errores**

#### Errores Comunes:

**1. Vehículo no encontrado:**
```json
{
  "status": "error",
  "message": "Vehicle XYZ not found"
}
```

**2. Vehículo ya averiado:**
```json
{
  "status": "error",
  "message": "Vehicle TA01 is already unavailable"
}
```

**3. Intentar reparar vehículo disponible:**
```json
{
  "status": "error",
  "message": "Vehicle TA01 is not broken down (status: AVAILABLE)"
}
```

**4. ID de vehículo faltante:**
```json
{
  "status": "error",
  "message": "Vehicle ID is required"
}
```

### 🔧 **Funcionalidad Técnica**

#### **Proceso de Avería:**
1. Valida que el vehículo existe y está disponible
2. Determina el tipo de incidente según horas estimadas
3. Crea un `Incident` object con el tipo apropiado
4. Establece tiempo de ocurrencia y ubicación actual
5. Cambia estado del vehículo a `UNAVAILABLE`
6. Agrega el incidente al entorno

#### **Proceso de Reparación:**
1. Valida que el vehículo existe y está averiado
2. Busca incidentes activos para el vehículo
3. Marca todos los incidentes como resueltos
4. Cambia estado del vehículo a `AVAILABLE`
5. El vehículo queda disponible para nueva asignación

### 📊 **Integración con el Sistema**

- **Planificación**: Los vehículos averiados son excluidos automáticamente de nuevas asignaciones
- **Visualización**: El estado se refleja en el endpoint `/environment` y `/vehicles`
- **Logs**: Las averías y reparaciones se registran en los logs del sistema
- **Replanificación**: El sistema puede replanificar rutas cuando hay vehículos no disponibles

### 🚛 **IDs de Vehículos Disponibles**

Según la configuración del sistema:
- **TA**: TA01, TA02, TA03 (3 unidades)
- **TB**: TB01, TB02, TB03, TB04 (4 unidades)  
- **TC**: TC01, TC02, TC03 (3 unidades)
- **TD**: TD01, TD02, TD03, TD04, TD05, TD06, TD07, TD08, TD09, TD10 (10 unidades)

### 🎯 **Casos de Uso**

1. **Simulación de averías aleatorias** durante operaciones
2. **Testing de robustez** del sistema de planificación
3. **Evaluación del impacto** de vehículos no disponibles
4. **Simulación de mantenimiento** no programado
5. **Testing de replanificación** automática

Estos endpoints permiten simular escenarios realistas donde los vehículos pueden fallar durante las operaciones y posteriormente ser reparados y vueltos al servicio.
