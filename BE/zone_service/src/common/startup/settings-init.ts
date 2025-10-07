/**
 * Settings Initialization
 * Initialize required settings keys if they don't exist
 */

import { logger } from '../logger/logger.service';
import { createSettingService, ZONE_SETTING_KEYS, ZONE_SETTING_GROUPS } from '../modules/setting';
import { SettingLevel, SettingType } from '../modules/setting/setting.model';

interface SettingDefinition {
  key: string;
  group: string;
  description: string;
  type: SettingType;
  value: string;
  level: SettingLevel;
}

/**
 * Default settings to initialize
 */
const DEFAULT_SETTINGS: SettingDefinition[] = [
  {
    key: ZONE_SETTING_KEYS.DEFAULT_ZONE_RADIUS,
    group: ZONE_SETTING_GROUPS.ZONE_CONFIG,
    description: 'Default radius for zones in meters',
    type: SettingType.INTEGER,
    value: '1000',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.MAX_ZONE_RADIUS,
    group: ZONE_SETTING_GROUPS.ZONE_CONFIG,
    description: 'Maximum allowed radius for zones in meters',
    type: SettingType.INTEGER,
    value: '50000',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.MIN_ZONE_RADIUS,
    group: ZONE_SETTING_GROUPS.ZONE_CONFIG,
    description: 'Minimum allowed radius for zones in meters',
    type: SettingType.INTEGER,
    value: '100',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.DEFAULT_CENTER_LATITUDE,
    group: ZONE_SETTING_GROUPS.CENTER_CONFIG,
    description: 'Default latitude for center location',
    type: SettingType.DECIMAL,
    value: '10.762622',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.DEFAULT_CENTER_LONGITUDE,
    group: ZONE_SETTING_GROUPS.CENTER_CONFIG,
    description: 'Default longitude for center location',
    type: SettingType.DECIMAL,
    value: '106.660172',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.ZONE_CACHE_TTL,
    group: ZONE_SETTING_GROUPS.PERFORMANCE,
    description: 'Cache TTL for zone data in seconds',
    type: SettingType.INTEGER,
    value: '3600',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.MAX_ZONES_PER_REQUEST,
    group: ZONE_SETTING_GROUPS.PERFORMANCE,
    description: 'Maximum number of zones to return per request',
    type: SettingType.INTEGER,
    value: '100',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.ENABLE_ZONE_VALIDATION,
    group: ZONE_SETTING_GROUPS.FEATURE_FLAGS,
    description: 'Enable zone polygon validation',
    type: SettingType.BOOLEAN,
    value: 'true',
    level: SettingLevel.SYSTEM,
  },
  {
    key: ZONE_SETTING_KEYS.ENABLE_GEOCODING,
    group: ZONE_SETTING_GROUPS.FEATURE_FLAGS,
    description: 'Enable geocoding services',
    type: SettingType.BOOLEAN,
    value: 'false',
    level: SettingLevel.SYSTEM,
  },
];

/**
 * Initialize settings
 */
export async function initializeSettings(): Promise<void> {
  const settingsUrl = process.env.SETTINGS_SERVICE_URL;

  if (!settingsUrl) {
    logger.warn('SETTINGS_SERVICE_URL not configured, skipping settings initialization');
    return;
  }

  try {
    logger.info('Initializing settings...');
    const settingsService = createSettingService();

    let created = 0;
    let skipped = 0;

    for (const setting of DEFAULT_SETTINGS) {
      try {
        const exists = await settingsService.settingExists(setting.key);

        if (!exists) {
          await settingsService.createSetting({
            key: setting.key,
            group: setting.group,
            description: setting.description,
            type: setting.type,
            value: setting.value,
            level: setting.level,
          });
          created++;
          logger.info(`Setting created: ${setting.key}`);
        } else {
          skipped++;
          logger.debug(`Setting already exists: ${setting.key}`);
        }
      } catch (error) {
        logger.error(`Failed to initialize setting: ${setting.key}`, { error });
      }
    }

    logger.info('Settings initialization completed', { created, skipped, total: DEFAULT_SETTINGS.length });
  } catch (error) {
    logger.error('Failed to initialize settings', { error });
    throw error;
  }
}
