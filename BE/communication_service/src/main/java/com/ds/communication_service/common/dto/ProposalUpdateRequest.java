package com.ds.communication_service.common.dto;

import java.util.UUID;

import com.ds.communication_service.common.enums.ProposalStatus;

public record ProposalUpdateRequest (
    UUID proposalId,
    ProposalStatus newStatus,
    UUID conversationId,
    String resultData
){}