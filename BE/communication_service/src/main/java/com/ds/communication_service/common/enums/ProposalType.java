package com.ds.communication_service.common.enums;

public enum ProposalType {
    CONFIRM_REFUSAL, 
    POSTPONE_REQUEST,
    DELAY_ORDER_RECEIVE, // User sets delay windows/time for not receiving parcels
    DISPUTE_APPEAL, // Shipper appeals dispute with evidence
    TICKET // Ticket created for delivery issue (DELIVERY_FAILED or NOT_RECEIVED)
}
