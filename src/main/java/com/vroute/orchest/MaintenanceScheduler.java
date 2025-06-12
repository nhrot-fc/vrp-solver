package com.vroute.orchest;

import com.vroute.models.Environment;
import com.vroute.models.MaintenanceTask;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for managing the schedule of maintenance tasks.
 * It loads, validates, and adds maintenance tasks to the environment.
 */
public class MaintenanceScheduler {
    private static final Logger logger = Logger.getLogger(MaintenanceScheduler.class.getName());

    /**
     * Loads maintenance tasks from a file and adds them to the environment.
     * The file should follow the format specified in the requirements:
     * "yyyyMMdd:TTNN" for each line
     * 
     * @param filePath Path to the maintenance schedule file
     * @param environment The environment to add the tasks to
     * @param referenceDate A reference date to calculate repetitions from
     * @return The number of tasks loaded
     */
    public static int loadMaintenanceSchedule(String filePath, Environment environment, LocalDate referenceDate) {
        List<MaintenanceTask> tasks = loadMaintenanceTasksFromFile(filePath);
        
        if (tasks.isEmpty()) {
            logger.warning("No maintenance tasks were loaded from " + filePath);
            return 0;
        }
        
        // Add tasks to environment, accounting for repetitions
        int addedTasks = 0;
        for (MaintenanceTask task : tasks) {
            // Add the original task if it's in the future
            if (task.getStartTime().isAfter(environment.getCurrentTime())) {
                environment.addMaintenanceTask(task);
                addedTasks++;
            }
            
            // Add future repetitions within a reasonable timeframe (1 year)
            LocalDateTime maxDate = environment.getCurrentTime().plusYears(1);
            MaintenanceTask nextTask = task;
            
            // Generate up to 6 repetitions (covers 1 year for bimonthly schedule)
            for (int i = 0; i < 6; i++) {
                nextTask = nextTask.createNextTask();
                
                // Stop if we're too far in the future
                if (nextTask.getStartTime().isAfter(maxDate)) {
                    break;
                }
                
                // Only add future tasks
                if (nextTask.getStartTime().isAfter(environment.getCurrentTime())) {
                    environment.addMaintenanceTask(nextTask);
                    addedTasks++;
                }
            }
        }
        
        logger.info(String.format("Loaded %d maintenance tasks from %s (including repetitions)", 
                addedTasks, filePath));
        return addedTasks;
    }
    
    /**
     * Loads maintenance tasks directly from a file.
     * 
     * @param filePath Path to the maintenance schedule file
     * @return List of maintenance tasks from the file
     */
    public static List<MaintenanceTask> loadMaintenanceTasksFromFile(String filePath) {
        List<MaintenanceTask> tasks = new ArrayList<>();
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            logger.warning("Maintenance schedule file does not exist: " + filePath);
            return tasks;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                MaintenanceTask task = MaintenanceTask.fromString(line);
                if (task != null) {
                    tasks.add(task);
                } else {
                    logger.warning(String.format("Invalid maintenance record at line %d: %s", 
                            lineNumber, line));
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading maintenance schedule file: " + filePath, e);
        }
        
        return tasks;
    }
    
    /**
     * Creates a sample maintenance schedule file.
     * 
     * @param filePath Path where to save the file
     * @param startDate The start date for the schedule
     * @return true if the file was created successfully
     */
    public static boolean createSampleMaintenanceSchedule(String filePath, LocalDate startDate) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("# Maintenance schedule - Format: yyyyMMdd:TTNN\n");
            content.append("# Generated sample for demonstration purposes\n\n");
            
            LocalDate currentDate = startDate;
            
            // TA vehicles
            content.append(formatMaintenanceDate(currentDate)).append(":TA01\n");
            currentDate = currentDate.plusDays(2);
            
            // TD vehicles
            for (int i = 1; i <= 5; i++) {
                String vehicleId = String.format("TD%02d", i);
                content.append(formatMaintenanceDate(currentDate)).append(":").append(vehicleId).append("\n");
                currentDate = currentDate.plusDays(3);
            }
            
            // TB vehicles
            for (int i = 1; i <= 2; i++) {
                String vehicleId = String.format("TB%02d", i);
                content.append(formatMaintenanceDate(currentDate)).append(":").append(vehicleId).append("\n");
                currentDate = currentDate.plusDays(3);
            }
            
            // TC vehicles
            for (int i = 1; i <= 2; i++) {
                String vehicleId = String.format("TC%02d", i);
                content.append(formatMaintenanceDate(currentDate)).append(":").append(vehicleId).append("\n");
                currentDate = currentDate.plusDays(3);
            }
            
            Files.writeString(Paths.get(filePath), content.toString());
            logger.info("Created sample maintenance schedule file: " + filePath);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create sample maintenance schedule file: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Formats a date in the required format for maintenance tasks.
     * 
     * @param date The date to format
     * @return The formatted date string "yyyyMMdd"
     */
    private static String formatMaintenanceDate(LocalDate date) {
        return String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
