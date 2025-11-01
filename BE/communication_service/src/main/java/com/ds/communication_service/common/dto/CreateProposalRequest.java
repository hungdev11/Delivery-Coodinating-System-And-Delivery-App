package com.ds.communication_service.common.dto;

import java.util.Collection;
import java.util.UUID;

import com.ds.communication_service.common.enums.ProposalType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProposalRequest {
    private UUID conversationId;
    private String recipientId;
    private ProposalType type;
    private String data; // Dữ liệu JSON
    private String fallbackContent; // Nội dung text hiển thị trên message

    private String senderId;
    private Collection<String> senderRoles;
}

