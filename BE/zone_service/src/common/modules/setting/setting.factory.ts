import { SettingService } from './setting.service';
import type { SettingsConfig } from './setting.model';
import { getSettingsConfig, validateSettingsConfig } from './setting.config';

/**
 * Factory function to create SettingService instance
 */
export function createSettingService(config?: Partial<SettingsConfig>): SettingService {
  // Merge provided config with default config
  const defaultConfig = getSettingsConfig();
  const finalConfig: SettingsConfig = {
    ...defaultConfig,
    ...config,
    headers: {
      ...defaultConfig.headers,
      ...config?.headers
    }
  };

  // Validate configuration
  validateSettingsConfig(finalConfig);

  // Create and return service instance
  return new SettingService(finalConfig);
}

/**
 * Create SettingService with environment-specific configuration
 */
export function createSettingServiceForEnvironment(environment?: string): SettingService {
  const config = getSettingsConfig(environment);
  return new SettingService(config);
}

/**
 * Create SettingService with custom base URL
 */
export function createSettingServiceWithUrl(baseUrl: string, additionalConfig?: Partial<SettingsConfig>): SettingService {
  return createSettingService({
    baseUrl,
    ...additionalConfig
  });
}
