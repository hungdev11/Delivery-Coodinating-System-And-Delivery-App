-- Flyway V1: Initialize schema for Settings Service
-- Creates table: system_settings

CREATE TABLE IF NOT EXISTS system_settings (
  setting_key VARCHAR(100) NOT NULL,
  setting_group VARCHAR(50) NOT NULL,
  description VARCHAR(500),
  value_type VARCHAR(20) NOT NULL,
  setting_value TEXT,
  level INT NOT NULL,
  is_read_only TINYINT(1) NOT NULL,
  display_mode VARCHAR(20) NOT NULL DEFAULT 'TEXT',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  updated_by VARCHAR(100),
  CONSTRAINT pk_system_settings PRIMARY KEY (setting_key)
);

CREATE INDEX idx_setting_group ON system_settings (setting_group);
CREATE INDEX idx_setting_level ON system_settings (level);
