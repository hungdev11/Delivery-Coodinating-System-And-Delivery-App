-- Flyway V3: Create user_addresses table
-- Stores user's saved addresses (destinations) with notes, tags, and primary flag

CREATE TABLE IF NOT EXISTS user_addresses (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  destination_id VARCHAR(36) NOT NULL,
  note VARCHAR(500),
  tag VARCHAR(50),
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  CONSTRAINT pk_user_addresses PRIMARY KEY (id),
  CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX idx_user_addresses_destination_id ON user_addresses(destination_id);
CREATE INDEX idx_user_addresses_is_primary ON user_addresses(user_id, is_primary);
