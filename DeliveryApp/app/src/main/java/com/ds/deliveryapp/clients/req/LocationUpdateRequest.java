package com.ds.deliveryapp.clients.req;

public class LocationUpdateRequest {
    private Double lat;
    private Double lon;
    private Double accuracy; // GPS accuracy in meters
    private Double speed; // Optional: speed in m/s
    private Long timestamp; // Unix timestamp in milliseconds

    public LocationUpdateRequest() {
    }

    public LocationUpdateRequest(Double lat, Double lon, Double accuracy, Double speed, Long timestamp) {
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy;
        this.speed = speed;
        this.timestamp = timestamp;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
