# Frontend Error Handling - DeliveryApp (Android)

## What Was Added

### ResponseHandler Utility

**File**: `app/src/main/java/com/ds/deliveryapp/utils/ResponseHandler.java`

Utility class for handling BaseResponse format and showing error messages via Toast or Snackbar:

-   `handleResponse(Context, BaseResponse<T>)`: Auto-show Toast on error, return result on success
-   `handleResponse(View, BaseResponse<T>)`: Auto-show Snackbar on error, return result on success
-   `isError()`, `isSuccess()`: Check response status
-   `getErrorMessage()`: Extract error message
-   Manual methods: `showError()`, `showSuccess()`, `showInfo()`

## Usage Examples

### Basic Usage with Toast

```java
import com.ds.deliveryapp.utils.ResponseHandler;
import com.ds.deliveryapp.clients.res.BaseResponse;

public class MainActivity extends AppCompatActivity {

    private void loadSession(String sessionId) {
        Call<BaseResponse<DeliverySession>> call = sessionClient.getSessionById(sessionId);

        call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliverySession>> call,
                                   Response<BaseResponse<DeliverySession>> response) {
                if (response.isSuccessful()) {
                    // Auto-handle: shows Toast if error, returns data if success
                    DeliverySession session = ResponseHandler.handleResponse(
                        MainActivity.this,
                        response.body()
                    );

                    if (session != null) {
                        // Use session data - only executes if successful
                        updateUI(session);
                    }
                    // Error already shown as Toast if session is null
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliverySession>> call, Throwable t) {
                ResponseHandler.showError(MainActivity.this, "Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
```

### Usage with Snackbar (in Fragment)

```java
import com.ds.deliveryapp.utils.ResponseHandler;

public class TaskFragment extends Fragment {

    private void completeTask(String parcelId) {
        Call<BaseResponse<DeliveryAssignment>> call = sessionClient.completeTask(
            deliveryManId, parcelId, routeInfo
        );

        call.enqueue(new Callback<BaseResponse<DeliveryAssignment>>() {
            @Override
            public void onResponse(Call<BaseResponse<DeliveryAssignment>> call,
                                   Response<BaseResponse<DeliveryAssignment>> response) {
                if (response.isSuccessful()) {
                    // Use Snackbar instead of Toast
                    DeliveryAssignment assignment = ResponseHandler.handleResponse(
                        requireView(),  // Pass View for Snackbar
                        response.body()
                    );

                    if (assignment != null) {
                        ResponseHandler.showSuccess(requireView(), "Hoàn thành giao hàng!");
                        refreshTaskList();
                    }
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<DeliveryAssignment>> call, Throwable t) {
                ResponseHandler.showError(requireView(), "Lỗi kết nối");
            }
        });
    }
}
```

### Manual Error Checking

```java
import com.ds.deliveryapp.utils.ResponseHandler;

BaseResponse<DeliverySession> response = ...;

if (ResponseHandler.isError(response)) {
    // Handle error
    String errorMsg = ResponseHandler.getErrorMessage(response);
    Log.e(TAG, "Error: " + errorMsg);
    // Error message is in Vietnamese from backend
} else {
    // Use data
    DeliverySession session = response.getResult();
    updateUI(session);
}
```

### Manual Toast Notifications

```java
import com.ds.deliveryapp.utils.ResponseHandler;

// Success message
ResponseHandler.showSuccess(this, "Cập nhật thành công");

// Error message
ResponseHandler.showError(this, "Không thể tải dữ liệu");

// Info message
ResponseHandler.showInfo(this, "Đang xử lý...");
```

## API Response Types

### Success Response (Non-Paging)

```java
public class BaseResponse<T> {
    private T result;        // Contains the actual data
    private String message;  // Empty or null on success
}
```

### Error Response

```java
public class BaseResponse<T> {
    private T result;        // null on error
    private String message;  // Vietnamese error message
}
```

### Paging Response (Unchanged)

Paging endpoints still return `PageResponse` directly:

```java
public class PageResponse<T> {
    private List<T> data;
    private Page page;
}
```

## Migration Guide

### Before (Old Format)

```java
Call<DeliverySession> call = sessionClient.getSessionById(sessionId);
call.enqueue(new Callback<DeliverySession>() {
    @Override
    public void onResponse(Call<DeliverySession> call, Response<DeliverySession> response) {
        if (response.isSuccessful()) {
            DeliverySession session = response.body();
            updateUI(session);
        }
    }
    // ...
});
```

### After (New Format - Automatic)

```java
Call<BaseResponse<DeliverySession>> call = sessionClient.getSessionById(sessionId);
call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
    @Override
    public void onResponse(Call<BaseResponse<DeliverySession>> call,
                           Response<BaseResponse<DeliverySession>> response) {
        if (response.isSuccessful()) {
            // Auto-shows Toast if error
            DeliverySession session = ResponseHandler.handleResponse(this, response.body());
            if (session != null) {
                updateUI(session);
            }
        }
    }
    // ...
});
```

### After (New Format - Manual)

```java
Call<BaseResponse<DeliverySession>> call = sessionClient.getSessionById(sessionId);
call.enqueue(new Callback<BaseResponse<DeliverySession>>() {
    @Override
    public void onResponse(Call<BaseResponse<DeliverySession>> call,
                           Response<BaseResponse<DeliverySession>> response) {
        if (response.isSuccessful()) {
            BaseResponse<DeliverySession> baseResponse = response.body();

            if (baseResponse.getResult() != null) {
                // Success
                DeliverySession session = baseResponse.getResult();
                updateUI(session);
            } else {
                // Error - show Vietnamese error message
                String errorMsg = baseResponse.getMessage();
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }
    // ...
});
```

## Error Messages

All error messages from the backend are now in Vietnamese:

-   `"Không tìm thấy dữ liệu"` - Not found
-   `"Dữ liệu không hợp lệ"` - Invalid data
-   `"Không có quyền truy cập"` - Access denied
-   `"System have a technical issues!"` - Generic server error

The ResponseHandler utility automatically displays these messages.

## Best Practices

1. **Use `handleResponse()` for automatic error handling** - simplest approach
2. **Use Toast for Activities**, Snackbar for Fragments (better UX)
3. **Always check for null** after `handleResponse()` before using result
4. **Paging endpoints** don't use BaseResponse wrapper - call them directly
5. **Network errors** (onFailure) should still show manual error messages

## Integration Status

✅ **Complete** - DeliveryApp now has:

-   ResponseHandler utility for automatic error handling
-   Support for both Toast and Snackbar notifications
-   Vietnamese error messages from backend

## Retrofit Client Updates Required

Update your Retrofit client interfaces to return `Call<BaseResponse<T>>` for non-paging endpoints:

```java
// Before
@GET("/api/v1/sessions/{sessionId}")
Call<DeliverySession> getSessionById(@Path("sessionId") String sessionId);

// After
@GET("/api/v1/sessions/{sessionId}")
Call<BaseResponse<DeliverySession>> getSessionById(@Path("sessionId") String sessionId);
```

Note: Some endpoints in `SessionClient.java` already use `BaseResponse` (like `getActiveSession`). Update the remaining endpoints to use the same pattern.
