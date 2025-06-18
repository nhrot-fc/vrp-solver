package com.vroute.utils;

import com.vroute.models.Bloqueo;
import com.vroute.models.Posicion;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase de utilidad para cargar bloqueos desde archivos de texto
 */
public class BloqueoLoader {

    // Patrón para analizar el formato de tiempo DDdHHhMMm
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m");

    /**
     * Carga bloqueos desde un archivo de texto
     * 
     * @param filePath Path completo al archivo de bloqueos
     * @param year     Año de los bloqueos
     * @param month    Mes de los bloqueos (1-12)
     * @return Lista de objetos Bloqueo
     * @throws IOException              Si hay problemas al leer el archivo
     * @throws IllegalArgumentException Si el formato del archivo es inválido
     */
    public static List<Bloqueo> cargarBloqueos(String filePath, int year, int month) throws IOException {
        List<Bloqueo> bloqueos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String linea;
            int lineaNum = 0;

            while ((linea = reader.readLine()) != null) {
                lineaNum++;
                // Ignorar líneas vacías o comentarios
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }

                try {
                    // Dividir la línea en el tiempo y las coordenadas
                    String[] partes = linea.split(":");
                    if (partes.length != 2) {
                        throw new IllegalArgumentException("Formato inválido en línea " + lineaNum +
                                ": Se esperaba 'TIEMPO:COORDENADAS', se encontró: " + linea);
                    }

                    String tiempos = partes[0].trim();
                    String coordenadas = partes[1].trim();

                    // Dividir los tiempos en inicio y fin
                    String[] rangoTiempo = tiempos.split("-");
                    if (rangoTiempo.length != 2) {
                        throw new IllegalArgumentException("Formato de tiempo inválido en línea " + lineaNum +
                                ": Se esperaba 'INICIO-FIN', se encontró: " + tiempos);
                    }

                    LocalDateTime horaInicio = parsearTiempo(rangoTiempo[0].trim(), year, month);
                    LocalDateTime horaFin = parsearTiempo(rangoTiempo[1].trim(), year, month);

                    // Validar que la hora de fin sea posterior a la de inicio
                    if (!horaFin.isAfter(horaInicio)) {
                        throw new IllegalArgumentException(
                                "La hora de fin debe ser posterior a la hora de inicio en línea " +
                                        lineaNum + ": " + horaInicio + " -> " + horaFin);
                    }

                    // Parsear las coordenadas
                    List<Posicion> tramos = parsearPosiciones(coordenadas);
                    if (tramos.size() < 2) {
                        throw new IllegalArgumentException(
                                "Deben especificarse al menos 2 posiciones para un bloqueo en línea " + lineaNum);
                    }

                    // Crear el objeto Bloqueo y añadirlo a la lista
                    bloqueos.add(new Bloqueo(horaInicio, horaFin, tramos));

                } catch (Exception e) {
                    System.err.println("Error procesando línea " + lineaNum + ": " + e.getMessage());
                }
            }
        }

        return bloqueos;
    }

    /**
     * Parsea una cadena de tiempo en formato DDdHHhMMm a un objeto LocalDateTime
     * Donde DD es el día del mes, HH es la hora (0-23) y MM son los minutos (0-59)
     */
    private static LocalDateTime parsearTiempo(String tiempo, int year, int month) {
        Matcher matcher = TIME_PATTERN.matcher(tiempo);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Formato de tiempo inválido: " + tiempo +
                    ". Debe ser en formato DDdHHhMMm (ejemplo: 01d08h30m)");
        }

        try {
            int dia = Integer.parseInt(matcher.group(1));
            int hora = Integer.parseInt(matcher.group(2));
            int minuto = Integer.parseInt(matcher.group(3));

            // Validar rangos
            if (dia < 1 || dia > 31) {
                throw new IllegalArgumentException("Día inválido: " + dia + ". Debe estar entre 1 y 31");
            }
            if (hora < 0 || hora > 23) {
                throw new IllegalArgumentException("Hora inválida: " + hora + ". Debe estar entre 0 y 23");
            }
            if (minuto < 0 || minuto > 59) {
                throw new IllegalArgumentException("Minuto inválido: " + minuto + ". Debe estar entre 0 y 59");
            }

            return LocalDateTime.of(year, month, dia, hora, minuto, 0);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Error al convertir valores numéricos en: " + tiempo, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al crear fecha para: " + tiempo +
                    ". Año=" + year + ", Mes=" + month + ". " + e.getMessage(), e);
        }
    }

    /**
     * Parsea una cadena de coordenadas separadas por comas a una lista de
     * Posiciones
     */
    private static List<Posicion> parsearPosiciones(String coordenadas) {
        List<Posicion> posiciones = new ArrayList<>();
        String[] valores = coordenadas.split(",");

        if (valores.length % 2 != 0) {
            throw new IllegalArgumentException("Número impar de coordenadas: " + coordenadas);
        }

        for (int i = 0; i < valores.length; i += 2) {
            int x = Integer.parseInt(valores[i].trim());
            int y = Integer.parseInt(valores[i + 1].trim());
            posiciones.add(new Posicion(x, y));
        }

        return posiciones;
    }

    /**
     * Método de utilidad para extraer el mes y año de un nombre de archivo de
     * bloqueo
     * Soporta varios formatos:
     * - "YYYYMM.bloqueos.txt"
     * - "YYYY-MM.bloqueos.txt"
     * - "YYYY_MM.bloqueos.txt"
     * 
     * @param filename Nombre del archivo
     * @return Array con [año, mes]
     */
    public static int[] extraerFechaDeNombreArchivo(String filename) {
        // Intentar con diferentes patrones de formato de fecha

        // Formato: YYYYMM.bloqueos.txt (ejemplo: 202501.bloqueos.txt)
        Pattern pattern1 = Pattern.compile("(\\d{4})(\\d{2})\\.bloqueos\\.txt");
        Matcher matcher1 = pattern1.matcher(filename);
        if (matcher1.find()) {
            int year = Integer.parseInt(matcher1.group(1));
            int month = Integer.parseInt(matcher1.group(2));
            return new int[] { year, month };
        }

        // Formato: YYYY-MM.bloqueos.txt (ejemplo: 2025-01.bloqueos.txt)
        Pattern pattern2 = Pattern.compile("(\\d{4})-(\\d{2})\\.bloqueos\\.txt");
        Matcher matcher2 = pattern2.matcher(filename);
        if (matcher2.find()) {
            int year = Integer.parseInt(matcher2.group(1));
            int month = Integer.parseInt(matcher2.group(2));
            return new int[] { year, month };
        }

        // Formato: YYYY_MM.bloqueos.txt (ejemplo: 2025_01.bloqueos.txt)
        Pattern pattern3 = Pattern.compile("(\\d{4})_(\\d{2})\\.bloqueos\\.txt");
        Matcher matcher3 = pattern3.matcher(filename);
        if (matcher3.find()) {
            int year = Integer.parseInt(matcher3.group(1));
            int month = Integer.parseInt(matcher3.group(2));
            return new int[] { year, month };
        }

        throw new IllegalArgumentException("Formato de nombre de archivo inválido: " + filename +
                "\nDebe tener el formato YYYYMM.bloqueos.txt o YYYY-MM.bloqueos.txt o YYYY_MM.bloqueos.txt");
    }

    /**
     * Método conveniente para cargar bloqueos directamente a partir de un nombre de
     * archivo
     * Extrae automáticamente el año y mes del nombre del archivo
     * 
     * @param filePath Ruta completa al archivo
     * @return Lista de bloqueos
     * @throws IOException              Si hay problemas al leer el archivo
     * @throws IllegalArgumentException Si el nombre del archivo o formato es
     *                                  inválido
     */
    public static List<Bloqueo> cargarBloqueosDesdeArchivo(String filePath) throws IOException {
        // Extraer el nombre del archivo, manejando rutas tanto de Windows como de Unix
        String filename;
        if (filePath.contains("/")) {
            filename = filePath.substring(filePath.lastIndexOf('/') + 1);
        } else if (filePath.contains("\\")) {
            filename = filePath.substring(filePath.lastIndexOf('\\') + 1);
        } else {
            filename = filePath; // El filePath es solo el nombre del archivo
        }

        int[] fecha = extraerFechaDeNombreArchivo(filename);

        return cargarBloqueos(filePath, fecha[0], fecha[1]);
    }

    /**
     * Método para probar el parseado de una línea de bloqueo directamente
     * Este método es útil para validar que el formato se interprete correctamente
     * 
     * @param linea La línea de texto con el formato de bloqueo
     * @param year  El año para el bloqueo
     * @param month El mes para el bloqueo
     * @return El objeto Bloqueo creado a partir de la línea
     * @throws IllegalArgumentException Si el formato de la línea es inválido
     */
    public static Bloqueo probarParseLinea(String linea, int year, int month) {
        try {
            // Dividir la línea en el tiempo y las coordenadas
            String[] partes = linea.split(":");
            if (partes.length != 2) {
                throw new IllegalArgumentException("Formato inválido en línea: " + linea);
            }

            String tiempos = partes[0];
            String coordenadas = partes[1];

            // Dividir los tiempos en inicio y fin
            String[] rangoTiempo = tiempos.split("-");
            if (rangoTiempo.length != 2) {
                throw new IllegalArgumentException("Formato de tiempo inválido en línea: " + linea);
            }

            LocalDateTime horaInicio = parsearTiempo(rangoTiempo[0], year, month);
            LocalDateTime horaFin = parsearTiempo(rangoTiempo[1], year, month);

            // Parsear las coordenadas
            List<Posicion> tramos = parsearPosiciones(coordenadas);
            if (tramos.size() < 2) {
                throw new IllegalArgumentException("Deben especificarse al menos 2 posiciones para un bloqueo");
            }

            // Crear y retornar el objeto Bloqueo
            return new Bloqueo(horaInicio, horaFin, tramos);

        } catch (Exception e) {
            throw new IllegalArgumentException("Error analizando la línea: " + e.getMessage(), e);
        }
    }
}
