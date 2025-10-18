/**
 * Role Guard for Vue Router
 * Handles route protection based on user roles
 */

import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { isValidToken, getJWTPayload } from '@/common/utils/jwtDecode'
import { Debug, Info, Warn, ErrorLog, DebugContexts } from '@/common/utils/debug'
import { useCookies } from '@vueuse/integrations/useCookies'
import type { UserDto } from '@/modules/LoginScreen/model.type'

const cookie = useCookies(['token'])

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
 * Gets JWT token from localStorage
 * @returns JWT token or null
 */
function getToken(): string | null {
  const token = cookie.get('jwt_token')

  console.log(
    Debug(
      'Token retrieval',
      {
        hasToken: !!token,
        tokenLength: token?.length || 0,
      },
      DebugContexts.AUTH,
    ),
  )

  return token
}

/**
 * Stores JWT token in storage
 * @param token - JWT token to store
 * @param useSession - Whether to use sessionStorage instead of localStorage
 */
export function setToken(token: string): void {
  console.log(
    Info(
      'Storing JWT token',
      {
        tokenLength: token.length,
        storageType: 'cookies',
      },
      DebugContexts.AUTH,
    ),
  )

  // Get token expiration from JWT payload
  const payload = getJWTPayload(token)
  let expirationDate: Date | undefined

  if (payload?.exp) {
    // Convert Unix timestamp to JavaScript Date
    expirationDate = new Date(payload.exp * 1000)
    console.log(
      Info(
        'Setting cookie expiration',
        {
          expirationTime: payload.exp,
          expirationDate: expirationDate.toISOString(),
          timeUntilExpiry: payload.exp * 1000 - Date.now(),
        },
        DebugContexts.AUTH,
      ),
    )
  }

  // Set cookie with expiration time
  cookie.set('jwt_token', token, {
    expires: expirationDate,
    secure: true, // Use secure cookies in production
    sameSite: 'lax', // CSRF protection
  })

  console.log(
    Info(
      'Token stored successfully',
      {
        storageType: 'cookie',
        hasExpiration: !!expirationDate,
        expirationDate: expirationDate?.toISOString(),
      },
      DebugContexts.AUTH,
    ),
  )
}

/**
 * Stores user information and roles in localStorage
 * @param user - User information to store
 * @param roles - User roles to store (optional)
 */
export function setUser(user: UserDto, roles?: string[]): void {
  console.log(
    Info(
      'Storing user information in localStorage',
      {
        userId: user.id,
        username: user.username,
        email: user.email,
        status: user.status,
        roles: roles || [],
      },
      DebugContexts.AUTH,
    ),
  )

  localStorage.setItem('user', JSON.stringify(user))

  // Store roles if provided
  if (roles) {
    localStorage.setItem('user_roles', JSON.stringify(roles))
  }

  console.log(Info('User information stored successfully in localStorage', {}, DebugContexts.AUTH))
}

/**
 * Stores user roles in localStorage
 * @param roles - User roles to store
 */
export function setUserRoles(roles: string[]): void {
  console.log(
    Info(
      'Storing user roles in localStorage',
      {
        roles,
        rolesCount: roles.length,
      },
      DebugContexts.AUTH,
    ),
  )

  localStorage.setItem('user_roles', JSON.stringify(roles))

  console.log(Info('User roles stored successfully in localStorage', {}, DebugContexts.AUTH))
}

/**
 * Gets user roles from localStorage
 * @returns User roles or empty array if not found
 */
export function getUserRoles(): string[] {
  try {
    const rolesJson = localStorage.getItem('user_roles')
    const roles = rolesJson ? JSON.parse(rolesJson) : []

    console.log(
      Debug(
        'User roles retrieval from localStorage',
        {
          roles,
          rolesCount: roles.length,
          hasRoles: roles.length > 0,
        },
        DebugContexts.AUTH,
      ),
    )

    return roles
  } catch (error) {
    console.log(ErrorLog('Error parsing user roles from localStorage', error, DebugContexts.AUTH))
    return []
  }
}

/**
 * Gets user information from localStorage
 * @returns User information or null if not found
 */
export function getUser(): UserDto | null {
  try {
    const userJson = localStorage.getItem('user')
    const user = userJson ? JSON.parse(userJson) : null

    console.log(
      Debug(
        'User retrieval from localStorage',
        {
          hasUser: !!user,
          userId: user?.id,
          username: user?.username,
        },
        DebugContexts.AUTH,
      ),
    )

    return user
  } catch (error) {
    console.log(ErrorLog('Error parsing user data from localStorage', error, DebugContexts.AUTH))
    return null
  }
}

/**
 * Removes JWT token, user information, and roles from storage
 */
