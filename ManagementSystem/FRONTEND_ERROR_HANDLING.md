# Frontend Error Handling - ManagementSystem

## What Was Added

### 1. BaseResponse Type Definition

**File**: `src/common/types/baseResponse.ts`

Type-safe definitions and utility functions for handling the standardized BaseResponse format:

- `BaseResponse<T>` interface matching backend format
- `PagedData<T>` interface for paginated responses
- Type guards: `isBaseResponse()`, `isErrorResponse()`, `isSuccessResponse()`, `isPagedData()`
- Helper: `unwrapBaseResponse()` for extracting result data

### 2. Error Handler Composable

**File**: `src/common/composables/useErrorHandler.ts`

Vue composable for automatic error handling:

- `handleResponse<T>()`: Automatically shows toast for errors and unwraps result
- `showError()`: Manual error toast
- `showSuccess()`: Manual success toast
- `showInfo()`: Manual info toast

### 3. Updated Axios Interceptor

**File**: `src/common/utils/axios.ts`

Response interceptor now:

- Handles both BaseResponse errors and network errors
- Shows Vietnamese error messages automatically
- Supports 401 authentication errors with user-friendly message

## API Response Formats

### Non-Paging Response

```typescript
interface BaseResponse<T> {
  result: T           // Contains the actual data
  message: string     // Empty on success, error message on failure
}

// Example: Get single user
GET /api/v1/users/{id}
Response: BaseResponse<UserDto>
{
  "result": { "id": "123", "name": "John" },
  "message": ""
}
```

### Paging Response

```typescript
interface BaseResponse<PagedData<T>> {
  result: PagedData<T>  // Contains paginated data
  message: string
}

interface PagedData<T> {
  data: T[]             // Array of items
  page: {
    page: number
    size: number
    totalElements: number
    totalPages: number
    filters?: any
    sorts?: any[]
  }
}

// Example: Get users list
POST /v1/users
Response: BaseResponse<PagedData<UserDto>>
{
  "result": {
    "data": [
      { "id": "123", "name": "John" },
      { "id": "456", "name": "Jane" }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 2,
      "totalPages": 1
    }
  },
  "message": ""
}
```

### Error Response

```typescript
{
  "result": null,
  "message": "Không tìm thấy dữ liệu"  // Vietnamese error message
}
```

## Usage Examples

### Automatic Error Handling - Non-Paging Endpoint

```typescript
import { useErrorHandler } from '@/common/composables/useErrorHandler'
import { getParcelById } from './api'
import type { BaseResponse } from '@/common/types/baseResponse'

export function useParcelDetails(id: string) {
  const { handleResponse, showSuccess } = useErrorHandler()
  const loading = ref(false)

  async function loadParcel() {
    loading.value = true
    try {
      const response: BaseResponse<ParcelDto> = await getParcelById(id)
      const parcel = handleResponse<ParcelDto>(response) // Auto-shows error toast if needed

      // Use parcel data - only executes if successful
      console.log('Parcel loaded:', parcel.code)
      return parcel
    } catch (error) {
      // Error already shown as toast, just handle cleanup
      console.error('Failed to load parcel')
    } finally {
      loading.value = false
    }
  }

  return { loadParcel, loading }
}
```

### Automatic Error Handling - Paging Endpoint

```typescript
import { useErrorHandler } from '@/common/composables/useErrorHandler'
import type { BaseResponse, PagedData } from '@/common/types/baseResponse'

export function useUsersList() {
  const { handleResponse } = useErrorHandler()
  const users = ref<UserDto[]>([])
  const pagination = ref({})

  async function loadUsers(query: QueryPayload) {
    try {
      const response: BaseResponse<PagedData<UserDto>> = await getUsersV2(query)
      const pagedData = handleResponse<PagedData<UserDto>>(response)

      // Access the data and pagination info
      users.value = pagedData.data // UserDto[]
      pagination.value = pagedData.page // Pagination info

      console.log(`Loaded ${pagedData.data.length} users`)
      console.log(`Total: ${pagedData.page.totalElements}`)
    } catch (error) {
      // Error already shown as toast
      console.error('Failed to load users')
    }
  }

  return { users, pagination, loadUsers }
}
```

