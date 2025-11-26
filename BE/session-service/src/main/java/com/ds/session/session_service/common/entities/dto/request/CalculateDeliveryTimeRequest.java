package com.ds.session.session_service.common.entities.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to calculate delivery time for a list of parcels
 * Used to validate if postpone time is within session time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateDeliveryTimeRequest {
    @NotEmpty(message = "Parcel IDs cannot be empty")
    private List<String> parcelIds;
    
    /**
     * Optional: Current location of delivery man (lat, lon)
     * If not provided, will use warehouse location or first parcel location
     */
    private Double currentLat;
    private Double currentLon;
}
