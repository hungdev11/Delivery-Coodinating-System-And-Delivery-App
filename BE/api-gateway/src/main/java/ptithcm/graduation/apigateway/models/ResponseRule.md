## Basic response
```java
    public class ItemToResponse {
        public String message;
    }

    public CompletableFuture<BaseResponse<ItemToResponse>> execute(T request) {
        try {
            ItemToResponse result = service.execute(request);
            return CompletableFuture.completedFuture(new BaseResponse<ItemToResponse>(result));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
```

## Paged response
```java
    public class ItemToResponse {
        public String message;
    }

    public CompletableFuture<PagedResult<ItemToResponse>> execute(T request) {
        try {
            PagedResult<ItemToResponse> result = service.execute(request);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
```

## Paged response with custom paging
```java
    public class ItemToResponse {
        public String message;
    }

    public class PagingToResponse extends Paging {
        public Map<String, String> filterMapping;
    }

    public CompletableFuture<CustomPagedResult<ItemToResponse, PagingToResponse>> execute(T request, PagingToResponse paging) {
        try {
            CustomPagedResult<ItemToResponse, PagingToResponse> result = service.execute(request, paging);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
```
