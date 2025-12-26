package com.ds.session.session_service.infrastructure.osrm;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OSRM Table API response
 * Response format from /table/v1/driving/{coordinates}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OSRMMatrixResponse {
    
    private String code;
    private List<List<Double>> durations; // Duration matrix in seconds
    private List<List<Double>> distances; // Distance matrix in meters
    private List<Waypoint> sources;
    private List<Waypoint> destinations;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Waypoint {
        private String hint;
        private Double distance;
        private String name;
        private List<Double> location; // [lon, lat]
    }
}
