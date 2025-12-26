package com.ds.session.session_service.business.v1.services.model;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a Shipper (DeliveryMan) for VRP solving
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipper {
    
    /**
     * Shipper ID (DeliveryMan ID)
     */
    private String shipperId;
    
    /**
     * Latitude of shipper's start location
     */
    private double lat;
    
    /**
     * Longitude of shipper's start location
     */
    private double lon;
    
    /**
     * Shift start time
     */
    private LocalTime shiftStart;
    
    /**
     * Shift end time (calculated as shiftStart + maxSessionTime)
     */
    private LocalTime shiftEnd;
    
    /**
     * Maximum session time in hours (3.5h for morning, 4.5h for afternoon)
     */
    private double maxSessionTime; // in hours
    
    /**
     * Vehicle capacity (number of parcels the shipper can carry)
     */
    private int capacity;
    
    /**
     * Working zone IDs (for zone-based filtering), ordered by priority
     */
    private java.util.List<String> zoneIds;
}
