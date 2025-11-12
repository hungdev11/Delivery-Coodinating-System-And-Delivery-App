/**
 * JWT Decode Utility
 * Handles JWT token decoding and validation
 */

import { Debug, Warn, ErrorLog, DebugContexts } from '@/common/utils/debug'
import type { TokenPayload } from '@/modules/LoginScreen/model.type'

export interface DecodedToken {
  header: Record<string, unknown>
  payload: TokenPayload
  signature: string
}

/**
 * Decodes a JWT token without verification
 * @param token - The JWT token to decode
 * @returns Decoded token object with header, payload, and signature
 */
export function decodeJWT(token: string): DecodedToken | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      console.log(
        ErrorLog(
          'Invalid JWT token format',
          {
            partsCount: parts.length,
            expectedParts: 3,
          },
          DebugContexts.UTILS,
        ),
      )
      throw new Error('Invalid JWT token format')
    }

    const [headerB64, payloadB64, signature] = parts

    const header = JSON.parse(atob(headerB64))
    const payload = JSON.parse(atob(payloadB64))

    return {
      header,
      payload,
      signature,
    }
  } catch (error) {
    console.log(ErrorLog('Error decoding JWT', error, DebugContexts.UTILS))
    return null
  }
}

/**
 * Extracts payload from JWT token
 * @param token - The JWT token
 * @returns JWT payload or null if invalid
 */
export function getJWTPayload(token: string): TokenPayload | null {
  const decoded = decodeJWT(token)
  return decoded?.payload || null
}

/**
 * Checks if JWT token is expired
 * @param token - The JWT token
 * @returns true if expired, false otherwise
 */
export function isTokenExpired(token: string): boolean {
  const payload = getJWTPayload(token)
  if (!payload) {
    console.log(Warn('No payload found for token expiration check', {}, DebugContexts.UTILS))
    return true
  }

  const currentTime = Math.floor(Date.now() / 1000)
  const isExpired = payload.exp < currentTime

  return isExpired
}

/**
 * Gets user roles from JWT token
 * @param token - The JWT token
 * @returns Array of roles or empty array if invalid
 */
export function getUserRoles(token: string): string[] {
  const payload = getJWTPayload(token)
  const roles = payload?.roles || []

  return roles
}

/**
 * Checks if user has specific role
 * @param token - The JWT token
 * @param role - The role to check
 * @returns true if user has the role, false otherwise
 */
export function hasRole(token: string, role: string): boolean {
  const roles = getUserRoles(token)
  return roles.includes(role)
}

/**
 * Checks if user has any of the specified roles
 * @param token - The JWT token
 * @param roles - Array of roles to check
 * @returns true if user has any of the roles, false otherwise
 */
export function hasAnyRole(token: string, roles: string[]): boolean {
  const userRoles = getUserRoles(token)
  const hasRole = roles.some((role) => userRoles.includes(role))

  return hasRole
}

/**
 * Gets user ID from JWT token
 * @param token - The JWT token
 * @returns User ID or null if invalid
 */
export function getUserId(token: string): string | null {
  const payload = getJWTPayload(token)
  return payload?.user.id || null
}

/**
 * Gets user name from JWT token
 * @param token - The JWT token
 * @returns User name or null if invalid
 */
export function getUserName(token: string): string | null {
  const payload = getJWTPayload(token)
  return payload?.user.firstName + ' ' + payload?.user.lastName || null
}

/**
 * Validates JWT token (checks expiration)
 * @param token - The JWT token
 * @returns true if valid, false otherwise
 */
export function isValidToken(token: string): boolean {
  if (!token) {
    console.log(Warn('Token validation failed - no token provided', {}, DebugContexts.UTILS))
    return false
  }

  const isValid = !isTokenExpired(token)

  return isValid
}
