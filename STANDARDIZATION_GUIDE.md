# API Response Standardization Guide

**Version**: 1.0  
**Last Updated**: 2025-11-23  
**Status**: ✅ Implemented across all backend services

## Table of Contents

1. [Overview](#overview)
2. [BaseResponse Structure](#baseresponse-structure)
3. [Controller Guidelines](#controller-guidelines)
4. [Exception Handling](#exception-handling)
5. [Logging Standards](#logging-standards)
6. [Frontend Integration](#frontend-integration)
7. [Examples](#examples)

---

## Overview

All backend services now use a standardized `BaseResponse<T>` wrapper for API responses. This ensures:

-   **Consistent API contract** across all microservices
-   **User-friendly error messages** in Vietnamese
-   **Clean separation** between success data and error messages
-   **Simplified frontend** error handling

### Services Standardized

✅ Session Service  
✅ Communication Service  
✅ Parcel Service  
✅ User Service  
✅ Settings Service  
✅ API Gateway

---

## BaseResponse Structure

### Location

Each service has its own `BaseResponse` class in the common/dto package:

```
{service}/common/entities/dto/common/BaseResponse.java
```

### Class Definition

```java
@Data
@Builder
public class BaseResponse<T> {
    private T result;      // Contains response data on success, null on error
    private String message; // Contains error message on failure, empty on success

    public static <T> BaseResponse<T> success(T result) {
        return BaseResponse.<T>builder()
                .result(result)
                .build();
    }

    public static <T> BaseResponse<T> error(String message) {
        return BaseResponse.<T>builder()
                .message(message)
                .build();
    }
}
```

### Response Format

**Success Response**:

```json
{
	"result": {
		"id": "123",
		"name": "Example Data"
	},
	"message": ""
}
```

**Error Response**:

```json
{
	"result": null,
	"message": "Không tìm thấy dữ liệu"
}
```

---

## Controller Guidelines

### ✅ DO: Wrap all non-paging endpoints

```java
@GetMapping("/{id}")
public ResponseEntity<BaseResponse<UserDto>> getUser(@PathVariable String id) {
    log.debug("Get user by ID: {}", id);
    UserDto user = userService.getUser(id);
    return ResponseEntity.ok(BaseResponse.success(user));
}

@PostMapping
public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
    log.debug("Create user: {}", request.getUsername());
    UserDto created = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(BaseResponse.success(created));
}
```

### ✅ DO: Keep paging endpoints unchanged

Paging endpoints use `PageResponse` or `PagedData` wrappers and should **NOT** be wrapped in BaseResponse:

```java
@GetMapping
public ResponseEntity<PageResponse<ParcelDto>> getParcels(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    log.debug("Get parcels page={} size={}", page, size);
    PageResponse<ParcelDto> parcels = parcelService.getParcels(page, size);
    return ResponseEntity.ok(parcels); // NO BaseResponse wrapper
}
```

### ❌ DON'T: Use old signature with message parameter

```java
// ❌ WRONG - Old signature (deprecated)
return ResponseEntity.ok(BaseResponse.success(data, "Success message"));

// ✅ CORRECT - New signature
return ResponseEntity.ok(BaseResponse.success(data));
```

### ❌ DON'T: Return raw DTOs

```java
// ❌ WRONG
@GetMapping("/{id}")
public ResponseEntity<UserDto> getUser(@PathVariable String id) {
    return ResponseEntity.ok(userService.getUser(id));
}

// ✅ CORRECT
@GetMapping("/{id}")
public ResponseEntity<BaseResponse<UserDto>> getUser(@PathVariable String id) {
    return ResponseEntity.ok(BaseResponse.success(userService.getUser(id)));
}
```

---

## Exception Handling

### GlobalExceptionHandler Pattern

Every service must have a `GlobalExceptionHandler` that returns `BaseResponse.error()`:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        log.error("[service-name] [GlobalExceptionHandler.handleNotFound] Resource not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("Không tìm thấy dữ liệu"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Object>> handleBadRequest(IllegalArgumentException ex) {
        log.error("[service-name] [GlobalExceptionHandler.handleBadRequest] Invalid argument", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Dữ liệu không hợp lệ"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(Exception ex) {
        log.error("[service-name] [GlobalExceptionHandler.handleGlobalException] Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("System have a technical issues!"));
    }
}
```

### Error Message Guidelines

1. **Use Vietnamese** for user-facing error messages
2. **Keep messages short** and user-friendly
3. **Don't expose** technical details or stack traces
4. **Use generic message** for unexpected errors: "System have a technical issues!"

**Common Vietnamese Error Messages**:

```java
"Không tìm thấy dữ liệu"              // Not found
"Dữ liệu không hợp lệ"                 // Invalid data
"Thiếu thông tin bắt buộc"             // Missing required fields
"Không có quyền truy cập"              // Access denied
"Dữ liệu đã tồn tại"                   // Already exists
"System have a technical issues!"      // Generic error (English)
```

---

## Logging Standards

### Log Levels

**Use `log.debug()` for routine operations**:

```java
log.debug("Get user by ID: {}", id);
log.debug("Create parcel with code: {}", code);
log.debug("Update session status to: {}", status);
```

**Use `log.error()` for exceptions only**:

```java
log.error("[service-name] [ClassName.methodName] Error description", exception);
```

**Never use `log.info()` or `log.warn()` in production code**.

### Logging Pattern

**Controllers**:

```java
log.debug("Action description with param: {}", param);
```

**Exception Handlers**:

```java
log.error("[service-name] [GlobalExceptionHandler.methodName] Error description", ex);
```

### ❌ DON'T: Use emojis or verbose messages

```java
// ❌ WRONG
log.info("🚀 GET /api/v1/users/{} - Fetching user details", id);

// ✅ CORRECT
log.debug("Get user by ID: {}", id);
```

---

## Frontend Integration

### TypeScript/JavaScript Example

```typescript
interface BaseResponse<T> {
	result: T | null;
	message: string;
}

async function callAPI<T>(url: string, options?: RequestInit): Promise<T> {
	const response = await fetch(url, options);

	if (!response.ok) {
		throw new Error(`HTTP ${response.status}`);
	}

	const data: BaseResponse<T> = await response.json();

	if (data.result !== null) {
		return data.result;
	} else {
		// data.message contains Vietnamese error message
		throw new Error(data.message || 'Unknown error');
	}
}

// Usage
try {
	const user = await callAPI<UserDto>('/api/v1/users/123');
	console.log('User:', user);
} catch (error) {
	toast.error(error.message); // Shows Vietnamese error to user
}
```

### Flutter/Dart Example

```dart
class BaseResponse<T> {
  final T? result;
  final String? message;

  BaseResponse({this.result, this.message});

  factory BaseResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJsonT,
  ) {
    return BaseResponse(
      result: json['result'] != null ? fromJsonT(json['result']) : null,
      message: json['message'] as String?,
    );
  }
}

Future<T> callAPI<T>(String url, T Function(Map<String, dynamic>) fromJson) async {
  final response = await http.get(Uri.parse(url));
  final baseResponse = BaseResponse.fromJson(
    jsonDecode(response.body),
    fromJson,
  );

  if (baseResponse.result != null) {
    return baseResponse.result!;
  } else {
    throw Exception(baseResponse.message ?? 'Unknown error');
  }
}
```

---

## Examples

### Complete Controller Example

```java
@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
@Slf4j
public class ParcelController {

    private final IParcelService parcelService;

    @PostMapping
    public ResponseEntity<BaseResponse<ParcelResponse>> createParcel(
            @Valid @RequestBody ParcelCreateRequest request) {
        log.debug("Create parcel with code: {}", request.getCode());
        ParcelResponse response = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    @GetMapping("/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> getParcelById(
            @PathVariable UUID parcelId) {
        log.debug("Get parcel by ID: {}", parcelId);
        ParcelResponse response = parcelService.getParcelById(parcelId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> updateParcel(
            @PathVariable UUID parcelId,
            @Valid @RequestBody ParcelUpdateRequest request) {
        log.debug("Update parcel: {}", parcelId);
        ParcelResponse response = parcelService.updateParcel(parcelId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @DeleteMapping("/{parcelId}")
    public ResponseEntity<Void> deleteParcel(@PathVariable UUID parcelId) {
        log.debug("Delete parcel: {}", parcelId);
        parcelService.deleteParcel(parcelId);
        return ResponseEntity.noContent().build();
    }

    // Paging endpoint - NO BaseResponse wrapper
    @GetMapping
    public ResponseEntity<PageResponse<ParcelResponse>> getParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Get parcels page={} size={}", page, size);
        PageResponse<ParcelResponse> response = parcelService.getParcels(page, size);
        return ResponseEntity.ok(response);
    }
}
```

---

## Migration Checklist

When adding new endpoints or updating existing ones:

-   [ ] Return type is `ResponseEntity<BaseResponse<YourDto>>`
-   [ ] Success uses `BaseResponse.success(data)` (no message parameter)
-   [ ] Paging endpoints use `PageResponse`/`PagedData` directly (no BaseResponse)
-   [ ] Logging uses `log.debug()` for routine operations
-   [ ] No emojis in log messages
-   [ ] GlobalExceptionHandler returns `BaseResponse.error()` with Vietnamese message
-   [ ] Import statement: `import {package}.common.entities.dto.common.BaseResponse;`

---

## FAQ

**Q: Should I wrap PageResponse in BaseResponse?**  
A: No. Paging responses already have their own structure and should not be wrapped.

**Q: What about DELETE endpoints that return void?**  
A: DELETE can return `ResponseEntity<Void>` with `noContent()` status, no BaseResponse needed.

**Q: Can I add custom messages to success responses?**  
A: No. The new standard uses `BaseResponse.success(data)` only. Messages are for errors only.

**Q: What if I need to return both data and a message?**  
A: Include the message in your DTO if needed. The BaseResponse message field is for errors only.

**Q: Should service layers also return BaseResponse?**  
A: No. Services should return plain DTOs or domain objects. Only controllers wrap responses in BaseResponse.

---

## Support

For questions or issues with the standardization:

1. Check this guide first
2. Review the [walkthrough.md](file:///C:/Users/phamb/.gemini/antigravity/brain/e2b0e360-f8ec-4fe8-899b-6c3b5519ddd4/walkthrough.md) for implementation examples
3. Contact the backend team lead

**Last Updated**: 2025-11-23  
**Maintained By**: Backend Development Team
