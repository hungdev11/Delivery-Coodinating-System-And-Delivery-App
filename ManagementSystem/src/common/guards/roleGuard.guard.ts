/**
 * Role Guard for Vue Router
 * Handles route protection based on user roles
 */

import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { Debug, Info, Warn, ErrorLog, DebugContexts } from '@/common/utils/debug'
import { useAuthStore } from '@/common/store/auth.store'
import type { UserDto } from '@/modules/LoginScreen/model.type'

export interface RoleGuardConfig {
  requiredRoles?: string[]
  redirectTo?: string
  allowUnauthenticated?: boolean
}

export interface RouteGuardMeta {
  requiresAuth?: boolean
  requiredRoles?: string[]
  redirectTo?: string
}

/**
 * Gets current user information from Pinia store
 * @returns User information or null if not authenticated
 */
export function getCurrentUser(): UserDto | null {
  const authStore = useAuthStore()
  return authStore.currentUser
}

/**
 * Gets user roles from Pinia store
 * @returns User roles or empty array if not found
 */
export function getUserRoles(): string[] {
  const authStore = useAuthStore()
  return authStore.currentUserRoles
}

/**
 * Checks if user is authenticated
 * @returns true if authenticated, false otherwise
 */
export function isAuthenticated(): boolean {
  const authStore = useAuthStore()
  return authStore.isAuthenticated
}

/**
 * Checks if user has required roles for a route
 * @param requiredRoles - Array of required roles
 * @returns true if user has required roles, false otherwise
 */
export function hasRequiredRoles(requiredRoles: string[]): boolean {
  const authStore = useAuthStore()
  return authStore.hasAnyRole(requiredRoles)
}

/**
 * Removes JWT token, user information, and roles from storage
 */
export function removeToken(): void {
  const authStore = useAuthStore()
  authStore.clear()
}

/**
 * Checks if token needs refresh (expires within next 5 minutes)
 * @returns true if token should be refreshed
 */
export function shouldRefreshToken(): boolean {
  const authStore = useAuthStore()
  return authStore.shouldRefreshToken()
}

/**
 * Creates a role guard function for Vue Router
 * @param config - Guard configuration
 * @returns Navigation guard function
 */
export function createRoleGuard(config: RoleGuardConfig = {}) {
  return (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext,
  ) => {
    const meta = to.meta as RouteGuardMeta
    const requiredRoles = meta.requiredRoles || config.requiredRoles || []
    const redirectTo = meta.redirectTo || config.redirectTo || '/login'
    const allowUnauthenticated = meta.requiresAuth === false || config.allowUnauthenticated

    // If no authentication required, allow access
    if (allowUnauthenticated) {
      console.log(Info('Public route - allowing access', {}, DebugContexts.ROUTER))
      return next()
    }

    // Check if user is authenticated
    if (!isAuthenticated()) {
      console.log(
        Warn('User not authenticated - redirecting to login', { redirectTo }, DebugContexts.ROUTER),
      )
      // Save the current route as redirect target (only if not already going to login)
      if (to.path !== '/login' && to.path !== redirectTo) {
        const redirectPath = to.fullPath
        localStorage.setItem('auth_redirect', redirectPath)
        console.log(Info('Saved redirect path', { redirectPath }, DebugContexts.ROUTER))
      }
      return next({ path: redirectTo, query: { redirect: to.fullPath } })
    }

    // If no roles required, allow access
    if (!requiredRoles || requiredRoles.length === 0) {
      console.log(Info('No roles required - allowing access', {}, DebugContexts.ROUTER))
      return next()
    }

    // Check if user has required roles
    if (!hasRequiredRoles(requiredRoles)) {
      return next('/unauthorized')
    }

    // Allow access
    next()
  }
}

/**
 * Default role guard instance
 */
export const roleGuard = createRoleGuard()

/**
 * Admin role guard - requires admin role
 */
export const adminGuard = createRoleGuard({
  requiredRoles: ['admin'],
  redirectTo: '/login',
})

/**
 * User role guard - requires user role
 */
export const userGuard = createRoleGuard({
  requiredRoles: ['user'],
  redirectTo: '/login',
})

/**
 * Manager role guard - requires manager role
 */
export const managerGuard = createRoleGuard({
  requiredRoles: ['manager'],
  redirectTo: '/login',
})

/**
 * Custom role guard factory
 * @param roles - Required roles
 * @param redirectTo - Redirect path if unauthorized
 * @returns Role guard function
 */
export function createCustomRoleGuard(roles: string[], redirectTo = '/login') {
  return createRoleGuard({
    requiredRoles: roles,
    redirectTo,
  })
}

/**
 * Public route guard - allows unauthenticated access
 */
export const publicGuard = createRoleGuard({
  allowUnauthenticated: true,
})
