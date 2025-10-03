/**
 * Debug utility for development environment
 * Only logs when IS_PRODUCTION=false or NODE_ENV !== 'production'
 */

// Check if we're in production mode
const isProduction =
  import.meta.env.IS_PRODUCTION === true ||
  import.meta.env.NODE_ENV === 'production' ||
  import.meta.env.MODE === 'production'

const isEnabled = !isProduction

/**
 * Get timestamp for logging
 */
const getTimestamp = (): string => {
  return new Date().toISOString()
}

/**
 * Format log message with timestamp and context
 */
const formatMessage = (level: string, message: string, context?: string): string => {
  const timestamp = getTimestamp()
  const contextStr = context ? ` [${context}]` : ''
  return `[${timestamp}] ${level}${contextStr}: ${message}`
}

/**
 * Main Debug function that returns formatted message
 * Usage: console.log(Debug(message, data, context))
 */
export const Debug = (message: string, data?: unknown, context?: string): string => {
  if (!isEnabled) return ''

  const formattedMessage = formatMessage('DEBUG', message, context)
  if (data !== undefined) {
    return `${formattedMessage} ${JSON.stringify(data, null, 2)}`
  }
  return formattedMessage
}

/**
 * Info function that returns formatted message
 * Usage: console.log(Info(message, data, context))
 */
export const Info = (message: string, data?: unknown, context?: string): string => {
  if (!isEnabled) return ''

  const formattedMessage = formatMessage('INFO', message, context)
  if (data !== undefined) {
    return `${formattedMessage} ${JSON.stringify(data, null, 2)}`
  }
  return formattedMessage
}

/**
 * Warn function that returns formatted message
 * Usage: console.log(Warn(message, data, context))
 */
export const Warn = (message: string, data?: unknown, context?: string): string => {
  if (!isEnabled) return ''

  const formattedMessage = formatMessage('WARN', message, context)
  if (data !== undefined) {
    return `${formattedMessage} ${JSON.stringify(data, null, 2)}`
  }
  return formattedMessage
}

/**
 * Error function that returns formatted message
 * Usage: console.log(ErrorLog(message, error, context))
 */
export const ErrorLog = (message: string, error?: Error | unknown, context?: string): string => {
  if (!isEnabled) return ''

  const formattedMessage = formatMessage('ERROR', message, context)
  if (error instanceof Error) {
    return `${formattedMessage} ${error.message}\n${error.stack}`
  } else if (error !== undefined) {
    return `${formattedMessage} ${JSON.stringify(error, null, 2)}`
  }
  return formattedMessage
}

/**
 * Styled function that returns formatted message with style info
 * Usage: console.log(Styled(message, style, data, context))
 */
export const Styled = (
  message: string,
  style: string,
  data?: unknown,
  context?: string,
): string => {
  if (!isEnabled) return ''

  const formattedMessage = formatMessage('STYLED', message, context)
  if (data !== undefined) {
    return `${formattedMessage} [Style: ${style}] ${JSON.stringify(data, null, 2)}`
  }
  return `${formattedMessage} [Style: ${style}]`
}

// Export utility functions
export const isDebugEnabledFunc = (): boolean => isEnabled
export const getEnvironmentInfoFunc = (): {
  isProduction: boolean
  mode: string
  isEnabled: boolean
} => ({
  isProduction,
  mode: import.meta.env.MODE || 'unknown',
  isEnabled,
})
