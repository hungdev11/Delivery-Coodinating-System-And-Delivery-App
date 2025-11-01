package com.ds.deliveryapp.clients.res;

import lombok.Data;

@Data
public class Conversation {
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String partnerAvatar;
}
