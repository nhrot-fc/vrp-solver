package com.vroute.orchest;

import com.vroute.models.Order;
import com.vroute.models.Vehicle;
import com.vroute.models.VehicleType;
import com.vroute.models.Position;
import com.vroute.models.Blockage;
import com.vroute.models.Constants;
import com.vroute.models.Maintenance;

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
    private static final DateTimeFormatter DATE_FORMAT_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Loads vehicles for the simulation environment
     */
    public List<Vehicle> loadVehicles(String filePath) {
        List<Vehicle> vehicles = new ArrayList<>();
        // TA: 2 units
        for (int i = 1; i <= 2; i++) {
            vehicles.add(new Vehicle(String.format("TA%02d", i), VehicleType.TA, Constants.CENTRAL_STORAGE_LOCATION));
        }
        // TB: 4 units
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle(String.format("TB%02d", i), VehicleType.TB, Constants.CENTRAL_STORAGE_LOCATION));
        }
        // TC: 4 units
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new Vehicle(String.format("TC%02d", i), VehicleType.TC, Constants.CENTRAL_STORAGE_LOCATION));
        }
        // TD: 10 units
        for (int i = 1; i <= 10; i++) {
            vehicles.add(new Vehicle(String.format("TD%02d", i), VehicleType.TD, Constants.CENTRAL_STORAGE_LOCATION));
        }
        return vehicles;
    }

    /**
     * Creates order events based on data from the file
     */
    public List<Event> createOrderEvents(String filePath, LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> orderEvents = new ArrayList<>();
        Pattern orderPattern = Pattern.compile("(\\d{2}d\\d{2}h\\d{2}m):(\\d+),(\\d+),([^,]+),(\\d+)m3,(\\d+)h");
        // Example: 11d13h31m:45,43,c-167,9m3,36h

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = orderPattern.matcher(line);
                if (matcher.matches()) {
                    try {
                        String arrivalStr = matcher.group(1);
                        int posX = Integer.parseInt(matcher.group(2));
                        int posY = Integer.parseInt(matcher.group(3));
                        String clientId = matcher.group(4);
                        int m3 = Integer.parseInt(matcher.group(5));
                        int limitHours = Integer.parseInt(matcher.group(6));

                        // Extract day, hour, minute from arrivalStr
                        Pattern dayHourMinutePattern = Pattern.compile("(\\d{2})d(\\d{2})h(\\d{2})m");
                        Matcher dhmMatcher = dayHourMinutePattern.matcher(arrivalStr);
                        if (!dhmMatcher.matches())
                            continue;

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

                        // Check if order is within the simulation timeframe
                        if (!arrivalDateTime.isBefore(startDate) && !arrivalDateTime.isAfter(endDate)) {
                            Position clientPosition = new Position(posX, posY);
                            Order order = new Order(clientId + "_" + arrivalStr, arrivalDateTime, dueDateTime, m3,
                                    clientPosition);
                            
                            // Create an order arrival event
                            Event orderEvent = new Event(EventType.ORDER_ARRIVAL, arrivalDateTime, order);
                            orderEvents.add(orderEvent);
                        }
                    } catch (DateTimeParseException | NumberFormatException e) {
                        System.err.println("Error parsing order line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading orders file: " + filePath + " - " + e.getMessage());
        }
        return orderEvents;
    }

    /**
     * Creates blockage events based on data from the file
     */
    public List<Event> createBlockageEvents(String filePath, LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> blockageEvents = new ArrayList<>();
        // Pattern: ##d##h##m-##d##h##m:x1,y1,x2,y2,...,xn,yn
        // Example: 01d06h00m-01d15h00m:31,21,34,21
        Pattern blockagePattern = Pattern.compile("(\\d{2}d\\d{2}h\\d{2}m)-(\\d{2}d\\d{2}h\\d{2}m):([\\d,]+)");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                Matcher matcher = blockagePattern.matcher(line);
                
                if (matcher.matches()) {
                    try {
                        String startTimeStr = matcher.group(1);
                        String endTimeStr = matcher.group(2);
                        String pointsStr = matcher.group(3);
                        
                        // Parse start and end times
                        LocalDateTime blockageStart = parseTimeFromDMH(startTimeStr, startDate);
                        LocalDateTime blockageEnd = parseTimeFromDMH(endTimeStr, startDate);
                        
                        // Check if the blockage overlaps with the simulation timeframe
                        if (!(blockageEnd.isBefore(startDate) || blockageStart.isAfter(endDate))) {
                            // Parse blockage points
                            String[] coords = pointsStr.split(",");
                            List<Position> blockagePoints = new ArrayList<>();
                            
                            for (int i = 0; i < coords.length; i += 2) {
                                if (i + 1 >= coords.length)
                                    break; // Avoid potential IndexOutOfBounds
                                
                                int x = Integer.parseInt(coords[i]);
                                int y = Integer.parseInt(coords[i + 1]);
                                blockagePoints.add(new Position(x, y));
                            }
                            
                            if (blockagePoints.size() >= 2) { // Need at least two points to form a path
                                // Create the blockage object
                                Blockage blockage = new Blockage(blockageStart, blockageEnd, blockagePoints);
                                
                                // Create a blockage start event
                                Event startEvent = new Event(EventType.BLOCKAGE_START, blockageStart, blockage);
                                blockageEvents.add(startEvent);
                                
                                // Also create a blockage end event
                                Event endEvent = new Event(EventType.BLOCKAGE_END, blockageEnd, blockage);
                                blockageEvents.add(endEvent);
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
        
        return blockageEvents;
    }
    
    /**
     * Creates maintenance events based on data from the file
     */
    public List<Event> createMaintenanceEvents(String filePath, LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> maintenanceEvents = new ArrayList<>();
        // Pattern: aaaammdd:TTNN
        // Example: 20250401:TA01
        Pattern maintenancePattern = Pattern.compile("(\\d{8}):(\\w{4})");
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                Matcher matcher = maintenancePattern.matcher(line);
                
                if (matcher.matches()) {
                    try {
                        String dateStr = matcher.group(1);
                        String vehicleId = matcher.group(2);
                        
                        // Parse maintenance date
                        LocalDate maintenanceDate = LocalDate.parse(dateStr, DATE_FORMAT_YMD);
                        LocalDateTime maintenanceDateTime = LocalDateTime.of(maintenanceDate, LocalTime.MIDNIGHT);
                        
                        // Check if the maintenance is within our time window
                        if (!maintenanceDateTime.isBefore(startDate) && !maintenanceDateTime.isAfter(endDate)) {
                            
                            // Create maintenance object
                            Maintenance maintenance = new Maintenance(vehicleId, maintenanceDate);
                            
                            // Create maintenance event
                            Event maintenanceEvent = new Event(EventType.MAINTENANCE, maintenanceDateTime, maintenance);
                            maintenanceEvents.add(maintenanceEvent);
                        }
                    } catch (DateTimeParseException e) {
                        System.err.println("Error parsing maintenance line: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading maintenance file: " + filePath + " - " + e.getMessage());
        }
        
        return maintenanceEvents;
    }

    /**
     * Creates scheduled refill events for depots
     */
    public List<Event> createDepotRefillEvents(LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> refillEvents = new ArrayList<>();
        
        // Schedule daily refills at midnight
        LocalDateTime currentDay = startDate.withHour(0).withMinute(0).withSecond(0);
        
        while (!currentDay.isAfter(endDate)) {
            // Create a refill event at midnight each day
            Event refillEvent = new Event(EventType.GLP_DEPOT_REFILL, currentDay, null);
            refillEvents.add(refillEvent);
            
            // Move to next day
            currentDay = currentDay.plusDays(1);
        }
        
        return refillEvents;
    }
    
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
     * Load all necessary data and create events for the simulation
     */
    public List<Event> loadAllEvents(LocalDateTime startDate, LocalDateTime endDate) {
        List<Event> allEvents = new ArrayList<>();
        
        // Load scheduled maintenance events
        allEvents.addAll(createMaintenanceEvents("data/mantpreventivo.txt", startDate, endDate));
        
        // Load order events
        allEvents.addAll(createOrderEvents("data/ventas202503.txt", startDate, endDate));
        
        // Load blockage events
        allEvents.addAll(createBlockageEvents("data/202503.bloqueadas", startDate, endDate));
        
        // Add scheduled depot refill events
        allEvents.addAll(createDepotRefillEvents(startDate, endDate));
        
        return allEvents;
    }
}
