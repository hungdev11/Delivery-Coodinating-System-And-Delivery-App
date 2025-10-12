-- Flyway V1: Initialize schema for User Service
-- Creates table: users

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) NOT NULL,
  keycloak_id VARCHAR(255),
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255),
  phone VARCHAR(50),
  address VARCHAR(500),
  identity_number VARCHAR(100),
  username VARCHAR(255),
  status INT DEFAULT 1,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NULL,
  CONSTRAINT pk_users PRIMARY KEY (id),
  CONSTRAINT uq_users_email_keycloak_username UNIQUE (email, keycloak_id, username)
);
