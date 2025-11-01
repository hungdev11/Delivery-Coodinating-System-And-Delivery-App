package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.ProposalType;

import lombok.Data;

@Data
public class ProposalConfigDTO {

    private ProposalType type;

    private String requiredRole; // Ví dụ: "ROLE_SHIPPER", "ROLE_CUSTOMER"

    private Long defaultTimeoutMinutes;
    
    private String description;
}
