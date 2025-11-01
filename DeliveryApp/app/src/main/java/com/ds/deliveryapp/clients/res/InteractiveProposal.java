package com.ds.deliveryapp.clients.res;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InteractiveProposal {
    private UUID id;
    private String type; // "CONFIRM_REFUSAL"
    private String status; // "PENDING", "ACCEPTED"
    private String proposerId;
    private String recipientId;
    private String data; // Dữ liệu JSON (dạng String)
    private String resultData;
    private String actionType;
}
