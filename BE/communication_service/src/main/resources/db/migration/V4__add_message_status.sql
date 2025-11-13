-- Add message status tracking fields to messages table
-- Status tracking: SENT → DELIVERED → READ

ALTER TABLE messages
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SENT' COMMENT 'Message status: SENT, DELIVERED, READ',
    ADD COLUMN delivered_at TIMESTAMP NULL COMMENT 'When message was delivered to recipient',
    ADD COLUMN read_at TIMESTAMP NULL COMMENT 'When message was read by recipient';

-- Add index for status queries
CREATE INDEX idx_messages_status ON messages(status);

-- Add index for delivered_at for analytics
CREATE INDEX idx_messages_delivered_at ON messages(delivered_at);

-- Add index for read_at for analytics
CREATE INDEX idx_messages_read_at ON messages(read_at);
