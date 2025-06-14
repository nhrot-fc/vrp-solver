# Nombre del compilador de Java
JC = javac
JAVA_VERSION = 21

# Directorio raíz donde se encuentran los paquetes de fuentes
SRC_ROOT = src/main/java

# Directorio donde se guardarán los archivos .class compilados
BIN_DIR = out

# Directorio de bibliotecas JavaFX
LIB_DIR = lib

# Obtener todas las JARs de JavaFX para el classpath
JAVAFX_JARS = $(shell find $(LIB_DIR) -name '*.jar' | tr '\n' ':')

# Nombre completo de la clase principal (con paquete)
MAIN_CLASS = com.vroute.Main
SIMULATION_CLASS = com.vroute.SimulationLauncher
ROUTING_CLASS = com.vroute.RoutingApplication
API_SERVICE_CLASS = com.vroute.api.ApiServiceLauncher

# Regla por defecto
all: compile

# Regla para compilar
compile:
	@echo "Creando directorio de salida: $(BIN_DIR)..."
	@mkdir -p $(BIN_DIR)
	@echo "Compilando con JavaFX..."
	$(JC) --release $(JAVA_VERSION) -d $(BIN_DIR) -cp $(JAVAFX_JARS) -sourcepath $(SRC_ROOT) $(SRC_ROOT)/com/vroute/Main.java $(SRC_ROOT)/com/vroute/SimulationLauncher.java $(SRC_ROOT)/com/vroute/api/ApiServiceLauncher.java
	@echo "Compilación finalizada."

# Regla para ejecutar la aplicación principal
run: compile
	@echo "Ejecutando $(MAIN_CLASS) con JavaFX..."
	java --module-path $(LIB_DIR) --add-modules javafx.controls,javafx.fxml -cp $(BIN_DIR):$(JAVAFX_JARS) $(MAIN_CLASS)

# Regla para ejecutar el simulador con visualización
run-simulation: compile
	@echo "Ejecutando $(SIMULATION_CLASS) con JavaFX..."
	java --module-path $(LIB_DIR) --add-modules javafx.controls,javafx.fxml -cp $(BIN_DIR):$(JAVAFX_JARS) $(SIMULATION_CLASS)

# Regla para ejecutar el servicio API
run-api: compile
	@echo "Ejecutando $(API_SERVICE_CLASS) - Servidor HTTP API..."
	java -cp $(BIN_DIR) $(API_SERVICE_CLASS)

# Regla para ejecutar el servicio API en un puerto específico
run-api-port: compile
	@echo "Ejecutando $(API_SERVICE_CLASS) en puerto $(PORT)..."
	java -cp $(BIN_DIR) $(API_SERVICE_CLASS) $(PORT)

# Regla para limpiar los archivos generados
clean:
	@echo "Limpiando directorio de salida: $(BIN_DIR)..."
	@rm -rf $(BIN_DIR)
	@echo "Limpieza finalizada."

.PHONY: all compile run run-simulation run-api run-api-port clean