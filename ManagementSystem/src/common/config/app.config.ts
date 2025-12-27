/**
 * Application Configuration
 *
 * Central configuration file for the Management System.
 * Contains API endpoints, pagination settings, and other app-wide constants.
 */

/**
 * API Configuration
 */
export const API_CONFIG = {
  /**
   * Base URL for the API Gateway
   * Default: http://localhost:8080/api/v1
   */
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',

  /**
   * Request timeout in milliseconds
   */
  TIMEOUT: 30000,

  /**
   * API endpoints
   */
  ENDPOINTS: {
    // Auth endpoints
    AUTH: {
      LOGIN: '/auth/login',
      LOGOUT: '/auth/logout',
      REFRESH: '/auth/refresh-token',
      ME: '/auth/me',
      VALIDATE: '/auth/validate-token',
      SYNC: '/auth/sync',
    },

    // User endpoints
    USERS: {
      BASE: '/users',
      ME: '/users/me',
      BY_ID: (id: string) => `/users/${id}`,
      BY_USERNAME: (username: string) => `/users/username/${username}`,
    },

    // Settings endpoints
    SETTINGS: {
      BASE: '/settings',
      BY_GROUP: (group: string) => `/settings/${group}`,
      BY_KEY: (group: string, key: string) => `/settings/${group}/${key}`,
      VALUE: (group: string, key: string) => `/settings/${group}/${key}/value`,
    },

    // Zone endpoints
    ZONES: {
      BASE: '/zones',
      HEALTH: '/zone/health',
      BY_ID: (id: string) => `/zones/${id}`,
      BY_CODE: (code: string) => `/zones/code/${code}`,
      BY_CENTER: (centerId: string) => `/zones/center/${centerId}`,
    },

    // Center endpoints
    CENTERS: {
      BASE: '/centers',
      BY_ID: (id: string) => `/centers/${id}`,
    },

    // Address endpoints
    ADDRESSES: {
      BASE: '/addresses',
      BY_ID: (id: string) => `/addresses/${id}`,
      SEARCH: '/addresses/search',
    },

    // Routing endpoints
    ROUTING: {
      BASE: '/routing',
      ROUTE: '/routing/route',
      MATRIX: '/routing/matrix',
    },
  },
} as const

/**
 * Pagination Configuration
 */
export const PAGINATION_CONFIG = {
  /**
   * Default page size
   */
  DEFAULT_PAGE_SIZE: 10,

  /**
   * Available page size options
   */
  PAGE_SIZE_OPTIONS: [5, 10, 20, 50, 100],

  /**
   * Maximum page size allowed
   */
  MAX_PAGE_SIZE: 100,

  /**
   * Default page number (0-indexed)
   */
  DEFAULT_PAGE: 0,
} as const

/**
 * Storage Keys
 */
export const STORAGE_KEYS = {
  /**
   * Authentication token
   */
  ACCESS_TOKEN: 'access_token',

  /**
   * Refresh token
   */
  REFRESH_TOKEN: 'refresh_token',

  /**
   * User information
   */
  USER: 'user',

  /**
   * Theme preference
   */
  THEME: 'theme',

  /**
   * Language preference
   */
  LOCALE: 'locale',

  /**
   * Sidebar state
   */
  SIDEBAR_STATE: 'sidebar_state',
} as const

/**
 * Application Constants
 */
export const APP_CONFIG = {
  /**
   * Application name
   */
  NAME: 'DSS - Phân phối hàng hoá đầu cuối',

  /**
   * Application version
   */
  VERSION: '1.0.0',

  /**
   * Default language
   */
  DEFAULT_LOCALE: 'en',

  /**
   * Available languages
   */
  AVAILABLE_LOCALES: ['en', 'vi'],

  /**
   * Date format
   */
  DATE_FORMAT: 'YYYY-MM-DD',

  /**
   * Date time format
   */
  DATETIME_FORMAT: 'YYYY-MM-DD HH:mm:ss',

  /**
   * Time format
   */
  TIME_FORMAT: 'HH:mm:ss',
} as const

/**
 * User Roles
 */
export const USER_ROLES = {
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  STAFF: 'STAFF',
  SHIPPER: 'SHIPPER',
  CLIENT: 'CLIENT',
} as const

/**
 * User Status
 */
export const USER_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  SUSPENDED: 'SUSPENDED',
  PENDING: 'PENDING',
} as const

/**
 * Login Types
 */
export const LOGIN_TYPES = {
  BACKEND: 'BACKEND', // Admin/Staff
  FRONTEND: 'FRONTEND', // Shipper/Client
} as const

/**
 * Toast Configuration
 */
export const TOAST_CONFIG = {
  /**
   * Default timeout in milliseconds
   */
  DEFAULT_TIMEOUT: 5000,

  /**
   * Success toast timeout
   */
  SUCCESS_TIMEOUT: 3000,

  /**
   * Error toast timeout
   */
  ERROR_TIMEOUT: 7000,
} as const

/**
 * Validation Configuration
 */
export const VALIDATION_CONFIG = {
  /**
   * Minimum password length
   */
  MIN_PASSWORD_LENGTH: 8,

  /**
   * Maximum password length
   */
  MAX_PASSWORD_LENGTH: 128,

  /**
   * Minimum username length
   */
  MIN_USERNAME_LENGTH: 3,

  /**
   * Maximum username length
   */
  MAX_USERNAME_LENGTH: 50,

  /**
   * Email regex pattern
   */
  EMAIL_PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,

  /**
   * Phone regex pattern (Vietnam)
   */
  PHONE_PATTERN: /^(\+84|0)[1-9]\d{8}$/,
} as const

/**
 * Map Configuration
 */
export const MAP_CONFIG = {
  /**
   * Default map center (Ho Chi Minh City)
   */
  DEFAULT_CENTER: {
    lat: 10.8231,
    lng: 106.6297,
  },

  /**
   * Default zoom level
   */
  DEFAULT_ZOOM: 12,

  /**
   * Map style URL (Track-Asia)
   */
  STYLE_URL: import.meta.env.VITE_MAP_STYLE_URL || 'https://api.track-asia.com/v1/styles/streets',

  /**
   * API Key
   */
  API_KEY: import.meta.env.VITE_MAP_API_KEY || '',
} as const

/**
 * Feature Flags
 */
export const FEATURE_FLAGS = {
  /**
   * Enable dark mode toggle
   */
  ENABLE_DARK_MODE: true,

  /**
   * Enable multi-language support
   */
  ENABLE_I18N: false,

  /**
   * Enable map features
   */
  ENABLE_MAP: true,

  /**
   * Enable debugging tools
   */
  ENABLE_DEBUG: import.meta.env.DEV,
} as const

// Type exports
export type UserRole = (typeof USER_ROLES)[keyof typeof USER_ROLES]
export type UserStatus = (typeof USER_STATUS)[keyof typeof USER_STATUS]
export type LoginType = (typeof LOGIN_TYPES)[keyof typeof LOGIN_TYPES]
