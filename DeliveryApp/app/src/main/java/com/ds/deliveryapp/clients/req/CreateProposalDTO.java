package com.ds.deliveryapp.clients.req;

import java.util.Collection;

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
public class CreateProposalDTO {
    private String conversationId;
    private String recipientId;
    private String type; // Ví dụ: "CONFIRM_REFUSAL"
    private String data; // JSON string
    private String fallbackContent;

    private String senderId;
    private Collection<String> senderRoles;
}
