package com.ds.communication_service.common.enums;

/**
 * Ticket status lifecycle
 */
public enum TicketStatus {
    /**
     * Ticket created, waiting for admin action
     */
    OPEN,
    
    /**
     * Admin is handling the ticket (e.g., reassigning, investigating)
     */
    IN_PROGRESS,
    
    /**
     * Ticket resolved successfully
     */
    RESOLVED,
    
    /**
     * Ticket cancelled (e.g., false report, duplicate)
     */
    CANCELLED
}
