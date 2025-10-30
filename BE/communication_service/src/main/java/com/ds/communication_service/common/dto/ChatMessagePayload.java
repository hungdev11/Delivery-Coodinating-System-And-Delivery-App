package com.ds.communication_service.common.dto;

import lombok.Data;

@Data
public class ChatMessagePayload {
    private String content;
    private String recipientId;
}
