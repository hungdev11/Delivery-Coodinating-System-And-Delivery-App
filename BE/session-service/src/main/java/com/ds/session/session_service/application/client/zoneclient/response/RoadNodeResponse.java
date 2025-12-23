package com.ds.session.session_service.application.client.zoneclient.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadNodeResponse {
    private String id;
    private Double latitude;
    private Double longitude;
    private Double distance; // Distance in meters
}
