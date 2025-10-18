/**
 * User Module Types
 *
 * Type definitions for User module
 */

import type { IApiResponse } from '@/common/types'

/**
 * User Data Transfer Object
 */
export class UserDto {
  id: string
  keycloakId?: string
  username: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  address?: string
  identityNumber?: string
  status: UserStatus
  createdAt: string
  updatedAt: string

  constructor(data: UserDto) {
    this.id = data.id
    this.keycloakId = data.keycloakId
    this.username = data.username
    this.email = data.email
    this.firstName = data.firstName
    this.lastName = data.lastName
    this.phone = data.phone
    this.address = data.address
    this.identityNumber = data.identityNumber
    this.status = data.status
    this.createdAt = data.createdAt
    this.updatedAt = data.updatedAt
  }

  get fullName(): string {
    return `${this.firstName} ${this.lastName}`
  }

  get displayStatus(): string {
    const statusMap: Record<UserStatus, string> = {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
      SUSPENDED: 'Suspended',
      PENDING: 'Pending',
    }
    return statusMap[this.status] || this.status
  }
}

/**
 * User Status
 */
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING'

/**
 * User Role
 */
export type UserRole = 'ADMIN' | 'MANAGER' | 'STAFF' | 'SHIPPER' | 'CLIENT'

/**
 * Create User Request
 */
export class CreateUserRequest {
  username: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  address?: string
  identityNumber?: string
  password?: string

  constructor(data: CreateUserRequest) {
    this.username = data.username
    this.email = data.email
    this.firstName = data.firstName
    this.lastName = data.lastName
    this.phone = data.phone
    this.address = data.address
    this.identityNumber = data.identityNumber
    this.password = data.password
  }
}

/**
 * Update User Request
 */
export class UpdateUserRequest {
  email?: string
  firstName?: string
  lastName?: string
  phone?: string
  address?: string
  identityNumber?: string
  status?: UserStatus

  constructor(data: UpdateUserRequest) {
    this.email = data.email
    this.firstName = data.firstName
    this.lastName = data.lastName
    this.phone = data.phone
    this.address = data.address
    this.identityNumber = data.identityNumber
    this.status = data.status
  }
}

/**
 * Pagination Interface
 */
export interface Paging {
  page: number
  size: number
  totalElements: number
  totalPages: number
  filters?: any[]
  sorts?: any[]
  selected?: string[]
}

/**
 * Paginated Users Response
 */
export interface PagedUsersResponse {
  data: UserDto[]
  page: Paging
}

/**
 * User API Responses
 */
export type GetUsersResponse = IApiResponse<PagedUsersResponse>
export type GetUserResponse = IApiResponse<UserDto>
export type CreateUserResponse = IApiResponse<UserDto>
export type UpdateUserResponse = IApiResponse<UserDto>
export type DeleteUserResponse = IApiResponse<null>
