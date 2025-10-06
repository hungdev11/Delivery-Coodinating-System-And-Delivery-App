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
   * Get all settings
   */
  async getAllSettings(): Promise<SettingModel[]> {
    try {
      const response: AxiosResponse<SystemSettingDto[]> = await this.client.get('/api/v1/settings');
      return response.data.map(setting => new SettingModel(setting));
    } catch (error) {
      throw new Error(`Failed to get all settings: ${error}`);
    }
  }

  /**
   * Get setting by key
   */
  async getSettingByKey(key: string): Promise<SettingModel> {
    try {
      const response: AxiosResponse<SystemSettingDto> = await this.client.get(`/api/v1/settings/${key}`);
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to get setting by key "${key}": ${error}`);
    }
  }

  /**
   * Get setting value by key (convenience method)
   */
  async getSettingValue(key: string): Promise<string> {
    try {
      const response: AxiosResponse<string> = await this.client.get(`/api/v1/settings/${key}/value`);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get setting value for key "${key}": ${error}`);
    }
  }

  /**
   * Get setting value by key with default fallback
   */
  async getSettingValueOrDefault(key: string, defaultValue: string): Promise<string> {
    try {
      return await this.getSettingValue(key);
    } catch (error) {
      console.warn(`Setting "${key}" not found, using default value: ${defaultValue}`);
      return defaultValue;
    }
  }

  /**
   * Get settings by group
   */
  async getSettingsByGroup(group: string): Promise<SettingModel[]> {
    try {
      const response: AxiosResponse<SystemSettingDto[]> = await this.client.get(`/api/v1/settings/group/${group}`);
      return response.data.map(setting => new SettingModel(setting));
    } catch (error) {
      throw new Error(`Failed to get settings by group "${group}": ${error}`);
    }
  }

  /**
   * Get setting by key and group
   */
  async getSettingByKeyAndGroup(key: string, group: string): Promise<SettingModel> {
    try {
      const response: AxiosResponse<SystemSettingDto> = await this.client.get(`/api/v1/settings/group/${group}/key/${key}`);
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to get setting by key "${key}" and group "${group}": ${error}`);
    }
  }

  /**
   * Get setting value by key and group
   */
  async getSettingValueByKeyAndGroup(key: string, group: string): Promise<string> {
    try {
      const response: AxiosResponse<string> = await this.client.get(`/api/v1/settings/group/${group}/key/${key}/value`);
      return response.data;
    } catch (error) {
      throw new Error(`Failed to get setting value for key "${key}" and group "${group}": ${error}`);
    }
  }

  /**
   * Get setting value by key and group with default fallback
   */
  async getSettingValueByKeyAndGroupOrDefault(key: string, group: string, defaultValue: string): Promise<string> {
    try {
      return await this.getSettingValueByKeyAndGroup(key, group);
    } catch (error) {
      console.warn(`Setting "${key}" in group "${group}" not found, using default value: ${defaultValue}`);
      return defaultValue;
    }
  }

  /**
   * Get settings by level
   */
  async getSettingsByLevel(level: SettingLevel): Promise<SettingModel[]> {
    try {
      const response: AxiosResponse<SystemSettingDto[]> = await this.client.get(`/api/v1/settings/level/${level}`);
      return response.data.map(setting => new SettingModel(setting));
    } catch (error) {
      throw new Error(`Failed to get settings by level "${level}": ${error}`);
    }
  }

  /**
   * Search settings
   */
  async searchSettings(query: string): Promise<SettingModel[]> {
    try {
      const response: AxiosResponse<SystemSettingDto[]> = await this.client.get('/api/v1/settings/search', {
        params: { q: query }
      });
      return response.data.map(setting => new SettingModel(setting));
    } catch (error) {
      throw new Error(`Failed to search settings with query "${query}": ${error}`);
    }
  }

  /**
   * Create a new setting
   */
  async createSetting(request: CreateSettingRequest, userId?: string): Promise<SettingModel> {
    try {
      const headers = userId ? { 'X-User-Id': userId } : {};
      const response: AxiosResponse<SystemSettingDto> = await this.client.post('/api/v1/settings', request, { headers });
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to create setting "${request.key}": ${error}`);
    }
  }

  /**
   * Update a setting
   */
  async updateSetting(key: string, request: UpdateSettingRequest, userId?: string): Promise<SettingModel> {
    try {
      const headers = userId ? { 'X-User-Id': userId } : {};
      const response: AxiosResponse<SystemSettingDto> = await this.client.put(`/api/v1/settings/${key}`, request, { headers });
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to update setting "${key}": ${error}`);
    }
  }

  /**
   * Update a setting by key and group
   */
  async updateSettingByKeyAndGroup(key: string, group: string, request: UpdateSettingRequest, userId?: string): Promise<SettingModel> {
    try {
      const headers = userId ? { 'X-User-Id': userId } : {};
      const response: AxiosResponse<SystemSettingDto> = await this.client.put(
        `/api/v1/settings/group/${group}/key/${key}`,
        request,
        { headers }
      );
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to update setting "${key}" in group "${group}": ${error}`);
    }
  }

  /**
   * Create or update a setting by key and group (upsert)
   */
  async createOrUpdateSettingByKeyAndGroup(key: string, group: string, request: CreateSettingRequest): Promise<SettingModel> {
    try {
      const response: AxiosResponse<SystemSettingDto> = await this.client.put(
        `/api/v1/settings/group/${group}/key/${key}/upsert`,
        request
      );
      return new SettingModel(response.data);
    } catch (error) {
      throw new Error(`Failed to create or update setting "${key}" in group "${group}": ${error}`);
    }
  }

  /**
   * Delete a setting
   */
  async deleteSetting(key: string): Promise<void> {
    try {
      await this.client.delete(`/api/v1/settings/${key}`);
    } catch (error) {
      throw new Error(`Failed to delete setting "${key}": ${error}`);
    }
  }

  /**
   * Check if setting exists by key
   */
  async settingExists(key: string): Promise<boolean> {
    try {
      await this.getSettingByKey(key);
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Check if setting exists by key and group
   */
  async settingExistsByKeyAndGroup(key: string, group: string): Promise<boolean> {
    try {
      await this.getSettingByKeyAndGroup(key, group);
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
