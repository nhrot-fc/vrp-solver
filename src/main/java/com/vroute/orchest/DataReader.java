package com.vroute.orchest;

import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleType;
import com.vroute.models.Position;
import com.vroute.models.Blockage;
import com.vroute.models.Maintenance;
import com.vroute.models.Incident;
import com.vroute.models.IncidentType;
import com.vroute.models.Shift;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Year;

public class DataReader {
    // Formatters for parsing dates and times from different file formats
    private static final DateTimeFormatter DATE_FORMAT_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    // Removed unused formatter
    
    // Placeholder for base path if needed, or pass full paths to methods
    // private String basePath = "path/to/your/data/files/";

    public List<Vehicle> loadVehicles(String filePath) {
        List<Vehicle> vehicles = new ArrayList<>();
        // According to the README, vehicle definitions are fixed.
        // This method could initialize them based on the README's table.
        // TTNN (TT = Type, NN = Correlative. Ej: TA01, TD10)

        // TA: 2 units
        vehicles.add(new Vehicle("TA01", VehicleType.TA, new Position(12, 8))); // Assuming starting at main depot
        vehicles.add(new Vehicle("TA02", VehicleType.TA, new Position(12, 8)));
        // TB: 4 units
        vehicles.add(new Vehicle("TB01", VehicleType.TB, new Position(12, 8)));
        vehicles.add(new Vehicle("TB02", VehicleType.TB, new Position(12, 8)));
        vehicles.add(new Vehicle("TB03", VehicleType.TB, new Position(12, 8)));
        vehicles.add(new Vehicle("TB04", VehicleType.TB, new Position(12, 8)));
        // TC: 4 units
        vehicles.add(new Vehicle("TC01", VehicleType.TC, new Position(12, 8)));
        vehicles.add(new Vehicle("TC02", VehicleType.TC, new Position(12, 8)));
        vehicles.add(new Vehicle("TC03", VehicleType.TC, new Position(12, 8)));
        vehicles.add(new Vehicle("TC04", VehicleType.TC, new Position(12, 8)));
        // TD: 10 units
        for (int i = 1; i <= 10; i++) {
            vehicles.add(new Vehicle(String.format("TD%02d", i), VehicleType.TD, new Position(12, 8)));
        }
        // If a specific file format for vehicles is provided later, this method can be updated.
        return vehicles;
    }

