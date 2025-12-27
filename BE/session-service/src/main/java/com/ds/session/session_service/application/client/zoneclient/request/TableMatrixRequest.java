package com.ds.session.session_service.application.client.zoneclient.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for OSRM table matrix API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableMatrixRequest {
    
    private List<Coordinate> coordinates;
    private String vehicle; // "car" or "motorbike"
    private String mode; // "v2-full", "v2-rating-only", etc.
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinate {
        private double lat;
        private double lon;
    }
}
