package com.vroute.orchest;

import com.vroute.models.Environment;
import com.vroute.models.Maintenance;

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
public class MaintenanceScheduler {
    private static final Logger logger = Logger.getLogger(MaintenanceScheduler.class.getName());

    public static int loadMaintenanceSchedule(String filePath, Environment environment, LocalDate referenceDate) {
        List<Maintenance> tasks = loadMaintenanceTasksFromFile(filePath);

        if (tasks.isEmpty()) {
            logger.warning("No maintenance tasks were loaded from " + filePath);
            return 0;
        }

        int addedTasks = 0;
        for (Maintenance task : tasks) {
            if (task.getStartTime().isAfter(environment.getCurrentTime())) {
                environment.addMaintenanceTask(task);
                addedTasks++;
            }

            LocalDateTime maxDate = environment.getCurrentTime().plusYears(1);
            Maintenance nextTask = task;
            for (int i = 0; i < 6; i++) {
                nextTask = nextTask.createNextTask();

                if (nextTask.getStartTime().isAfter(maxDate)) {
                    break;
                }
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


    public static List<Maintenance> loadMaintenanceTasksFromFile(String filePath) {
        List<Maintenance> tasks = new ArrayList<>();
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

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Maintenance task = Maintenance.fromString(line);
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


    public static boolean createSampleMaintenanceSchedule(String filePath, LocalDate startDate) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("# Maintenance schedule - Format: yyyyMMdd:TTNN\n");
            content.append("# Generated sample for demonstration purposes\n\n");

            LocalDate currentDate = startDate;

            content.append(formatMaintenanceDate(currentDate)).append(":TA01\n");
            currentDate = currentDate.plusDays(2);
            for (int i = 1; i <= 5; i++) {
                String vehicleId = String.format("TD%02d", i);
                content.append(formatMaintenanceDate(currentDate)).append(":").append(vehicleId).append("\n");
                currentDate = currentDate.plusDays(3);
            }
            for (int i = 1; i <= 2; i++) {
                String vehicleId = String.format("TB%02d", i);
                content.append(formatMaintenanceDate(currentDate)).append(":").append(vehicleId).append("\n");
                currentDate = currentDate.plusDays(3);
            }
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


    private static String formatMaintenanceDate(LocalDate date) {
        return String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
