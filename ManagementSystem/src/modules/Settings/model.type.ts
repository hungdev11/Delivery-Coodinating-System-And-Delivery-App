/**
 * Settings Module Types
 */

import type { IApiResponse } from '@/common/types'

/**
 * Setting Type
 */
export type SettingType = 'STRING' | 'INTEGER' | 'BOOLEAN' | 'DOUBLE' | 'JSON'

/**
 * Display Mode
 */
export type DisplayMode = 'TEXT' | 'NUMBER' | 'BOOLEAN' | 'SELECT' | 'TEXTAREA' | 'JSON'

/**
 * System Setting DTO
 */
export class SystemSettingDto {
  key: string
  group: string
  description?: string
  type: SettingType
  value: string
  level: number
  isReadOnly: boolean
  displayMode: DisplayMode
  createdAt: string
  updatedAt: string
  updatedBy?: string

  constructor(data: SystemSettingDto) {
    this.key = data.key
    this.group = data.group
    this.description = data.description
    this.type = data.type
    this.value = data.value
    this.level = data.level
    this.isReadOnly = data.isReadOnly
    this.displayMode = data.displayMode
    this.createdAt = data.createdAt
    this.updatedAt = data.updatedAt
    this.updatedBy = data.updatedBy
  }

  get displayName(): string {
    return this.description || this.key
  }

  get isEditable(): boolean {
    return !this.isReadOnly
  }
}

/**
 * Upsert Setting Request
 */
export class UpsertSettingRequest {
  key: string
  group: string
  description?: string
  type: SettingType
  value: string
  level?: number
  isReadOnly?: boolean
  displayMode?: DisplayMode

  constructor(data: UpsertSettingRequest) {
    this.key = data.key
    this.group = data.group
    this.description = data.description
    this.type = data.type
    this.value = data.value
    this.level = data.level
    this.isReadOnly = data.isReadOnly
    this.displayMode = data.displayMode
  }
}

/**
 * Settings API Responses
 */
export type GetSettingsResponse = IApiResponse<SystemSettingDto[]>
export type GetSettingResponse = IApiResponse<SystemSettingDto>
export type UpsertSettingResponse = IApiResponse<SystemSettingDto>
export type DeleteSettingResponse = IApiResponse<null>
