package com.ds.deliveryapp.clients.req;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class ProposalUpdateDTO {
    private UUID proposalId;
    private String newStatus; // "ACCEPTED", "DECLINED"
    private UUID conversationId;
    private String resultData;
}
