package com.ds.user.common.entities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressDto {
    private String id;
    private String userId;
    private String destinationId;
    private String note;
    private String tag;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Optional: Include destination details if needed
    private DestinationDetails destinationDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationDetails {
        private String id;
        private String name;
        private String addressText;
        private Double lat;
        private Double lon;
    }
}