### Manual Toast Notifications

```typescript
import { useErrorHandler } from '@/common/composables/useErrorHandler'

const { showSuccess, showError, showInfo } = useErrorHandler()

// Success message
await updateParcel(id, data)
showSuccess('Cập nhật bưu kiện thành công')

// Error message
if (!isValid) {
  showError('Dữ liệu không hợp lệ')
  return
}

// Info message
showInfo('Đang xử lý yêu cầu...')
```

### Direct BaseResponse Handling

```typescript
import type { BaseResponse, PagedData } from '@/common/types/baseResponse'
import { isErrorResponse, isPagedData } from '@/common/types/baseResponse'

// Non-paging response
const response: BaseResponse<ParcelDto> = await api.getParcel(id)

if (isErrorResponse(response)) {
  console.error(response.message) // Error message
} else {
  const parcel = response.result // ParcelDto
  console.log(parcel)
}

// Paging response
const response: BaseResponse<PagedData<UserDto>> = await api.getUsers(query)

if (response.result && isPagedData<UserDto>(response.result)) {
  const users = response.result.data // UserDto[]
  const totalCount = response.result.page.totalElements
  console.log(`${users.length} of ${totalCount} users`)
}
```

## Migration Guide

### Before (Old Format - No BaseResponse)

```typescript
// Old: API returned PageResponse directly
const pageResponse = await getUsers(query) // PageResponse<UserDto>
const users = pageResponse.data
const totalPages = pageResponse.page.totalPages
```

### After (New Format - With BaseResponse)

```typescript
// New: API returns BaseResponse<PagedData<T>>
const response = await getUsers(query) // BaseResponse<PagedData<UserDto>>
const pagedData = handleResponse(response) // Auto-handles errors

const users = pagedData.data // UserDto[]
const totalPages = pagedData.page.totalPages
```

### Type Definitions for API Calls

```typescript
// Non-paging endpoint
export const getParcelById = async (id: string): Promise<BaseResponse<ParcelDto>> => {
  return apiClient.get<BaseResponse<ParcelDto>>(`/v1/parcels/${id}`)
}

// Paging endpoint
export const getUsersV2 = async (
  params: QueryPayload,
): Promise<BaseResponse<PagedData<UserDto>>> => {
  return apiClient.post<BaseResponse<PagedData<UserDto>>>('/v1/users', params)
}
```

## Error Messages

All error messages from the backend are now in Vietnamese:

- `"Không tìm thấy dữ liệu"` - Not found
- `"Dữ liệu không hợp lệ"` - Invalid data
- `"Không có quyền truy cập"` - Access denied
- `"System have a technical issues!"` - Generic server error

The axios interceptor automatically shows these as toast notifications.

## Integration Status

✅ **Complete** - ManagementSystem now has:

- BaseResponse type definitions (including PagedData)
- Automatic error handling composable
- Updated axios interceptor with Vietnamese error messages
- Support for both non-paging and paging responses

## Important Notes

1. **All endpoints now use BaseResponse**:
   - Non-paging: `BaseResponse<T>`
   - Paging: `BaseResponse<PagedData<T>>`

2. **Use `handleResponse()`** to:
   - Automatically show error toasts
   - Unwrap the result for easier use
   - Works with both regular and paging responses

3. **Paging responses are wrapped**:
   - Old: `PageResponse<T>` directly
   - New: `BaseResponse<PagedData<T>>`
   - Access data: `pagedData.data` (array of items)
   - Access pagination: `pagedData.page` (pagination info)

4. **Type safety**:
   - Always specify the type parameter: `handleResponse<UserDto>(response)`
   - For paging: `handleResponse<PagedData<UserDto>>(response)`

## Next Steps for Developers

1. **Update API type definitions** to return `BaseResponse<T>` or `BaseResponse<PagedData<T>>`
2. **Use `useErrorHandler()`** in your components/composables
3. **Call `handleResponse()`** on all API responses for automatic error handling
4. **For paging responses**, access `result.data` and `result.page`
