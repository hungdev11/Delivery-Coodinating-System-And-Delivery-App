package com.ds.session.session_service.application.client.zoneclient.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    private List<Waypoint> waypoints;
    
    @JsonProperty("steps")
    private Boolean steps;
    
    @JsonProperty("annotations")
    private Boolean annotations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Waypoint {
        private Double lat;
        private Double lon;
        
        @JsonProperty("parcelId")
        private String parcelId;
    }
}

