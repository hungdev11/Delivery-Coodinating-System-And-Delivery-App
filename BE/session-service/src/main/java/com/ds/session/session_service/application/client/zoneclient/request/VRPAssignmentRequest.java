package com.ds.session.session_service.application.client.zoneclient.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for VRP assignment API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VRPAssignmentRequest {
    
    private List<VRPShipperDto> shippers;
    private List<VRPOrderDto> orders;
    private String vehicle; // "car" or "motorbike"
    private String mode; // "v2-full", "v2-rating-only", etc.
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VRPShipperDto {
        private String shipperId;
        private Double lat;
        private Double lon;
        private String shiftStart; // ISO format or HH:mm:ss
        private Double maxSessionTime; // hours
        private Integer capacity;
        private List<String> zoneIds; // Working zone IDs
        private String vehicle; // "car" or "motorbike"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VRPOrderDto {
        private String orderId; // Parcel ID
        private Double lat;
        private Double lon;
        private Integer serviceTime; // seconds
        private Integer priority; // 0 = urgent, higher = less urgent
        private String zoneId; // Zone ID for filtering
        private String deliveryAddressId; // All parcels with same deliveryAddressId should be in same assignment
    }
}
