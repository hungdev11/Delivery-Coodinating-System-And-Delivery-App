package com.ds.deliveryapp.clients.res;

import com.ds.deliveryapp.enums.ContentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private String sentAt;
    private ContentType type;
    private String status; // Message status: SENT, DELIVERED, READ
    private String deliveredAt; // When message was delivered
    private String readAt; // When message was read
    private InteractiveProposal proposal;
}
