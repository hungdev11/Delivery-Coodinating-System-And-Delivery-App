/**
 * BaseResponse Type Definition
 *
 * Standard response wrapper for all API v1 endpoints
 * - Non-paging endpoints: BaseResponse<T>
 * - Paging endpoints: BaseResponse<PagedData<T>>
 */

export interface BaseResponse<T> {
  result: T | null
  message: string | null
}

/**
 * PagedData Type Definition
 *
 * Used for paginated responses - wrapped inside BaseResponse
 * Example: BaseResponse<PagedData<UserDto>>
 */
export interface PagedData<T> {
  data: T[]
  page: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    filters?: any
    sorts?: any[]
    selected?: string[]
  }
}

/**
 * Check if response is a BaseResponse
 */
export function isBaseResponse<T>(response: unknown): response is BaseResponse<T> {
  return (
    typeof response === 'object' &&
    response !== null &&
    'result' in response &&
    'message' in response
  )
}

/**
 * Check if the result is PagedData
 */
export function isPagedData<T>(data: unknown): data is PagedData<T> {
  return (
    typeof data === 'object' &&
    data !== null &&
    'data' in data &&
    'page' in data &&
    Array.isArray((data as any).data)
  )
}

/**
 * Extract result from BaseResponse
 * Throws error with message if result is null
 */
export function unwrapBaseResponse<T>(response: BaseResponse<T>): T {
  if (response.result !== null) {
    return response.result
  }

  // If result is null, throw error with message
  throw new Error(response.message || 'Unknown error occurred')
}

/**
 * Check if BaseResponse indicates an error
 */
export function isErrorResponse<T>(response: BaseResponse<T>): boolean {
  return response.result === null && !!response.message
}

/**
 * Check if BaseResponse indicates success
 */
export function isSuccessResponse<T>(response: BaseResponse<T>): boolean {
  return response.result !== null
}
