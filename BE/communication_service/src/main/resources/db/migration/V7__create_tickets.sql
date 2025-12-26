-- Create tickets table for delivery issue tracking
CREATE TABLE IF NOT EXISTS tickets (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(50) NOT NULL COMMENT 'Ticket type: DELIVERY_FAILED or NOT_RECEIVED',
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN' COMMENT 'Ticket status: OPEN, IN_PROGRESS, RESOLVED, CANCELLED',
    parcel_id VARCHAR(36) NOT NULL COMMENT 'Parcel ID from parcel-service',
    assignment_id VARCHAR(36) COMMENT 'Delivery Assignment ID from session-service (optional)',
    reporter_id VARCHAR(255) NOT NULL COMMENT 'User who created the ticket (client or shipper)',
    assigned_admin_id VARCHAR(255) COMMENT 'Admin who is handling the ticket',
    description TEXT COMMENT 'Description/reason for the ticket',
    resolution_notes TEXT COMMENT 'Admin resolution notes',
    action_taken VARCHAR(50) COMMENT 'Action taken: REASSIGN, CANCEL, RESOLVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP COMMENT 'Timestamp when ticket was resolved',
    
    INDEX idx_parcel_id (parcel_id),
    INDEX idx_assignment_id (assignment_id),
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_assigned_admin_id (assigned_admin_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
