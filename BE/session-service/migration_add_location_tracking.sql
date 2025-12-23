-- Migration: Add location tracking tables and fields
-- Date: 2024

-- Add start location fields to delivery_sessions table
ALTER TABLE delivery_sessions
ADD COLUMN start_location_lat DOUBLE NULL,
ADD COLUMN start_location_lon DOUBLE NULL,
ADD COLUMN start_location_timestamp DATETIME NULL;

-- Create shipper_location_tracking table
CREATE TABLE IF NOT EXISTS shipper_location_tracking (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    delivery_man_id VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    timestamp DATETIME NOT NULL,
    accuracy DOUBLE NULL,
    speed DOUBLE NULL,
    nearest_node_id VARCHAR(36) NULL,
    distance_to_node DOUBLE NULL,
    INDEX idx_session_id (session_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_nearest_node_id (nearest_node_id),
    INDEX idx_session_timestamp (session_id, timestamp)
);

-- Create delivery_confirmation_points table
CREATE TABLE IF NOT EXISTS delivery_confirmation_points (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    assignment_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    confirmed_at DATETIME NOT NULL,
    confirmation_type VARCHAR(20) NOT NULL,
    distance_from_parcel DOUBLE NULL,
    INDEX idx_assignment_id (assignment_id),
    INDEX idx_session_id (session_id),
    INDEX idx_confirmed_at (confirmed_at),
    INDEX idx_confirmation_type (confirmation_type)
);
