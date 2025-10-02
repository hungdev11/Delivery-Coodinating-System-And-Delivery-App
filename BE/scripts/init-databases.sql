-- Initialize databases for Delivery System Services
CREATE DATABASE IF NOT EXISTS ds_user_service;
CREATE DATABASE IF NOT EXISTS ds_settings_service;

-- Create user for services (optional, can use root for development)
-- CREATE USER 'dss_user'@'%' IDENTIFIED BY 'dss_password';
-- GRANT ALL PRIVILEGES ON ds_user_service.* TO 'dss_user'@'%';
-- GRANT ALL PRIVILEGES ON ds_settings_service.* TO 'dss_user'@'%';
-- GRANT ALL PRIVILEGES ON ds_order_service.* TO 'dss_user'@'%';
-- GRANT ALL PRIVILEGES ON ds_delivery_service.* TO 'dss_user'@'%';
-- FLUSH PRIVILEGES;

-- Use ds_user_service for Keycloak
CREATE DATABASE IF NOT EXISTS keycloak;

-- Show created databases
SHOW DATABASES;
