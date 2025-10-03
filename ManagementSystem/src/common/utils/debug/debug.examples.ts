/**
 * Debug utility usage examples
 * This file demonstrates how to use the debug utility in different scenarios
 */

import { Debug, Info, Warn, ErrorLog, Styled, DebugContexts, DebugStyles } from './index'

// Example 1: Basic logging
export const basicLoggingExample = () => {
  console.log(Info('Application started'))
  console.log(Debug('User data loaded', { userId: 123, name: 'John' }))
  console.log(Warn('Deprecated feature used'))
  console.log(ErrorLog('API call failed', new Error('Network timeout')))
}

// Example 2: API logging
export const apiLoggingExample = () => {
  console.log(Info('Making API call', { endpoint: '/users', method: 'GET' }, DebugContexts.API))
  console.time('api-call')

  // Simulate API call
  setTimeout(() => {
    console.timeEnd('api-call')
    console.log(
      Info('API response received', { status: 200, data: { users: [] } }, DebugContexts.API),
    )
  }, 1000)
}

// Example 3: Component lifecycle logging
export const componentLoggingExample = () => {
  console.log(Info('Component mounted', { componentName: 'UserProfile' }, DebugContexts.COMPONENT))
  console.log(Debug('Props received', { userId: 123, showDetails: true }, DebugContexts.COMPONENT))
}

// Example 4: Performance monitoring
export const performanceLoggingExample = () => {
  console.log(Styled('Performance check started', DebugStyles.PERFORMANCE))
  console.time('data-processing')

  // Simulate data processing
  setTimeout(() => {
    console.timeEnd('data-processing')
    console.log(Styled('Performance check completed', DebugStyles.SUCCESS))
  }, 500)
}

// Example 5: Error handling
export const errorHandlingExample = () => {
  try {
    // Simulate error
    throw new Error('Something went wrong')
  } catch (error) {
    console.log(ErrorLog('Caught exception', error, DebugContexts.UTILS))
  }
}

// Example 6: Table logging (using console.table directly)
export const tableLoggingExample = () => {
  const userData = [
    { id: 1, name: 'John', email: 'john@example.com' },
    { id: 2, name: 'Jane', email: 'jane@example.com' },
  ]

  console.log(Info('Displaying user data table', undefined, DebugContexts.API))
  console.table(userData)
}

// Example 7: Authentication logging
export const authLoggingExample = () => {
  console.log(Info('User attempting login', { email: 'user@example.com' }, DebugContexts.AUTH))
  console.log(Styled('Login successful', DebugStyles.SUCCESS))
  console.log(Info('Token generated', { token: 'jwt-token-here' }, DebugContexts.AUTH))
}

// Example 8: Router navigation logging
export const routerLoggingExample = () => {
  console.log(Info('Route change', { from: '/home', to: '/profile' }, DebugContexts.ROUTER))
  console.log(Debug('Route params', { userId: 123 }, DebugContexts.ROUTER))
}

// Example 9: Store state changes
export const storeLoggingExample = () => {
  console.log(Info('Previous state', { user: null }, DebugContexts.STORE))
  console.log(Info('New state', { user: { id: 123, name: 'John' } }, DebugContexts.STORE))
  console.log(Styled('State updated successfully', DebugStyles.SUCCESS))
}

// Example 10: Validation logging
export const validationLoggingExample = () => {
  console.log(
    Info('Validating user input', { email: 'test@example.com' }, DebugContexts.VALIDATION),
  )
  console.log(
    Warn(
      'Validation warning',
      { field: 'email', message: 'Email format is weak' },
      DebugContexts.VALIDATION,
    ),
  )
  console.log(Styled('Validation completed', DebugStyles.INFO))
}

// Example 11: HTTP request/response logging
export const httpLoggingExample = () => {
  console.log(
    Styled(
      'Request sent',
      DebugStyles.API_CALL,
      {
        url: '/api/users',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      },
      DebugContexts.HTTP,
    ),
  )

  setTimeout(() => {
    console.log(
      Styled(
        'Response received',
        DebugStyles.SUCCESS,
        {
          status: 201,
          data: { id: 456, name: 'New User' },
        },
        DebugContexts.HTTP,
      ),
    )
  }, 1000)
}

// Example 12: Cache operations
export const cacheLoggingExample = () => {
  console.log(Info('Cache miss', { key: 'user:123' }, DebugContexts.CACHE))
  console.log(Info('Fetching from API', { endpoint: '/users/123' }, DebugContexts.CACHE))
  console.log(Info('Storing in cache', { key: 'user:123', ttl: 3600 }, DebugContexts.CACHE))
}

// Example 13: WebSocket logging
export const websocketLoggingExample = () => {
  console.log(
    Info('Connecting to WebSocket', { url: 'ws://localhost:8080' }, DebugContexts.WEBSOCKET),
  )
  console.log(Styled('Connection established', DebugStyles.SUCCESS))
  console.log(
    Info('Message sent', { type: 'subscribe', channel: 'notifications' }, DebugContexts.WEBSOCKET),
  )
}

// Example 14: Custom styled logging
export const customStyledLoggingExample = () => {
  console.log(
    Styled('Custom styled message', 'color: magenta; font-size: 16px; font-weight: bold;'),
  )
  console.log(
    Styled(
      'Another custom style',
      'background: linear-gradient(45deg, #ff6b6b, #4ecdc4); color: white; padding: 5px; border-radius: 3px;',
    ),
  )
}

// Example 15: Conditional logging
export const conditionalLoggingExample = () => {
  // Debug functions return empty string in production, so they're safe to use
  console.log(Info('This will only show in development mode'))
  console.log(Debug('Debug info', { someData: 'value' }))
}
