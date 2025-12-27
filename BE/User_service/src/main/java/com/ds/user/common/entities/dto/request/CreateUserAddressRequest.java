package com.ds.user.common.entities.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to create a user address.
 * 
 * Two modes:
 * 1. Provide destinationId (existing address from zone-service)
 * 2. Provide lat/lon (will call zone-service to get-or-create address first)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserAddressRequest {
    /**
     * Optional: If provided, use this existing address ID from zone-service.
     * If not provided, lat and lon must be provided to create/get address.
     */
    private String destinationId;
    
    /**
     * Required if destinationId is not provided: Latitude for creating/getting address in zone-service
     */
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal lat;
    
    /**
     * Required if destinationId is not provided: Longitude for creating/getting address in zone-service
     */
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal lon;
    
    /**
     * Optional: Name for the address (used when creating address in zone-service)
     */
    private String name;
    
    /**
     * Optional: Address text (used when creating address in zone-service)
     */
    private String addressText;
    
    private String note;
    
    private String tag; // e.g., "Home", "Work", "Other"
    
    @Builder.Default
    private Boolean isPrimary = false;
}
