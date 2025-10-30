package com.ds.deliveryapp.clients.req;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessagePayload {
    private String content;
    private String recipientId;
}
