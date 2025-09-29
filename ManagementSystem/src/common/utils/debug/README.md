# Debug Utility

A simple and powerful debug utility for development environment that returns formatted messages for use with `console.log()`.

## Features

- ✅ **Simple API**: Use `console.log(Debug(message))` format
- ✅ **Environment aware**: Only logs in development mode
- ✅ **Multiple log levels**: Debug, Info, Warn, Error, Styled
- ✅ **Context support**: Add context tags for better organization
- ✅ **Data serialization**: Automatically formats objects and arrays
- ✅ **TypeScript support**: Full type definitions included
- ✅ **Zero dependencies**: No external dependencies required

## Quick Start

```typescript
import { Debug, Info, Warn, ErrorLog, Styled } from '@/common/utils/debug'

// Basic usage
console.log(Info('Application started'))
console.log(Debug('User data loaded', { userId: 123, name: 'John' }))
console.log(Warn('Deprecated feature used'))
console.log(ErrorLog('API call failed', new Error('Network timeout')))
```

## API Reference

### Main Functions

All functions return a formatted string that can be used with `console.log()`.

#### `Debug(message, data?, context?)`

Log debug information.

```typescript
console.log(Debug('Processing user data', { userId: 123 }, 'API'))
// Output: [2024-01-15T10:30:00.000Z] DEBUG [API]: Processing user data { "userId": 123 }
```

#### `Info(message, data?, context?)`

Log general information.

```typescript
console.log(Info('User logged in', { email: 'user@example.com' }, 'AUTH'))
// Output: [2024-01-15T10:30:00.000Z] INFO [AUTH]: User logged in { "email": "user@example.com" }
```

#### `Warn(message, data?, context?)`

Log warning messages.

```typescript
console.log(Warn('Deprecated API endpoint used', { endpoint: '/old-api' }, 'API'))
// Output: [2024-01-15T10:30:00.000Z] WARN [API]: Deprecated API endpoint used { "endpoint": "/old-api" }
```

#### `ErrorLog(message, error?, context?)`

Log error messages with stack trace.

```typescript
console.log(ErrorLog('Database connection failed', new Error('Connection timeout'), 'DB'))
// Output: [2024-01-15T10:30:00.000Z] ERROR [DB]: Database connection failed Connection timeout
//         at Database.connect (db.js:10:5)
//         ...
```

#### `Styled(message, style, data?, context?)`

Log with custom styling information.

```typescript
console.log(
  Styled('Operation completed', 'color: green; font-weight: bold;', { result: 'success' }),
)
// Output: [2024-01-15T10:30:00.000Z] STYLED: Operation completed [Style: color: green; font-weight: bold;] { "result": "success" }
```

## Context Constants

Use predefined context constants for consistency:

```typescript
import { DebugContexts } from '@/common/utils/debug'

console.log(Info('API call started', { endpoint: '/users' }, DebugContexts.API))
console.log(Info('User authenticated', { userId: 123 }, DebugContexts.AUTH))
console.log(Info('Route changed', { from: '/home', to: '/profile' }, DebugContexts.ROUTER))
```

Available contexts:

- `API` - API calls and responses
- `AUTH` - Authentication and authorization
- `ROUTER` - Route navigation
- `STORE` - State management
- `COMPONENT` - Vue component lifecycle
- `UTILS` - Utility functions
- `VALIDATION` - Form and data validation
- `HTTP` - HTTP requests
- `WEBSOCKET` - WebSocket connections
- `CACHE` - Caching operations
- `PERFORMANCE` - Performance monitoring

## Style Constants

Use predefined styles for consistent styling:

```typescript
import { DebugStyles } from '@/common/utils/debug'

console.log(Styled('Success!', DebugStyles.SUCCESS))
console.log(Styled('Error occurred', DebugStyles.ERROR))
console.log(Styled('API call', DebugStyles.API_CALL))
```

Available styles:

- `SUCCESS` - Green, bold
- `ERROR` - Red, bold
- `WARNING` - Orange, bold
- `INFO` - Blue, bold
- `DEBUG` - Gray, italic
- `HIGHLIGHT` - Yellow background, black text
- `API_CALL` - Purple, bold
- `PERFORMANCE` - Teal, bold

## Usage Examples

### Basic Logging

```typescript
import { Debug, Info, Warn, ErrorLog } from '@/common/utils/debug'

// Simple messages
console.log(Info('Application started'))
console.log(Debug('Processing data'))
console.log(Warn('Feature deprecated'))
console.log(ErrorLog('Something went wrong'))

// With data
console.log(Info('User data loaded', { userId: 123, name: 'John' }))
console.log(Debug('API response', { status: 200, data: { users: [] } }))
```

