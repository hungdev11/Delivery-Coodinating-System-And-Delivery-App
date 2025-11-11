package com.ds.communication_service.common.dto;

import java.time.LocalDateTime;

import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class MessageResponse {
    private String id;
    private String conversationId;
    private String senderId;
    private String content;
    private LocalDateTime sentAt;
    private ContentType type;
    private MessageStatus status; // Message status: SENT, DELIVERED, READ
    private LocalDateTime deliveredAt; // When message was delivered
    private LocalDateTime readAt; // When message was read
    private InteractiveProposalResponseDTO proposal;
}
