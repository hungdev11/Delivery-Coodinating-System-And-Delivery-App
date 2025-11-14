-- Migration script to add CREATED status to delivery_sessions table
-- Run this script if you encounter "Data truncated for column 'status'" error

-- Option 1: If status column is VARCHAR, just ensure it has enough length
ALTER TABLE delivery_sessions 
MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Option 2: If status column is ENUM, add CREATED to the enum values
-- Uncomment and run this if your column is ENUM type:
-- ALTER TABLE delivery_sessions 
-- MODIFY COLUMN status ENUM('CREATED', 'IN_PROGRESS', 'COMPLETED', 'FAILED') NOT NULL;
