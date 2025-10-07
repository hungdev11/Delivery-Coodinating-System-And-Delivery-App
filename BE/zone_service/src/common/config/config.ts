/**
 * Application Configuration
 * Centralized configuration with environment variable fallbacks
 */

export const config = {
  // Server Configuration
  server: {
    port: parseInt(process.env.PORT || '21503'),
    nodeEnv: process.env.NODE_ENV || 'development',
  },

  // Database Configuration
  database: {
    url: process.env.ZONE_DB_CONNECTION || 
         `mysql://${process.env.DB_USERNAME || 'root'}:${process.env.DB_PASSWORD || 'root'}@${process.env.DB_HOST || 'localhost'}:${process.env.DB_PORT || '3306'}/${process.env.ZONE_DB_NAME || 'ds_zone_service'}`,
  },

  // Settings Service Configuration
  settings: {
    serviceUrl: process.env.SETTINGS_SERVICE_URL || 'http://localhost:21502',
    timeout: parseInt(process.env.SETTINGS_SERVICE_TIMEOUT || '10000'),
  },

  // CORS Configuration
  cors: {
    origins: process.env.CORS_ORIGINS?.split(',').map(origin => origin.trim()) || [
      'http://localhost:3000',
      'http://localhost:5173',
      'http://localhost:8080',
    ],
  },

  // Kafka Configuration (Optional)
  kafka: {
    brokers: process.env.KAFKA_BROKERS?.split(',') || [],
    clientId: 'zone-service',
    groupId: process.env.KAFKA_GROUP_ID || 'zone-service-group',
  },

  // Logging Configuration
  logging: {
    level: process.env.LOG_LEVEL || 'info',
  },
} as const;

/**
 * Validate required configuration
 */
export function validateConfig() {
  const errors: string[] = [];

  // Validate database URL format
  if (!config.database.url.startsWith('mysql://')) {
    errors.push('ZONE_DB_CONNECTION must be a valid MySQL connection string');
  }

  // Validate port
  if (isNaN(config.server.port) || config.server.port < 1 || config.server.port > 65535) {
    errors.push('PORT must be a valid number between 1 and 65535');
  }

  if (errors.length > 0) {
    throw new Error(`Configuration validation failed:\n${errors.join('\n')}`);
  }
}
