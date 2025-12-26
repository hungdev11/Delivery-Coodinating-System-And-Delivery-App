package com.ds.session.session_service.business.v1.services.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a Task (assigned order) in the VRP solution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    /**
     * Order ID (Parcel ID)
     */
    private String orderId;
    
    /**
     * Sequence index in the route (0 = first stop, 1 = second stop, etc.)
     */
    private int sequenceIndex;
    
    /**
     * Estimated arrival time at this location
     */
    private LocalDateTime estimatedArrivalTime;
    
    /**
     * Travel time from previous stop in seconds
     */
    private Long travelTimeFromPreviousStop; // in seconds
}
