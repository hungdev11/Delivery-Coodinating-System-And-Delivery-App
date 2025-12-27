package com.ds.session.session_service.application.client.zoneclient.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for OSRM table matrix API
 * Wrapped in BaseResponse{success: true, result: {...}} from zone-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableMatrixResponse {
    
    private String code;
    private List<List<Double>> durations; // Duration matrix in seconds
    private List<List<Double>> distances; // Distance matrix in meters
    private List<Waypoint> sources;
    private List<Waypoint> destinations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Waypoint {
        private String hint;
        private Double distance;
        private String name;
        private List<Double> location; // [lon, lat]
    }
}
