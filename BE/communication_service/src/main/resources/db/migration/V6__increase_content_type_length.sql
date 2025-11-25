-- Increase content_type column length to support longer enum values like DELIVERY_COMPLETED
-- DELIVERY_COMPLETED is 17 characters, so we set length to 50 for safety

ALTER TABLE messages
    MODIFY COLUMN content_type VARCHAR(50) NOT NULL COMMENT 'Message content type: TEXT, IMAGE, INTERACTIVE_PROPOSAL, DELIVERY_COMPLETED';
