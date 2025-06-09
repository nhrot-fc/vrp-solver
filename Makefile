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

# Regla por defecto
all: compile

# Regla para compilar
compile:
	@echo "Creando directorio de salida: $(BIN_DIR)..."
	@mkdir -p $(BIN_DIR)
	@echo "Compilando con JavaFX..."
	$(JC) --release $(JAVA_VERSION) -d $(BIN_DIR) -cp $(JAVAFX_JARS) -sourcepath $(SRC_ROOT) $(SRC_ROOT)/com/vroute/Main.java
	@echo "Compilación finalizada."

# Regla para ejecutar
run: compile
	@echo "Ejecutando $(MAIN_CLASS) con JavaFX..."
	java --module-path $(LIB_DIR) --add-modules javafx.controls,javafx.fxml -cp $(BIN_DIR):$(JAVAFX_JARS) $(MAIN_CLASS)

# Regla para limpiar los archivos generados
clean:
	@echo "Limpiando directorio de salida: $(BIN_DIR)..."
	@rm -rf $(BIN_DIR)
	@echo "Limpieza finalizada."

.PHONY: all compile run clean