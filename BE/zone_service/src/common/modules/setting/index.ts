/**
 * Settings Module - Zone Service
 * 
 * This module provides integration with the Settings Service API
 * for managing system settings, configurations, and feature flags.
 */

// Export main classes and interfaces
export { SettingService } from './setting.service';
export { SettingModel } from './setting.model';

// Export types and enums
export type {
  SystemSettingDto,
  CreateSettingRequest,
  UpdateSettingRequest,
  SettingsApiResponse,
  SettingsConfig
} from './setting.model';

export {
  SettingLevel,
  SettingType,
  DisplayMode
} from './setting.model';

// Export configuration utilities
export {
  DEFAULT_SETTINGS_CONFIG,
  SETTINGS_CONFIGS,
  getSettingsConfig,
  validateSettingsConfig,
  ZONE_SETTING_KEYS,
  ZONE_SETTING_GROUPS
} from './setting.config';

// Export convenience factory function
export { createSettingService } from './setting.factory';
