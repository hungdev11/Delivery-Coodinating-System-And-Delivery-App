package com.ds.communication_service.common.dto;

import lombok.Data;

@Data
public class ConversationResponse {
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String partnerAvatar; 
    private MessageResponse lastMessage;
}
