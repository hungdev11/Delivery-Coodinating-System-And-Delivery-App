import type { SettingsConfig } from './setting.model';

/**
 * Default configuration for Settings Service
 */
export const DEFAULT_SETTINGS_CONFIG: SettingsConfig = {
  baseUrl: process.env.SETTINGS_SERVICE_URL || 'http://localhost:8080',
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
    baseUrl: 'http://localhost:8080'
  },
  staging: {
    ...DEFAULT_SETTINGS_CONFIG,
    baseUrl: process.env.SETTINGS_SERVICE_URL || 'http://settings-service-staging:8080'
  },
  production: {
    ...DEFAULT_SETTINGS_CONFIG,
    baseUrl: process.env.SETTINGS_SERVICE_URL || 'http://settings-service:8080',
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
  DEFAULT_ZONE_RADIUS: 'default_zone_radius',
  MAX_ZONE_RADIUS: 'max_zone_radius',
  MIN_ZONE_RADIUS: 'min_zone_radius',
  
  // Center configuration
  DEFAULT_CENTER_LATITUDE: 'default_center_latitude',
  DEFAULT_CENTER_LONGITUDE: 'default_center_longitude',
  
  // Performance settings
  ZONE_CACHE_TTL: 'zone_cache_ttl',
  MAX_ZONES_PER_REQUEST: 'max_zones_per_request',
  
  // Feature flags
  ENABLE_ZONE_VALIDATION: 'enable_zone_validation',
  ENABLE_GEOCODING: 'enable_geocoding',
  
  // Integration settings
  MAPS_API_KEY: 'maps_api_key',
  GEOCODING_SERVICE_URL: 'geocoding_service_url'
} as const;

/**
 * Common setting groups used by Zone Service
 */
export const ZONE_SETTING_GROUPS = {
  ZONE_CONFIG: 'zone_config',
  CENTER_CONFIG: 'center_config',
  PERFORMANCE: 'performance',
  FEATURE_FLAGS: 'feature_flags',
  INTEGRATIONS: 'integrations'
} as const;
