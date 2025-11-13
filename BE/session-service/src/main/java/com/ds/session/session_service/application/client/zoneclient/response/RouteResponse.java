package com.ds.session.session_service.application.client.zoneclient.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RouteResponse {
    private String code;
    private RouteData route;
    
    @Data
    public static class RouteData {
        private Double distance; // in meters
        private Double duration; // in seconds
        private String geometry;
        private Summary summary;
    }
    
    @Data
    public static class Summary {
        @JsonProperty("totalDistance")
        private Double totalDistance; // in meters
        
        @JsonProperty("totalDuration")
        private Double totalDuration; // in seconds
        
        @JsonProperty("totalWaypoints")
        private Integer totalWaypoints;
    }
}

