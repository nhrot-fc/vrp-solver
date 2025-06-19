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
MAIN_FILE = com/vroute/Main.java
MAIN_CLASS = com.vroute.Main
BLOQUEO_DEMO_FILE = com/vroute/ui/BloqueoDemo.java
BLOQUEO_DEMO_CLASS = com.vroute.ui.BloqueoDemo

# Regla por defecto
all: compile

# Regla para compilar
compile:
	@echo "Creando directorio de salida: $(BIN_DIR)..."
	@mkdir -p $(BIN_DIR)
	@echo "Compilando con JavaFX..."
	$(JC) --release $(JAVA_VERSION) -d $(BIN_DIR) -cp $(JAVAFX_JARS) -sourcepath $(SRC_ROOT) $(SRC_ROOT)/$(MAIN_FILE)
	@echo "Compilación finalizada."

# Regla para ejecutar la aplicación principal
run: compile
	@echo "Ejecutando $(MAIN_CLASS) con JavaFX..."
	java --module-path $(LIB_DIR) --add-modules javafx.controls,javafx.fxml -cp $(BIN_DIR):$(JAVAFX_JARS) $(MAIN_CLASS)

# Regla para ejecutar la demo de bloqueos reales
run-bloqueos:
	@echo "Creando directorio de salida: $(BIN_DIR)..."
	@mkdir -p $(BIN_DIR)
	@echo "Compilando la demo de bloqueos y el módulo de pathfinding..."
	$(JC) --release $(JAVA_VERSION) -d $(BIN_DIR) -sourcepath $(SRC_ROOT) $(SRC_ROOT)/$(BLOQUEO_DEMO_FILE)
	@echo "Ejecutando $(BLOQUEO_DEMO_CLASS)..."
	java -cp $(BIN_DIR) $(BLOQUEO_DEMO_CLASS)

# Regla para limpiar los archivos generados
clean:
	@echo "Limpiando directorio de salida: $(BIN_DIR)..."
	@rm -rf $(BIN_DIR)
	@echo "Limpieza finalizada."

.PHONY: all compile run run-simulation run-api run-api-port run-visualizer run-bloqueos run-bloqueos-debug clean