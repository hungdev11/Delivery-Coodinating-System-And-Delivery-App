package com.ds.gateway.common.entities.dto.communicate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enriched conversation response with additional user, parcel, and session information
 * This DTO is returned by API Gateway after aggregating data from multiple services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedConversationResponse {
    // Basic conversation info
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String partnerUsername;
    private String partnerAvatar;
    
    // Last message info
    private String lastMessageContent;
    private String lastMessageTime;
    private Integer unreadCount;
    
    // Partner details (from User Service)
    private String partnerEmail;
    private String partnerPhone;
    private String partnerFirstName;
    private String partnerLastName;
    
    // Parcel info (if applicable)
    // For CLIENT: current parcel being delivered by this shipper
    // For SHIPPER: current parcel being delivered to this customer
    private String currentParcelId;
    private String currentParcelCode;
    private String currentParcelStatus;
    
    // Session info (if applicable)
    // For SHIPPER: info about active session
    private String activeSessionId;
    private String activeSessionStatus;
    
    // Online status (future)
    private Boolean isOnline;
}
