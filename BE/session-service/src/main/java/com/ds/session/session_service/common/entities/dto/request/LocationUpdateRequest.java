package com.ds.session.session_service.common.entities.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateRequest {
    private Double lat;
    private Double lon;
    private Double accuracy; // GPS accuracy in meters
    private Double speed; // Optional: speed in m/s
    private Long timestamp; // Unix timestamp in milliseconds
}
