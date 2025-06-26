package com.vroute.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServeRecord {
    private final String vehicleId;
    private final String orderId;
    private final int servedGlpM3;
    private final LocalDateTime serveTime;

    public ServeRecord(String vehicleId, String orderId, int servedGlpM3, LocalDateTime serveDate) {
        this.vehicleId = vehicleId;
        this.orderId = orderId;
        this.servedGlpM3 = servedGlpM3;
        this.serveTime = serveDate;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getServedGlpM3() {
        return servedGlpM3;
    }

    public LocalDateTime getServeTime() {
        return serveTime;
    }

    @Override
    public String toString() {
        return String.format("📝 %s→%s [GLP:%d m³] 🕒 %s", 
                vehicleId, 
                orderId,
                servedGlpM3, 
                serveTime.format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)));

    }
}
