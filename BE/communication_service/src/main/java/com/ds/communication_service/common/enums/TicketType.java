package com.ds.communication_service.common.enums;

/**
 * Ticket type enum
 * Represents the type of issue reported in a ticket
 */
public enum TicketType {
    /**
     * Shipper reports that delivery failed
     */
    DELIVERY_FAILED,
    
    /**
     * Client reports that they did not receive the parcel
     */
    NOT_RECEIVED
}
