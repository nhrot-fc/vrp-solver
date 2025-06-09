package com.vroute.models;

import java.time.LocalDateTime;

public class ServeRecord {
    private final String vehicleId;
    private final String orderId;
    private final int servedGlpM3;
    private final LocalDateTime serveDate;

    public ServeRecord(String vehicleId, String orderId, int servedGlpM3, LocalDateTime serveDate) {
        this.vehicleId = vehicleId;
        this.orderId = orderId;
        this.servedGlpM3 = servedGlpM3;
        this.serveDate = serveDate;
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

    public LocalDateTime getServeDate() {
        return serveDate;
    }

    @Override
    public String toString() {
        return String.format("📝 %s→%s [GLP:%d m³] 🕒 %s", 
                vehicleId, 
                orderId,
                servedGlpM3, 
                serveDate.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
    }
}
