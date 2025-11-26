-- Flyway V2: Create delivery_mans table for shipper control
-- Creates table: delivery_mans with relationship to users

CREATE TABLE IF NOT EXISTS delivery_mans (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  vehicle_type VARCHAR(50) NOT NULL,
  capacity_kg DOUBLE NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  CONSTRAINT pk_delivery_mans PRIMARY KEY (id),
  CONSTRAINT uq_delivery_mans_user_id UNIQUE (user_id),
  CONSTRAINT fk_delivery_mans_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_delivery_mans_user_id ON delivery_mans(user_id);
