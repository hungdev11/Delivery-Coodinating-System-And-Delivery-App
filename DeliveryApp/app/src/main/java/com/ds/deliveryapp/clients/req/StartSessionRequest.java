package com.ds.deliveryapp.clients.req;

/**
 * Request body for starting a delivery session with shipper's current location.
 * Timestamp is sent as ISO-8601 string to be parsed as LocalDateTime on backend.
 */
public class StartSessionRequest {
    private Double startLocationLat;
    private Double startLocationLon;
    private String startLocationTimestamp;

    public StartSessionRequest() {
    }

    public StartSessionRequest(Double startLocationLat, Double startLocationLon, String startLocationTimestamp) {
        this.startLocationLat = startLocationLat;
        this.startLocationLon = startLocationLon;
        this.startLocationTimestamp = startLocationTimestamp;
    }

    public Double getStartLocationLat() {
        return startLocationLat;
    }

    public void setStartLocationLat(Double startLocationLat) {
        this.startLocationLat = startLocationLat;
    }

    public Double getStartLocationLon() {
        return startLocationLon;
    }

    public void setStartLocationLon(Double startLocationLon) {
        this.startLocationLon = startLocationLon;
    }

    public String getStartLocationTimestamp() {
        return startLocationTimestamp;
    }

    public void setStartLocationTimestamp(String startLocationTimestamp) {
        this.startLocationTimestamp = startLocationTimestamp;
    }
}
