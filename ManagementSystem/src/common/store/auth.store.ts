/**
 * Auth Store
 * Manages authentication state using Pinia
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useCookies } from '@vueuse/integrations/useCookies'
import { isValidToken, getJWTPayload } from '@/common/utils/jwtDecode'
import type { UserDto } from '@/modules/LoginScreen/model.type'

const cookie = useCookies(['jwt_token'])

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<UserDto | null>(null)
  const roles = ref<string[]>([])
  const token = ref<string | null>(null)

  // Getters
  const isAuthenticated = computed(() => {
    const currentToken = token.value || cookie.get('jwt_token')
    return currentToken ? isValidToken(currentToken) : false
  })

  const currentUser = computed(() => user.value)
  const currentUserId = computed(() => user.value?.id || '')
  const currentUserRoles = computed(() => roles.value)

  /**
   * Initialize store from localStorage (for persistence)
   */
  function initialize() {
    try {
      // Load user from localStorage
      const userJson = localStorage.getItem('user')
      if (userJson) {
        user.value = JSON.parse(userJson)
      }

      // Load roles from localStorage
      const rolesJson = localStorage.getItem('user_roles')
      if (rolesJson) {
        roles.value = JSON.parse(rolesJson)
      }

      // Load token from cookie
      token.value = cookie.get('jwt_token')
    } catch (error) {
      console.error('Failed to initialize auth store:', error)
      clear()
    }
  }

  /**
   * Set authentication data
   */
  function setAuth(accessToken: string, userData: UserDto, userRoles?: string[]) {
    // Set token
    token.value = accessToken
    const payload = getJWTPayload(accessToken)
    let expirationDate: Date | undefined

    if (payload?.exp) {
      expirationDate = new Date(payload.exp * 1000)
    }

    cookie.set('jwt_token', accessToken, {
      expires: expirationDate,
      secure: true,
      sameSite: 'lax',
    })

    // Set user
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))

    // Set roles (use provided roles or from user object if available)
    const rolesToSet = userRoles || userData.roles || []
    roles.value = rolesToSet
    localStorage.setItem('user_roles', JSON.stringify(rolesToSet))
  }

  /**
   * Clear authentication data
   */
  function clear() {
    user.value = null
    roles.value = []
    token.value = null
    cookie.remove('jwt_token')
    localStorage.removeItem('user')
    localStorage.removeItem('user_roles')
  }

  /**
   * Update user data
   */
  function updateUser(userData: Partial<UserDto>) {
    if (user.value) {
      user.value = { ...user.value, ...userData }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  /**
   * Update roles
   */
  function updateRoles(newRoles: string[]) {
    roles.value = newRoles
    localStorage.setItem('user_roles', JSON.stringify(newRoles))
  }

  /**
   * Check if user has required roles
   */
  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  /**
   * Check if user has any of the required roles
   */
  function hasAnyRole(requiredRoles: string[]): boolean {
    return requiredRoles.some(role => roles.value.includes(role))
  }

  /**
   * Check if token needs refresh
   */
  function shouldRefreshToken(): boolean {
    const currentToken = token.value || cookie.get('jwt_token')
    if (!currentToken) return false

    const payload = getJWTPayload(currentToken)
    if (!payload?.exp) return false

    const currentTime = Math.floor(Date.now() / 1000)
    const fiveMinutesInSeconds = 5 * 60

    return payload.exp - currentTime <= fiveMinutesInSeconds
  }

  /**
   * Get current token
   */
  function getToken(): string | null {
    return token.value || cookie.get('jwt_token')
  }

  return {
    // State
    user,
    roles,
    token,
    // Getters
    isAuthenticated,
    currentUser,
    currentUserId,
    currentUserRoles,
    // Actions
    initialize,
    setAuth,
    clear,
    updateUser,
    updateRoles,
    hasRole,
    hasAnyRole,
    shouldRefreshToken,
    getToken,
  }
})
