package com.ds.session.session_service.business.v1.services.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing an Order (Parcel) for VRP solving
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    /**
     * Order ID (Parcel ID)
     */
    private String orderId;
    
    /**
     * Latitude of delivery location
     */
    private double lat;
    
    /**
     * Longitude of delivery location
     */
    private double lon;
    
    /**
     * Service time in seconds (time required to complete delivery at this location)
     */
    private long serviceTime; // in seconds
    
    /**
     * Priority of the order (0 = urgent, higher = less urgent)
     * P0 parcels (priority 0) should be prioritized
     */
    private int priority;
    
    /**
     * Zone ID for zone-based filtering
     */
    private String zoneId;
    
    /**
     * Delivery address ID (all parcels in same assignment must have same deliveryAddressId)
     */
    private String deliveryAddressId;
}
