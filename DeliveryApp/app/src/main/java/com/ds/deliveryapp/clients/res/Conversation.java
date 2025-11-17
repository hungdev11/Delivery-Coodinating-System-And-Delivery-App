package com.ds.deliveryapp.clients.res;

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
    
    // Online status
    private Boolean partnerOnline;
}
