-- Create tickets table for managing delivery issues
-- Links with Parcel, DeliveryAssignment, and User (client/shipper)

CREATE TABLE IF NOT EXISTS tickets (
    id VARCHAR(36) PRIMARY KEY COMMENT 'Ticket UUID',
    parcel_id VARCHAR(36) NOT NULL COMMENT 'Reference to parcel-service.parcels.id',
    delivery_assignment_id VARCHAR(36) COMMENT 'Reference to session-service.delivery_assignments.id',
    user_id VARCHAR(36) NOT NULL COMMENT 'Reference to user-service.users.id (client or shipper)',
    type VARCHAR(50) NOT NULL COMMENT 'Ticket type: DELIVERY_FAILED, NOT_RECEIVED',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'Ticket status: PENDING, RESOLVED, CANCELLED',
    description TEXT COMMENT 'Description of the issue',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When ticket was created',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'When ticket was last updated',
    resolved_at TIMESTAMP NULL COMMENT 'When ticket was resolved',
    resolved_by VARCHAR(36) COMMENT 'Admin user ID who resolved the ticket (reference to user-service.users.id)',
    resolution_notes TEXT COMMENT 'Additional notes from admin when resolving',
    INDEX idx_tickets_parcel_id (parcel_id),
    INDEX idx_tickets_assignment_id (delivery_assignment_id),
    INDEX idx_tickets_user_id (user_id),
    INDEX idx_tickets_status (status),
    INDEX idx_tickets_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tickets for managing delivery issues (delivery failed, not received, etc.)';
