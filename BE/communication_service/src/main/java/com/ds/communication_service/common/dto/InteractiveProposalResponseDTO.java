package com.ds.communication_service.common.dto;

import java.util.UUID;

import com.ds.communication_service.app_context.models.InteractiveProposal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractiveProposalResponseDTO {
    private UUID id;
    private String type; // "CONFIRM_REFUSAL"
    private String status; // "PENDING", "ACCEPTED"
    private String proposerId;
    private String recipientId;
    private String data; // Dữ liệu JSON (dạng String)

    private String actionType; // Ví dụ: "ACCEPT_DECLINE", "DATE_PICKER"
    private String resultData; // Ví dụ: "2025-11-10"
    private UUID sessionId; // ID của delivery session liên quan

    public static InteractiveProposalResponseDTO from (InteractiveProposal p) {
        if (p == null) {
            return null;
        }
        return InteractiveProposalResponseDTO.builder()
            .id(p.getId())
            .data(p.getData())
            .proposerId(p.getProposerId())
            .recipientId(p.getRecipientId())
            .status(p.getStatus().name())
            .type(p.getType().name())
            .actionType(p.getActionType().name())
            .resultData(p.getResultData())
            .sessionId(p.getSessionId())
            .build();
    }
}
