package com.ds.deliveryapp.clients.res;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProposalTypeConfig {
    private UUID id;
    private String type;
    private String requiredRole;
    private String description;
    private String creationActionType;
    private String responseActionType;

    private Long defaultTimeoutMinutes;
}
