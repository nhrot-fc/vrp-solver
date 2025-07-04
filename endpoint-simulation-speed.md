# Endpoint de Control de Velocidad de Simulación

## Nuevo Endpoint Agregado: `/simulation/speed`

Se ha agregado un nuevo endpoint para controlar la velocidad de la simulación, permitiendo obtener y modificar el tiempo entre ticks de simulación.

### 📋 **Endpoints Disponibles:**

#### 1. **GET /simulation/speed** - Obtener Velocidad Actual
Obtiene la velocidad actual de la simulación.

**URL:** `http://localhost:8080/simulation/speed`

**Método:** `GET`

**Respuesta Exitosa:**
```json
{
  "status": "success",
  "currentSpeed": 500,
  "unit": "milliseconds",
  "description": "Time between simulation ticks",
  "simulationRunning": true,
  "timestamp": "2025-07-03 14:30:00"
}
```

#### 2. **POST /simulation/speed** - Configurar Velocidad
Configura una nueva velocidad de simulación.

**URL:** `http://localhost:8080/simulation/speed`

**Método:** `POST`

**Content-Type:** `application/json`

**Body JSON:**
```json
{
  "speed": 1000
}
```

**Parámetros:**
- `speed` (requerido): Tiempo en milisegundos entre ticks de simulación
  - **Mínimo**: 50ms (para evitar sobrecarga de CPU)
  - **Máximo**: 10000ms (10 segundos)

**Respuesta Exitosa:**
```json
{
  "status": "success",
  "message": "Simulation speed updated successfully",
  "oldSpeed": 500,
  "newSpeed": 1000,
  "unit": "milliseconds",
  "timestamp": "2025-07-03 14:30:00"
}
```

### 🧪 **Ejemplos de Uso**

#### Ejemplo 1: Obtener velocidad actual con cURL
```bash
curl -X GET http://localhost:8080/simulation/speed
```

#### Ejemplo 2: Configurar velocidad más rápida (200ms)
```bash
curl -X POST http://localhost:8080/simulation/speed \
  -H "Content-Type: application/json" \
  -d '{"speed": 200}'
```

#### Ejemplo 3: Configurar velocidad más lenta (2000ms)
```bash
curl -X POST http://localhost:8080/simulation/speed \
  -H "Content-Type: application/json" \
  -d '{"speed": 2000}'
```

#### Ejemplo 4: Con JavaScript (fetch)
```javascript
// Obtener velocidad actual
fetch('http://localhost:8080/simulation/speed')
  .then(response => response.json())
  .then(data => console.log('Current speed:', data.currentSpeed + 'ms'));

// Configurar nueva velocidad
fetch('http://localhost:8080/simulation/speed', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ speed: 750 })
})
.then(response => response.json())
.then(data => console.log('Speed updated:', data));
```

### 🛡️ **Validaciones y Errores**

#### Errores Comunes:

**1. Valor de velocidad faltante:**
```json
{
  "status": "error",
  "message": "Speed value is required"
}
```

**2. Valor no numérico:**
```json
{
  "status": "error",
  "message": "Speed must be a valid integer"
}
```

**3. Velocidad demasiado baja:**
```json
{
  "status": "error",
  "message": "Speed must be at least 50 milliseconds"
}
```

**4. Velocidad demasiado alta:**
```json
{
  "status": "error",
  "message": "Speed must not exceed 10000 milliseconds"
}
```

### 🔧 **Funcionalidad Técnica**

#### **Proceso de Obtención:**
1. Consulta la velocidad actual del `ApiServiceLauncher`
2. Verifica el estado de ejecución de la simulación
3. Retorna la información en formato JSON

#### **Proceso de Configuración:**
1. Valida que el valor esté presente y sea numérico
2. Verifica que esté dentro del rango permitido (50-10000ms)
3. Actualiza la velocidad en el `ApiServiceLauncher`
4. Si la simulación está ejecutándose, reinicia el hilo con la nueva velocidad
5. Retorna confirmación con velocidades anterior y nueva

### ⚡ **Efectos de la Velocidad**

- **Velocidades Bajas (50-200ms)**: 
  - Simulación muy rápida
  - Mayor uso de CPU
  - Útil para pruebas rápidas

- **Velocidades Medias (500-1000ms)**:
  - Simulación balanceada
  - Uso moderado de CPU
  - Ideal para observación normal

- **Velocidades Altas (2000-10000ms)**:
  - Simulación lenta y pausada
  - Bajo uso de CPU
  - Útil para análisis detallado

### 🎯 **Casos de Uso**

1. **Acelerar pruebas**: Reducir velocidad para completar simulaciones rápidamente
2. **Análisis detallado**: Aumentar velocidad para observar cambios paso a paso
3. **Optimización de recursos**: Ajustar según capacidad del sistema
4. **Debugging**: Velocidad lenta para depurar problemas específicos
5. **Presentaciones**: Velocidad media para demostraciones

### 🔄 **Integración con Otros Endpoints**

- **Compatible con `/simulation/start` y `/simulation/pause`**
- **Se refleja en `/simulation/status`**
- **Efecto inmediato en simulación en ejecución**
- **Persiste durante toda la sesión**

### 📊 **Valores Recomendados**

- **Desarrollo/Testing**: 100-200ms
- **Demostración**: 500-750ms  
- **Análisis**: 1000-2000ms
- **Debugging**: 2000-5000ms
- **Pausa efectiva**: 8000-10000ms

El endpoint está completamente integrado con el sistema existente y permite control dinámico de la velocidad de simulación sin necesidad de reiniciar el servicio.