export function removeToken(): void {
  console.log(Info('Removing JWT token, user data, and roles from storage', {}, DebugContexts.AUTH))

  // Remove token from cookies
  cookie.remove('jwt_token')

  // Remove user data and roles from localStorage
  localStorage.removeItem('user')
  localStorage.removeItem('user_roles')

  console.log(Info('Token, user data, and roles removed successfully', {}, DebugContexts.AUTH))
}

/**
 * Checks if token needs refresh (expires within next 5 minutes)
 * @returns true if token should be refreshed
 */
export function shouldRefreshToken(): boolean {
  const token = getToken()
  if (!token) return false

  const payload = getJWTPayload(token)
  if (!payload?.exp) return false

  const currentTime = Math.floor(Date.now() / 1000)
  const fiveMinutesInSeconds = 5 * 60

  // Check if token expires within next 5 minutes
  const shouldRefresh = payload.exp - currentTime <= fiveMinutesInSeconds

  console.log(
    Debug(
      'Token refresh check',
      {
        currentTime,
        expirationTime: payload.exp,
        timeRemaining: payload.exp - currentTime,
        shouldRefresh,
        refreshThreshold: fiveMinutesInSeconds,
      },
      DebugContexts.AUTH,
    ),
  )

  return shouldRefresh
}

/**
 * Gets current user information from stored user data
 * @returns User information or null if not authenticated
 */
export function getCurrentUser(): UserDto | null {
  const user = getUser()
  if (!user) {
    console.log(Debug('No user data found for current user', {}, DebugContexts.AUTH))
    return null
  }

  console.log(
    Debug(
      'Current user retrieved',
      {
        userId: user.id,
        userName: user.firstName + ' ' + user.lastName,
        username: user.username,
        email: user.email,
        status: user.status,
        hasUser: !!user,
      },
      DebugContexts.AUTH,
    ),
  )

  return user
}

/**
 * Checks if user is authenticated
 * @returns true if authenticated, false otherwise
 */
export function isAuthenticated(): boolean {
  const token = getToken()
  const authenticated = token ? isValidToken(token) : false

  console.log(
    Debug(
      'Authentication check',
      {
        hasToken: !!token,
        isValid: authenticated,
        tokenLength: token?.length || 0,
      },
      DebugContexts.AUTH,
    ),
  )

  return authenticated
}

/**
 * Checks if user has required roles for a route
 * @param requiredRoles - Array of required roles
 * @returns true if user has required roles, false otherwise
 */
export function hasRequiredRoles(requiredRoles: string[]): boolean {
  const userRoles = getUserRoles()
  if (userRoles.length === 0) {
    console.log(Warn('No user roles available for role check', { requiredRoles }, DebugContexts.AUTH))
    return false
  }

  const hasRoles = requiredRoles.some(role => userRoles.includes(role))

  console.log(
    Debug(
      'Role check',
      {
        requiredRoles,
        userRoles,
        hasRequiredRoles: hasRoles,
        matchingRoles: requiredRoles.filter(role => userRoles.includes(role)),
      },
      DebugContexts.AUTH,
    ),
  )

  return hasRoles
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
    console.log(
      Info(
        'Route Guard Check started',
        {
          from: from.path,
          to: to.path,
          routeName: to.name,
        },
        DebugContexts.ROUTER,
      ),
    )

    const meta = to.meta as RouteGuardMeta
    const requiredRoles = meta.requiredRoles || config.requiredRoles || []
    const redirectTo = meta.redirectTo || config.redirectTo || '/login'
    const allowUnauthenticated = meta.requiresAuth === false || config.allowUnauthenticated

    console.log(
      Debug(
        'Guard configuration',
        {
          requiredRoles,
          redirectTo,
          allowUnauthenticated,
          requiresAuth: meta.requiresAuth,
        },
        DebugContexts.ROUTER,
      ),
    )

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
      return next(redirectTo)
    }

    // If no roles required, allow access
    if (!requiredRoles || requiredRoles.length === 0) {
      console.log(Info('No roles required - allowing access', {}, DebugContexts.ROUTER))
      return next()
    }

    // Check if user has required roles
    if (!hasRequiredRoles(requiredRoles)) {
      console.log(
        ErrorLog(
          'User lacks required roles - redirecting to unauthorized',
          {
            requiredRoles,
            currentUser: getCurrentUser(),
          },
          DebugContexts.ROUTER,
        ),
      )
      return next('/unauthorized')
    }

    // Allow access
    console.log(
      Info(
        'Access granted',
        {
          user: getCurrentUser()?.firstName + ' ' + getCurrentUser()?.lastName,
          roles: getUserRoles(),
        },
        DebugContexts.ROUTER,
      ),
    )
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
