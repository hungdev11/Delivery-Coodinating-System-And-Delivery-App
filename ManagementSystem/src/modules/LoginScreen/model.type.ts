// Request types
export class LoginForm {
  username: string
  password: string
  rememberMe: boolean

  constructor(username: string, password: string, rememberMe: boolean) {
    this.username = username
    this.password = password
    this.rememberMe = rememberMe
  }
}

export interface LoginRequest {
  username: string
  password: string
  type: 'BACKEND' | 'FRONTEND'
}

export interface LogoutRequest {
  refreshToken: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}

// Response types
export interface UserDto {
  id: string
  keycloakId: string
  username: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  address?: string
  identityNumber?: string
  status: 'ACTIVE' | 'BLOCKED' | 'PENDING'
  createdAt: string
  updatedAt: string
}

export interface LoginResponse {
  message: string
  accessToken: string
  expiresIn: number
  refreshToken: string
  tokenType: string
  user: UserDto
}

// Keycloak JWT Token interfaces
export interface KeycloakRealmAccess {
  roles: string[]
}

export interface KeycloakResourceAccess {
  [resource: string]: {
    roles: string[]
  }
}

export interface KeycloakJwtPayload {
  exp: number
  iat: number
  jti: string
  iss: string
  aud: string | string[]
  sub: string
  typ: string
  azp: string
  sid: string
  acr: string
  'allowed-origins': string[]
  realm_access: KeycloakRealmAccess
  resource_access: KeycloakResourceAccess
  scope: string
  email_verified: boolean
  name: string
  preferred_username: string
  given_name: string
  family_name: string
  email: string
}

export interface TokenValidationResponse {
  sub: string
  preferred_username: string
  email: string
  given_name: string
  family_name: string
  realm_access: KeycloakRealmAccess
  resource_access: KeycloakResourceAccess
  email_verified: boolean
  name: string
  scope: string
}

// Enhanced TokenPayload class for Keycloak integration
export class TokenPayload {
  token: string
  tokenType: string
  iat: number
  exp: number
  user: UserDto
  roles: string[]
  keycloakPayload: KeycloakJwtPayload

  constructor(
    token: string,
    tokenType: string,
    iat: number,
    exp: number,
    user: UserDto,
    roles: string[],
    keycloakPayload?: KeycloakJwtPayload,
  ) {
    this.token = token
    this.tokenType = tokenType
    this.iat = iat
    this.exp = exp
    this.user = user
    this.roles = roles
    this.keycloakPayload = keycloakPayload || {} as KeycloakJwtPayload
  }

  // Helper methods for Keycloak integration
  getRealmRoles(): string[] {
    return this.keycloakPayload?.realm_access?.roles || []
  }

  getResourceRoles(resource: string): string[] {
    return this.keycloakPayload?.resource_access?.[resource]?.roles || []
  }

  getClientRoles(): string[] {
    return this.getResourceRoles('frontend-client')
  }

  hasRole(role: string): boolean {
    const allRoles = [
      ...this.getRealmRoles(),
      ...this.getClientRoles(),
      ...this.roles
    ]
    return allRoles.includes(role)
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role))
  }

  isEmailVerified(): boolean {
    return this.keycloakPayload?.email_verified || false
  }

  getScope(): string {
    return this.keycloakPayload?.scope || ''
  }

  getIssuer(): string {
    return this.keycloakPayload?.iss || ''
  }

  getAudience(): string | string[] {
    return this.keycloakPayload?.aud || ''
  }
}

// Legacy TokenPayload for backward compatibility
export class LegacyTokenPayload {
  token: string
  tokenType: string
  iat: number
  exp: number
  user: UserDto
  roles: string[]

  constructor(
    token: string,
    tokenType: string,
    iat: number,
    exp: number,
    user: UserDto,
    roles: string[],
  ) {
    this.token = token
    this.tokenType = tokenType
    this.iat = iat
    this.exp = exp
    this.user = user
    this.roles = roles
  }
}
