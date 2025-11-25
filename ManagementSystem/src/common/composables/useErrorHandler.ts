import { useToast } from '@nuxt/ui/runtime/composables/useToast.js'
import { isBaseResponse, isErrorResponse } from '@/common/types/baseResponse'

/**
 * Composable for handling API errors globally
 *
 * Automatically shows toast notifications for BaseResponse errors
 *
 * Works with both:
 * - Regular endpoints: BaseResponse<T>
 *
 * @example
 * ```ts
 * const { handleResponse } = useErrorHandler()
 *
 * // Non-paging endpoint
 * const response = await api.getUser(id)  // BaseResponse<UserDto>
 * const user = handleResponse(response)    // Returns UserDto or throws
 * ```
 */
export function useErrorHandler() {
  const toast = useToast()

  /**
   * Handle BaseResponse and show error toast if needed
   * Returns the unwrapped result or throws error
   *
   * Works with both regular and paging responses:
   * - BaseResponse<T> → returns T
   */
  function handleResponse<T = unknown>(response: unknown): T {
    // Check if it's a BaseResponse
    if (isBaseResponse<T>(response)) {
      // If it's an error response, show toast and throw
      if (isErrorResponse(response)) {
        const errorMessage = response.message || 'Đã xảy ra lỗi'

        toast.add({
          title: 'Lỗi',
          description: errorMessage,
          color: 'error',
        })

        throw new Error(errorMessage)
      }

      // Success response - return the result
      return response.result as T
    }

    // Not a BaseResponse (should not happen with standardized API)
    // Return as-is for backward compatibility
    return response as T
  }

  /**
   * Show error toast manually
   */
  function showError(message: string, title = 'Lỗi') {
    toast.add({
      title,
      description: message,
      color: 'error',
    })
  }

  /**
   * Show success toast manually
   */
  function showSuccess(message: string, title = 'Thành công') {
    toast.add({
      title,
      description: message,
      color: 'success',
    })
  }

  /**
   * Show info toast manually
   */
  function showInfo(message: string, title = 'Thông báo') {
    toast.add({
      title,
      description: message,
      color: 'info',
    })
  }

  return {
    handleResponse,
    showError,
    showSuccess,
    showInfo,
  }
}
