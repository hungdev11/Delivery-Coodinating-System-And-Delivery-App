import axios, { AxiosInstance, AxiosResponse } from 'axios';
import type {
  SystemSettingDto,
  CreateSettingRequest,
  UpdateSettingRequest,
  SettingsConfig
} from './setting.model';
import { SettingLevel, SettingModel } from './setting.model';

/**
 * Service class for interacting with Settings Service API
 */
export class SettingService {
  private client: AxiosInstance;
  private config: SettingsConfig;

  constructor(config: SettingsConfig) {
    this.config = config;
    this.client = axios.create({
      baseURL: config.baseUrl,
      timeout: config.timeout || 10000,
      headers: {
        'Content-Type': 'application/json',
        ...config.headers
      }
    });

    // Add request interceptor for logging
    this.client.interceptors.request.use(
      (config) => {
        console.log(`[SettingService] ${config.method?.toUpperCase()} ${config.url}`);
        return config;
      },
      (error) => {
        console.error('[SettingService] Request error:', error);
        return Promise.reject(error);
      }
    );

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        console.error('[SettingService] Response error:', error.response?.data || error.message);
        return Promise.reject(error);
      }
    );
  }

  /**
   * Get all settings by group (service identifier)
   * @param group - Service/module identifier (e.g., "zone-service", "user-service")
   */
  async getSettingsByGroup(group: string): Promise<SettingModel[]> {
    try {
      const response: AxiosResponse<SystemSettingDto[]> = await this.client.get(`/api/v1/settings/${group}`);
      return response.data.map(setting => new SettingModel(setting));
    } catch (error) {
      throw new Error(`Failed to get settings by group "${group}": ${error}`);
    }
  }

  /**
   * Get setting by group and key pair
   * @param group - Service/module identifier
   * @param key - Setting key
   */
  async getSetting(group: string, key: string): Promise<SettingModel> {
    try {
      const response: AxiosResponse<SystemSettingDto> = await this.client.get(`/api/v1/settings/${group}/${key}`);
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to get setting "${group}/${key}": ${error}`);
    }
  }

  /**
   * Get setting value only by group and key pair
   * @param group - Service/module identifier
   * @param key - Setting key
   */
  async getSettingValue(group: string, key: string): Promise<string> {
    try {
      const response: AxiosResponse<string> = await this.client.get(`/api/v1/settings/${group}/${key}/value`);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get setting value for "${group}/${key}": ${error}`);
    }
  }

  /**
   * Get setting value with default fallback
   * @param group - Service/module identifier
   * @param key - Setting key
   * @param defaultValue - Default value if setting not found
   */
  async getSettingValueOrDefault(group: string, key: string, defaultValue: string): Promise<string> {
    try {
      return await this.getSettingValue(group, key);
    } catch (error) {
      console.warn(`Setting "${group}/${key}" not found, using default value: ${defaultValue}`);
      return defaultValue;
    }
  }

  /**
   * Upsert (create or update) a setting by group/key pair
   * This is the main API for settings modification
   * @param group - Service/module identifier
   * @param key - Setting key
   * @param request - Setting data
   * @param userId - User identifier for audit trail
   */
  async upsertSetting(group: string, key: string, request: CreateSettingRequest, userId?: string): Promise<SettingModel> {
    try {
      const headers = userId ? { 'X-User-Id': userId } : {};
      const response: AxiosResponse<SystemSettingDto> = await this.client.put(
        `/api/v1/settings/${group}/${key}`,
        request,
        { headers }
      );
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to upsert setting "${group}/${key}": ${error}`);
    }
  }

  /**
   * Delete a setting by group/key pair
   * @param group - Service/module identifier
   * @param key - Setting key
   */
  async deleteSetting(group: string, key: string): Promise<void> {
    try {
      await this.client.delete(`/api/v1/settings/${group}/${key}`);
    } catch (error) {
      throw new Error(`Failed to delete setting "${group}/${key}": ${error}`);
    }
  }

  /**
   * Check if setting exists by group and key pair
   * @param group - Service/module identifier
   * @param key - Setting key
   */
  async settingExists(group: string, key: string): Promise<boolean> {
    try {
      await this.getSetting(group, key);
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Get configuration
   */
  getConfig(): SettingsConfig {
    return { ...this.config };
  }

  /**
   * Update configuration
   */
  updateConfig(newConfig: Partial<SettingsConfig>): void {
    this.config = { ...this.config, ...newConfig };
    this.client.defaults.baseURL = this.config.baseUrl;
    this.client.defaults.timeout = this.config.timeout || 10000;
    if (this.config.headers) {
      this.client.defaults.headers = { ...this.client.defaults.headers, ...this.config.headers };
    }
  }
}
