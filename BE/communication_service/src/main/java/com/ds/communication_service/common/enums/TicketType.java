package com.ds.communication_service.common.enums;

/**
 * Ticket types for delivery issues
 */
public enum TicketType {
    /**
     * Shipper reports delivery failed (e.g., recipient not available, wrong address)
     */
    DELIVERY_FAILED,
    
    /**
     * Client reports not received parcel
     */
    NOT_RECEIVED
}
