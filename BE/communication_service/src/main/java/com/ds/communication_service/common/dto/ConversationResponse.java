package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String partnerAvatar; 
}
