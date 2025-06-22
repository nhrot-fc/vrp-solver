# Nombre del compilador de Java
JC = javac
JAVA = java
JAVA_VERSION = 21

# Directorio raíz donde se encuentran los paquetes de fuentes
SRC_ROOT = src/main/java

# Directorio donde se guardarán los archivos .class compilados
BIN_DIR = out

# Nombre completo de la clase principal (con paquete)
MAIN_CLASS = com.vroute.Main

# Opciones del compilador
JFLAGS = -d $(BIN_DIR) -sourcepath $(SRC_ROOT)

# Archivos fuente
SOURCES := $(shell find $(SRC_ROOT) -name "*.java")

# Comando default (compile + run)
all: compile run

# Regla para compilar el proyecto
compile: 
	@echo "Creando directorio de salida si no existe..."
	@mkdir -p $(BIN_DIR)
	@echo "Compilando archivos Java..."
	@$(JC) $(JFLAGS) $(SOURCES)
	@echo "Compilación finalizada."

# Regla para ejecutar la aplicación
run:
	@echo "Ejecutando $(MAIN_CLASS)..."
	@$(JAVA) -cp $(BIN_DIR) $(MAIN_CLASS)
	@echo "Ejecución finalizada."

# Regla para limpiar los archivos generados
clean:
	@echo "Limpiando directorio de salida: $(BIN_DIR)..."
	@rm -rf $(BIN_DIR)
	@echo "Limpieza finalizada."

.PHONY: all clean compile run