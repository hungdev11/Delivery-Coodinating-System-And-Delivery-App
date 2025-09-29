/**
 * Type definitions for debug utility
 */

export interface DebugEnvironmentInfo {
  isProduction: boolean
  mode: string
  isEnabled: boolean
}

// Main Debug functions that return formatted strings
export interface DebugFunctions {
  Debug(message: string, data?: unknown, context?: string): string
  Info(message: string, data?: unknown, context?: string): string
  Warn(message: string, data?: unknown, context?: string): string
  ErrorLog(message: string, error?: Error | unknown, context?: string): string
  Styled(message: string, style: string, data?: unknown, context?: string): string
}

export interface DebugUtil {
  debug(message: string, data?: unknown, context?: string): void
  info(message: string, data?: unknown, context?: string): void
  warn(message: string, data?: unknown, context?: string): void
  error(message: string, error?: Error | unknown, context?: string): void
  group(label: string, context?: string): void
  groupEnd(): void
  time(label: string): void
  timeEnd(label: string): void
  table(data: unknown, context?: string): void
  styled(message: string, style: string, data?: unknown, context?: string): void
  isDebugEnabled(): boolean
  getEnvironmentInfo(): DebugEnvironmentInfo
}

// Common context names for consistency
export const DebugContexts = {
  API: 'API',
  AUTH: 'AUTH',
  ROUTER: 'ROUTER',
  STORE: 'STORE',
  COMPONENT: 'COMPONENT',
  UTILS: 'UTILS',
  VALIDATION: 'VALIDATION',
  HTTP: 'HTTP',
  WEBSOCKET: 'WEBSOCKET',
  CACHE: 'CACHE',
  PERFORMANCE: 'PERFORMANCE',
} as const

export type DebugContext = (typeof DebugContexts)[keyof typeof DebugContexts] | string

// Common CSS styles for styled logging
export const DebugStyles = {
  SUCCESS: 'color: green; font-weight: bold;',
  ERROR: 'color: red; font-weight: bold;',
  WARNING: 'color: orange; font-weight: bold;',
  INFO: 'color: blue; font-weight: bold;',
  DEBUG: 'color: gray; font-style: italic;',
  HIGHLIGHT: 'background: yellow; color: black; font-weight: bold;',
  API_CALL: 'color: purple; font-weight: bold;',
  PERFORMANCE: 'color: teal; font-weight: bold;',
} as const
