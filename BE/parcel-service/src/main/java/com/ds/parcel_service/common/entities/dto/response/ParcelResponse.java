package com.ds.parcel_service.common.entities.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponse {
    private String id;
    private String code;
    private String senderId;
    private String senderName; // Full name from User Service
    private String receiverId;
    private String receiverName; // Full name from User Service
    private String receiverPhoneNumber;
    private DeliveryType deliveryType;
    
    /**
     * UserAddress ID from user-service for sender address
     */
    private String senderAddressId;
    
    /**
     * UserAddress ID from user-service for receiver address
     */
    private String receiverAddressId;
    
    private ParcelStatus status;
    private double weight;
    private BigDecimal value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalTime windowStart;
    private LocalTime windowEnd;
    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    private String confirmationNote;

    private BigDecimal lat;
    private BigDecimal lon;
    
    // Priority and delay fields
    private Integer priority;
    private Boolean isDelayed;
    private LocalDateTime delayedUntil;
    
    // Nested objects for full information (like join/include)
    private UserInfoDto sender;
    private UserInfoDto receiver;
    private AddressInfoDto senderAddress;
    private AddressInfoDto receiverAddress;
    
    /**
     * DTO for user information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private String id;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String phone;
        private String address; // Full address string
    }
    
    /**
     * DTO for address information with coordinates
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInfoDto {
        private String id; // UserAddress ID
        private String userId;
        private String destinationId; // Zone Service destination ID
        private String note;
        private String tag;
        private Boolean isPrimary;
        
        // Coordinates from Zone Service
        private BigDecimal lat;
        private BigDecimal lon;
        private String zoneId;
        
        // Zone information (nested)
        private ZoneInfoDto zone;
    }
    
    /**
     * DTO for zone information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneInfoDto {
        private String id;
        private String code;
        private String name;
        
        // Center information (nested)
        private CenterInfoDto center;
    }
    
    /**
     * DTO for center information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CenterInfoDto {
        private String id;
        private String code;
        private String name;
        private String address;
        private BigDecimal lat;
        private BigDecimal lon;
    }
}