    public List<Order> loadOrders(String filePath, LocalDateTime startDate, int durationHours, int maxOrders) {
        List<Order> orders = new ArrayList<>();
        Pattern orderPattern = Pattern.compile("(\\d{2}d\\d{2}h\\d{2}m):(\\d+),(\\d+),([^,]+),(\\d+)m3,(\\d+)h");
        // Example: 11d13h31m:45,43,c-167,9m3,36h

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int ordersLoaded = 0;
            while ((line = br.readLine()) != null && (maxOrders <= 0 || ordersLoaded < maxOrders)) {
                Matcher matcher = orderPattern.matcher(line);
                if (matcher.matches()) {
                    try {
                        String arrivalStr = matcher.group(1);
                        int posX = Integer.parseInt(matcher.group(2));
                        int posY = Integer.parseInt(matcher.group(3));
                        String clientId = matcher.group(4);
                        int m3 = Integer.parseInt(matcher.group(5));
                        int limitHours = Integer.parseInt(matcher.group(6));

                        // Assuming the day in "##d##h##m" refers to day of the month of the file.
                        // The year and month come from the file name e.g., ventas2025mm
                        // For simplicity, we'll need to parse year/month from filePath or pass it.
                        // Let's assume startDate already has the correct year and month for parsing ##d.
                        // This part needs refinement based on how year/month are determined for orders.
                        // For now, we'll construct LocalDateTime using startDate's year/month and parsed day.
                        
                        // Extract day, hour, minute from arrivalStr
                        Pattern dayHourMinutePattern = Pattern.compile("(\\d{2})d(\\d{2})h(\\d{2})m");
                        Matcher dhmMatcher = dayHourMinutePattern.matcher(arrivalStr);
                        if (!dhmMatcher.matches()) continue;

                        int day = Integer.parseInt(dhmMatcher.group(1));
                        int hour = Integer.parseInt(dhmMatcher.group(2));
                        int minute = Integer.parseInt(dhmMatcher.group(3));

                        // Check for valid day for the given month
                        int year = startDate.getYear();
                        int month = startDate.getMonthValue();
                        int maxDaysInMonth = startDate.getMonth().length(Year.isLeap(year));
                        
                        if (day < 1 || day > maxDaysInMonth) {
                            System.err.println("Invalid day: " + day + " for month: " + startDate.getMonth() + 
                                               " in line: " + line + " - using day 1 instead");
                            day = 1; // Default to day 1 if invalid
                        }

                        LocalDateTime arrivalDateTime = LocalDateTime.of(year, month, day, hour, minute);
                        LocalDateTime dueDateTime = arrivalDateTime.plusHours(limitHours);

                        if (!arrivalDateTime.isBefore(startDate) && 
                            (durationHours <= 0 || arrivalDateTime.isBefore(startDate.plusHours(durationHours)))) {
                            Position clientPosition = new Position(posX, posY);
                            Order order = new Order(clientId + "_" + arrivalStr, arrivalDateTime, dueDateTime, m3, clientPosition);
                            orders.add(order);
                            if (maxOrders > 0) {
                                ordersLoaded++;
                            }
                        }
                    } catch (DateTimeParseException | NumberFormatException e) {
                        System.err.println("Error parsing order line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading orders file: " + filePath + " - " + e.getMessage());
        }
        return orders;
    }

    /**
     * Loads street blockages from a file based on time period and maximum count
     * 
     * @param filePath Path to the blockages file (format: aaaamm.bloqueadas)
     * @param startDate The reference start date/time
     * @param durationHours Duration in hours to load blockages for (0 for unlimited)
     * @param maxBlockages Maximum number of blockages to load (0 for unlimited)
     * @return List of Blockage objects
     */
    public List<Blockage> loadBlockages(String filePath, LocalDateTime startDate, int durationHours, int maxBlockages) {
        List<Blockage> blockages = new ArrayList<>();
        // Pattern: ##d##h##m-##d##h##m:x1,y1,x2,y2,...,xn,yn
        // Example: 01d06h00m-01d15h00m:31,21,34,21
        Pattern blockagePattern = Pattern.compile("(\\d{2}d\\d{2}h\\d{2}m)-(\\d{2}d\\d{2}h\\d{2}m):([\\d,]+)");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int blockagesLoaded = 0;
            
            while ((line = br.readLine()) != null && (maxBlockages <= 0 || blockagesLoaded < maxBlockages)) {
                Matcher matcher = blockagePattern.matcher(line);
                
                if (matcher.matches()) {
                    try {
                        String startTimeStr = matcher.group(1);
                        String endTimeStr = matcher.group(2);
                        String pointsStr = matcher.group(3);
                        
                        // Parse start and end times
                        LocalDateTime blockageStart = parseTimeFromDMH(startTimeStr, startDate);
                        LocalDateTime blockageEnd = parseTimeFromDMH(endTimeStr, startDate);
                        
                        // Check if the blockage is within our time window
                        if (isTimeInInterval(blockageStart, blockageEnd, startDate, durationHours)) {
                            // Parse blockage points
                            String[] coords = pointsStr.split(",");
                            List<Position> blockagePoints = new ArrayList<>();
                            
                            for (int i = 0; i < coords.length; i += 2) {
                                if (i + 1 >= coords.length) break; // Avoid potential IndexOutOfBounds
                                
                                int x = Integer.parseInt(coords[i]);
                                int y = Integer.parseInt(coords[i + 1]);
                                blockagePoints.add(new Position(x, y));
                            }
                            
                            if (blockagePoints.size() >= 2) { // Need at least two points to form a path
                                blockages.add(new Blockage(blockageStart, blockageEnd, blockagePoints));
                                if (maxBlockages > 0) {
                                    blockagesLoaded++;
                                }
                            }
                        }
                    } catch (DateTimeParseException | NumberFormatException e) {
                        System.err.println("Error parsing blockage line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading blockages file: " + filePath + " - " + e.getMessage());
        }
        
        return blockages;
    }
    
    /**
     * Loads maintenance tasks from a file based on time period and maximum count
     * 
     * @param filePath Path to the maintenance file (mantpreventivo)
     * @param startDate The reference start date/time
     * @param durationDays Duration in days to load maintenance tasks for (0 for unlimited)
     * @param maxTasks Maximum number of tasks to load (0 for unlimited)
     * @return List of MaintenanceTask objects
     */
    public List<Maintenance> loadMaintenanceSchedule(String filePath, LocalDateTime startDate, int durationDays, int maxTasks) {
        List<Maintenance> tasks = new ArrayList<>();
        // Pattern: aaaammdd:TTNN
        // Example: 20250401:TA01
        Pattern maintenancePattern = Pattern.compile("(\\d{8}):(\\w{4})");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int tasksLoaded = 0;
            
            while ((line = br.readLine()) != null && (maxTasks <= 0 || tasksLoaded < maxTasks)) {
                Matcher matcher = maintenancePattern.matcher(line);
                
                if (matcher.matches()) {
                    try {
                        String dateStr = matcher.group(1);
                        String vehicleId = matcher.group(2);
                        
                        // Parse maintenance date
                        LocalDate maintenanceDate = LocalDate.parse(dateStr, DATE_FORMAT_YMD);
                        LocalDateTime maintenanceDateTime = LocalDateTime.of(maintenanceDate, LocalTime.MIDNIGHT);
                        
                        // Check if the maintenance is within our time window
                        if (!maintenanceDateTime.isBefore(startDate) && 
                            (durationDays <= 0 || maintenanceDateTime.isBefore(startDate.plusDays(durationDays)))) {
                            
                            tasks.add(new Maintenance(vehicleId, maintenanceDate));
                            
                            if (maxTasks > 0) {
                                tasksLoaded++;
                            }
                        }
                    } catch (DateTimeParseException e) {
                        System.err.println("Error parsing maintenance line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading maintenance file: " + filePath + " - " + e.getMessage());
        }
        
        return tasks;
    }
    
    /**
     * Loads potential vehicle incidents from a file
     * 
     * @param filePath Path to the incidents file (averias.txt)
     * @param maxIncidents Maximum number of incidents to load (0 for unlimited)
     * @return List of Incident objects
     */
    public List<Incident> loadIncidents(String filePath, int maxIncidents) {
        List<Incident> incidents = new ArrayList<>();
        // Pattern: tt_######_ti
        // Example: T1_TA01_TI2
        Pattern incidentPattern = Pattern.compile("(T\\d)_(\\w{4})_(TI\\d)");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int incidentsLoaded = 0;
            
            while ((line = br.readLine()) != null && (maxIncidents <= 0 || incidentsLoaded < maxIncidents)) {
                Matcher matcher = incidentPattern.matcher(line);
                
                if (matcher.matches()) {
                    try {
                        String shiftStr = matcher.group(1);
                        String vehicleId = matcher.group(2);
                        String incidentTypeStr = matcher.group(3);
                        
                        // Parse shift
                        Shift shift;
                        switch (shiftStr) {
                            case "T1": shift = Shift.T1; break;
                            case "T2": shift = Shift.T2; break;
                            case "T3": shift = Shift.T3; break;
                            default: throw new IllegalArgumentException("Invalid shift: " + shiftStr);
                        }
                        
                        // Parse incident type
                        IncidentType incidentType;
                        switch (incidentTypeStr) {
                            case "TI1": incidentType = IncidentType.TI1; break;
                            case "TI2": incidentType = IncidentType.TI2; break;
                            case "TI3": incidentType = IncidentType.TI3; break;
                            default: throw new IllegalArgumentException("Invalid incident type: " + incidentTypeStr);
                        }
                        
                        incidents.add(new Incident(vehicleId, incidentType, shift));
                        
                        if (maxIncidents > 0) {
                            incidentsLoaded++;
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error parsing incident line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading incidents file: " + filePath + " - " + e.getMessage());
        }
        
        return incidents;
    }
    
    /**
     * Helper method to parse a time string in the format ##d##h##m into a LocalDateTime
     * 
     * @param timeStr The time string to parse
     * @param referenceDate The reference date to use for year and month
     * @return The parsed LocalDateTime
     */
    private LocalDateTime parseTimeFromDMH(String timeStr, LocalDateTime referenceDate) {
        Pattern dayHourMinutePattern = Pattern.compile("(\\d{2})d(\\d{2})h(\\d{2})m");
        Matcher dhmMatcher = dayHourMinutePattern.matcher(timeStr);
        
        if (!dhmMatcher.matches()) {
            throw new DateTimeParseException("Invalid time format", timeStr, 0);
        }
        
        int day = Integer.parseInt(dhmMatcher.group(1));
        int hour = Integer.parseInt(dhmMatcher.group(2));
        int minute = Integer.parseInt(dhmMatcher.group(3));
        
        return LocalDateTime.of(referenceDate.getYear(), referenceDate.getMonth(), day, hour, minute);
    }
    
    /**
     * Helper method to check if a time interval overlaps with our reference time window
     * 
     * @param intervalStart The start of the interval to check
     * @param intervalEnd The end of the interval to check
     * @param referenceStart The start of our reference window
     * @param durationHours The duration of our reference window in hours (0 for unlimited)
     * @return true if there is overlap, false otherwise
     */
    private boolean isTimeInInterval(LocalDateTime intervalStart, LocalDateTime intervalEnd, 
                                    LocalDateTime referenceStart, int durationHours) {
        // If durationHours is 0, we consider all times after referenceStart
        if (durationHours <= 0) {
            return !intervalEnd.isBefore(referenceStart);
        }
        
        LocalDateTime referenceEnd = referenceStart.plusHours(durationHours);
        
        // Check if there's overlap between the intervals
        return !(intervalEnd.isBefore(referenceStart) || intervalStart.isAfter(referenceEnd));
    }
}
