import type { SettingsConfig } from './setting.model';

/**
 * Default configuration for Settings Service
 */
export const DEFAULT_SETTINGS_CONFIG: SettingsConfig = {
  baseUrl: process.env.SETTINGS_SERVICE_URL || 'http://localhost:21502',
  timeout: parseInt(process.env.SETTINGS_SERVICE_TIMEOUT || '10000'),
  headers: {
    'User-Agent': 'ZoneService/1.0.0',
    ...(process.env.SETTINGS_SERVICE_API_KEY && {
      'Authorization': `Bearer ${process.env.SETTINGS_SERVICE_API_KEY}`
    })
  }
};

/**
 * Environment-specific configurations
 */
export const SETTINGS_CONFIGS = {
  development: {
    ...DEFAULT_SETTINGS_CONFIG,
    baseUrl: 'http://localhost:21502'
  },
  staging: {
    ...DEFAULT_SETTINGS_CONFIG,
    baseUrl: process.env.SETTINGS_SERVICE_URL || 'http://settings-service-staging:21502'
  },
  production: {
    ...DEFAULT_SETTINGS_CONFIG,
    baseUrl: process.env.SETTINGS_SERVICE_URL || 'http://settings-service:21502',
    timeout: 15000 // Longer timeout for production
  }
};

/**
 * Get configuration based on environment
 */
export function getSettingsConfig(environment?: string): SettingsConfig {
  const env = environment || process.env.NODE_ENV || 'development';
  
  switch (env.toLowerCase()) {
    case 'production':
      return SETTINGS_CONFIGS.production;
    case 'staging':
      return SETTINGS_CONFIGS.staging;
    case 'development':
    default:
      return SETTINGS_CONFIGS.development;
  }
}

/**
 * Validate configuration
 */
export function validateSettingsConfig(config: SettingsConfig): void {
  if (!config.baseUrl) {
    throw new Error('Settings service baseUrl is required');
  }

  if (config.timeout && config.timeout <= 0) {
    throw new Error('Settings service timeout must be greater than 0');
  }

  try {
    new URL(config.baseUrl);
  } catch (error) {
    throw new Error(`Invalid Settings service baseUrl: ${config.baseUrl}`);
  }
}

/**
 * Common setting keys used by Zone Service
 */
export const ZONE_SETTING_KEYS = {
  // Zone configuration
  DEFAULT_ZONE_RADIUS: 'DEFAULT_ZONE_RADIUS',
  MAX_ZONE_RADIUS: 'MAX_ZONE_RADIUS',
  MIN_ZONE_RADIUS: 'MIN_ZONE_RADIUS',
  
  // Center configuration
  DEFAULT_CENTER_LATITUDE: 'DEFAULT_CENTER_LATITUDE',
  DEFAULT_CENTER_LONGITUDE: 'DEFAULT_CENTER_LONGITUDE',
  
  // Performance settings
  ZONE_CACHE_TTL: 'ZONE_CACHE_TTL',
  MAX_ZONES_PER_REQUEST: 'MAX_ZONES_PER_REQUEST',
  
  // Feature flags
  ENABLE_ZONE_VALIDATION: 'ENABLE_ZONE_VALIDATION',
  ENABLE_GEOCODING: 'ENABLE_GEOCODING',
  
  // Integration settings
  MAPS_API_KEY: 'MAPS_API_KEY',
  GEOCODING_SERVICE_URL: 'GEOCODING_SERVICE_URL'
} as const;

/**
 * Common setting groups used by Zone Service
 */
export const ZONE_SETTING_GROUPS = {
  ZONE_CONFIG: 'ZONE_CONFIG',
  CENTER_CONFIG: 'CENTER_CONFIG',
  PERFORMANCE: 'PERFORMANCE',
  FEATURE_FLAGS: 'FEATURE_FLAGS',
  INTEGRATIONS: 'INTEGRATIONS'
} as const;
