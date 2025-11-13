-- Create notifications table for in-app notifications
-- Supports various notification types and real-time delivery via WebSocket

CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY COMMENT 'Notification UUID',
    user_id VARCHAR(36) NOT NULL COMMENT 'User ID who receives the notification',
    type VARCHAR(50) NOT NULL COMMENT 'Notification type: NEW_MESSAGE, NEW_PROPOSAL, etc.',
    title VARCHAR(255) NOT NULL COMMENT 'Notification title',
    message TEXT NOT NULL COMMENT 'Notification message/body',
    data TEXT COMMENT 'Optional JSON data payload',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether notification has been read',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When notification was created',
    read_at TIMESTAMP NULL COMMENT 'When notification was read',
    action_url VARCHAR(512) COMMENT 'Optional action URL/link',
    INDEX idx_notifications_user_id (user_id),
    INDEX idx_notifications_read (is_read),
    INDEX idx_notifications_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='In-app notifications for users';
