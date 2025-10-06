/**
 * Type definitions for Settings Service API
 */

export enum SettingLevel {
  SYSTEM = 'SYSTEM',
  USER = 'USER',
  GROUP = 'GROUP'
}

export enum SettingType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  BOOLEAN = 'BOOLEAN',
  DECIMAL = 'DECIMAL',
  JSON = 'JSON'
}

export enum DisplayMode {
  TEXT = 'TEXT',
  PASSWORD = 'PASSWORD',
  TEXTAREA = 'TEXTAREA',
  SELECT = 'SELECT',
  CHECKBOX = 'CHECKBOX',
  NUMBER = 'NUMBER'
}

export interface SystemSettingDto {
  key: string;
  group: string;
  description?: string;
  type: SettingType;
  value: string;
  level: SettingLevel;
  isReadOnly?: boolean;
  displayMode?: DisplayMode;
  createdAt: string;
  updatedAt: string;
  updatedBy?: string;
}

export interface CreateSettingRequest {
  key: string;
  group: string;
  description?: string;
  type: SettingType;
  value: string;
  level: SettingLevel;
  isReadOnly?: boolean;
  displayMode?: DisplayMode;
}

export interface UpdateSettingRequest {
  description?: string;
  type?: SettingType;
  value?: string;
  displayMode?: DisplayMode;
}

export interface SettingsApiResponse<T> {
  data: T;
  status: number;
  message?: string;
}

export interface SettingsConfig {
  baseUrl: string;
  timeout?: number;
  headers?: Record<string, string>;
}

/**
 * Setting Model class with utility methods
 */
export class SettingModel {
  private setting: SystemSettingDto;

  constructor(setting: SystemSettingDto) {
    this.setting = setting;
  }

  get key(): string {
    return this.setting.key;
  }

  get group(): string {
    return this.setting.group;
  }

  get value(): string {
    return this.setting.value;
  }

  get description(): string | undefined {
    return this.setting.description;
  }

  get type(): SettingType {
    return this.setting.type;
  }

  get level(): SettingLevel {
    return this.setting.level;
  }

  get isReadOnly(): boolean {
    return this.setting.isReadOnly ?? false;
  }

  /**
   * Get value as boolean
   */
  getValueAsBoolean(): boolean {
    return this.setting.value.toLowerCase() === 'true';
  }

  /**
   * Get value as number
   */
  getValueAsNumber(): number {
    const num = parseFloat(this.setting.value);
    if (isNaN(num)) {
      throw new Error(`Cannot convert value "${this.setting.value}" to number for setting "${this.setting.key}"`);
    }
    return num;
  }

  /**
   * Get value as integer
   */
  getValueAsInteger(): number {
    const num = parseInt(this.setting.value, 10);
    if (isNaN(num)) {
      throw new Error(`Cannot convert value "${this.setting.value}" to integer for setting "${this.setting.key}"`);
    }
    return num;
  }

  /**
   * Get value as JSON object
   */
  getValueAsJson<T = any>(): T {
    try {
      return JSON.parse(this.setting.value);
    } catch (error) {
      throw new Error(`Cannot parse JSON value for setting "${this.setting.key}": ${error}`);
    }
  }

  /**
   * Check if setting is system level
   */
  isSystemLevel(): boolean {
    return this.setting.level === SettingLevel.SYSTEM;
  }

  /**
   * Check if setting is user level
   */
  isUserLevel(): boolean {
    return this.setting.level === SettingLevel.USER;
  }

  /**
   * Check if setting is group level
   */
  isGroupLevel(): boolean {
    return this.setting.level === SettingLevel.GROUP;
  }

  /**
   * Get raw setting data
   */
  toJson(): SystemSettingDto {
    return { ...this.setting };
  }

  /**
   * Convert to string representation
   */
  toString(): string {
    return `Setting(key=${this.setting.key}, group=${this.setting.group}, value=${this.setting.value})`;
  }
}
