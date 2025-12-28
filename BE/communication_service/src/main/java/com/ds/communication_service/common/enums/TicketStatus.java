package com.ds.communication_service.common.enums;

/**
 * Ticket status enum
 * Represents the current status of a ticket
 */
public enum TicketStatus {
    /**
     * Ticket is pending resolution
     */
    PENDING,
    
    /**
     * Ticket has been resolved
     */
    RESOLVED,
    
    /**
     * Ticket has been cancelled
     */
    CANCELLED
}
