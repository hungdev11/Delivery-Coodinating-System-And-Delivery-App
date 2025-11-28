package com.ds.deliveryapp.clients.res;

import java.util.List;

import lombok.Data;

@Data
public class Conversation {
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String partnerAvatar;
    
    // Optional fields that might be added in backend response
    private String lastMessageContent;
    private String lastMessageTime;
    private Integer unreadCount;
    
    // Parcel info if conversation is related to a specific parcel
    private String currentParcelId;
    private String currentParcelCode;
    
    // Online status (maps to "isOnline" from backend EnrichedConversationResponse)
    private Boolean partnerOnline;
    private Boolean isOnline;
    
    // Messages included in response (when includeMessages=true)
    private List<Message> messages;
    
    // Helper method to get online status (checks both fields for compatibility)
    public Boolean getPartnerOnline() {
        if (partnerOnline != null) return partnerOnline;
        return isOnline;
    }
}
