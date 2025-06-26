package com.vroute.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Order {
    // unmutable attributes
    private final String id;
    private final LocalDateTime arriveTime;
    private final LocalDateTime dueTime;
    private final int glpRequestM3;
    private final Position position;

    // mutable attributes
    private int remainingGlpM3;
    private List<ServeRecord> records;

    public Order(String id, LocalDateTime arriveDate, LocalDateTime dueDate, int glpRequestM3, Position position) {
        this.id = id;
        this.arriveTime = arriveDate;
        this.dueTime = dueDate;
        this.glpRequestM3 = glpRequestM3;
        this.position = position;

        this.remainingGlpM3 = glpRequestM3;
        this.records = new java.util.ArrayList<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public LocalDateTime getArriveTime() {
        return arriveTime;
    }

    public LocalDateTime getDueTime() {
        return dueTime;
    }

    public int getGlpRequestM3() {
        return glpRequestM3;
    }

    public int getRemainingGlpM3() {
        return remainingGlpM3;
    }

    public List<ServeRecord> getRecords() {
        return records;
    }

    public Position getPosition() {
        return position;
    }

    // Operations
    public void recordDelivery(int deliveredVolumeM3, String vehicleId, LocalDateTime serveDate) {
        remainingGlpM3 -= deliveredVolumeM3;
        records.add(new ServeRecord(vehicleId, id, deliveredVolumeM3, serveDate));
    }

    public boolean isDelivered() {
        return remainingGlpM3 <= 0;
    }

    public boolean isOverdue(LocalDateTime referenceDateTime) {
        return referenceDateTime.isAfter(dueTime);
    }

    public int timeUntilDue(LocalDateTime referenceDateTime) {
        if (isDelivered())
            return 0;
        if (isOverdue(referenceDateTime))
            return -1;

        Duration duration = Duration.between(referenceDateTime, dueTime);
        long minutesUntilDue = duration.toMinutes();

        return (int) minutesUntilDue;
    }

    public double calculatePriority(LocalDateTime referenceDateTime) {
        if (isDelivered())
            return 0.0;
        int minutesUntilDue = timeUntilDue(referenceDateTime);
        if (minutesUntilDue < 0)
            return 1000.0 + (-minutesUntilDue / 60.0);
        return 100.0 / (1.0 + (minutesUntilDue / 60.0));
    }

    @Override
    public String toString() {
        String status = isDelivered() ? "✅" : "⏳";
        return String.format("📦 %s %s [🕒 %s] [GLP: %d/%d m³] %s",
                id,
                status,
                dueTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
                remainingGlpM3,
                glpRequestM3,
                position);
    }

    public Order clone() {
        Order clonedOrder = new Order(
                this.id,
                this.arriveTime,
                this.dueTime,
                this.glpRequestM3,
                this.position);

        clonedOrder.remainingGlpM3 = this.remainingGlpM3;
        clonedOrder.records = new java.util.ArrayList<>(this.records);
        return clonedOrder;
    }
}