### API Logging

```typescript
import { Info, Debug, ErrorLog, DebugContexts } from '@/common/utils/debug'

// API request
console.log(
  Info(
    'Making API call',
    {
      endpoint: '/users',
      method: 'GET',
    },
    DebugContexts.API,
  ),
)

// API response
console.log(
  Info(
    'API response received',
    {
      status: 200,
      data: { users: [] },
    },
    DebugContexts.API,
  ),
)

// API error
console.log(ErrorLog('API call failed', new Error('Network timeout'), DebugContexts.API))
```

### Component Lifecycle

```typescript
import { Info, Debug, DebugContexts } from '@/common/utils/debug'

// Component mounted
console.log(
  Info(
    'Component mounted',
    {
      componentName: 'UserProfile',
    },
    DebugContexts.COMPONENT,
  ),
)

// Props received
console.log(
  Debug(
    'Props received',
    {
      userId: 123,
      showDetails: true,
    },
    DebugContexts.COMPONENT,
  ),
)
```

### Performance Monitoring

```typescript
import { Styled, DebugStyles } from '@/common/utils/debug'

console.log(Styled('Performance check started', DebugStyles.PERFORMANCE))
console.time('data-processing')

// ... processing code ...

console.timeEnd('data-processing')
console.log(Styled('Performance check completed', DebugStyles.SUCCESS))
```

### Error Handling

```typescript
import { ErrorLog, DebugContexts } from '@/common/utils/debug'

try {
  // Some operation that might fail
  riskyOperation()
} catch (error) {
  console.log(ErrorLog('Operation failed', error, DebugContexts.UTILS))
}
```

### Custom Styling

```typescript
import { Styled } from '@/common/utils/debug'

console.log(Styled('Custom message', 'color: magenta; font-size: 16px; font-weight: bold;'))
console.log(
  Styled(
    'Gradient style',
    'background: linear-gradient(45deg, #ff6b6b, #4ecdc4); color: white; padding: 5px; border-radius: 3px;',
  ),
)
```

## Environment Detection

The debug utility automatically detects the environment and only logs in development mode:

- **Development**: All debug messages are shown
- **Production**: All debug functions return empty strings (no output)

Environment detection is based on:

- `import.meta.env.IS_PRODUCTION === true`
- `import.meta.env.NODE_ENV === 'production'`
- `import.meta.env.MODE === 'production'`

## TypeScript Support

Full TypeScript support with type definitions:

```typescript
import {
  Debug,
  Info,
  Warn,
  ErrorLog,
  Styled,
  DebugContexts,
  DebugStyles,
} from '@/common/utils/debug'

// All functions are fully typed
const message: string = Debug('Message', { data: 'value' }, DebugContexts.API)
const info: string = Info('Info message', { key: 'value' })
const error: string = ErrorLog('Error message', new Error('Error details'))
```

## Migration from Old Format

If you were using the old `debug.action()` format, here's how to migrate:

### Before (Old Format)

```typescript
import debug from '@/common/utils/debug'

debug.info('Message', { data: 'value' }, 'API')
debug.debug('Debug info', { key: 'value' })
debug.warn('Warning message')
debug.error('Error message', new Error('Details'))
```

### After (New Format)

```typescript
import { Info, Debug, Warn, ErrorLog, DebugContexts } from '@/common/utils/debug'

console.log(Info('Message', { data: 'value' }, DebugContexts.API))
console.log(Debug('Debug info', { key: 'value' }))
console.log(Warn('Warning message'))
console.log(ErrorLog('Error message', new Error('Details')))
```

## Best Practices

1. **Use contexts consistently**: Always use `DebugContexts` for better organization
2. **Keep messages descriptive**: Write clear, actionable log messages
3. **Include relevant data**: Add useful data objects for debugging
4. **Use appropriate log levels**:
   - `Debug` for detailed debugging info
   - `Info` for general information
   - `Warn` for warnings and deprecations
   - `ErrorLog` for errors and exceptions
5. **Don't over-log**: Avoid logging in tight loops or frequently called functions
6. **Use styled logging sparingly**: Reserve styled logs for important messages

## Examples

See `debug.examples.ts` for comprehensive usage examples covering various scenarios like API logging, component lifecycle, performance monitoring, error handling, and more.

## License

This utility is part of the Delivery Coordinating System project.
