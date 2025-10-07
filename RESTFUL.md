# RESTful JSON Quick Guide

All responses are wrapped in BaseResponse. This guide shows only JSON payloads for quick reference.

## Types (reference)

```ts
// Base response wrapper
interface BaseResponse<T> {
  result?: T;        // optional data
  message?: string;  // optional message
}

// Paginated data wrapper
interface PagedData<T> {
  data: T[];         // list of items
  page: Paging<string>; // pagination info
}

// Pagination info
interface Paging<TKey> {
  page: number;          // 0-based
  size: number;          // items per page
  totalElements: number; // total items
  totalPages: number;    // total pages
  filters: any[];        // applied filters
  sorts: any[];          // sort config
  selected?: TKey[];     // optional selected IDs
}

// Paging request (query)
interface PagingRequest {
  page?: number;         // default 0
  size?: number;         // default 10
  filters?: any[];
  sorts?: any[];
  selected?: string[];
}
```

---

## BaseResponse

Success:
```json
{
  "result": { "id": "uuid", "name": "Item" }
}
```

Success with message:
```json
{
  "result": { "id": "uuid" },
  "message": "Created successfully"
}
```

Error:
```json
{
  "message": "Error description"
}
```

---

## Pagination

Request (query string):
```
?page=0&size=10&filters=[]&sorts=[]&selected=[]
```

Response (BaseResponse<PagedData<T>>):
```json
{
  "result": {
    "data": [
      { "id": "uuid-1", "name": "Item 1" },
      { "id": "uuid-2", "name": "Item 2" }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10,
      "filters": [],
      "sorts": [],
      "selected": []
    }
  }
}
```

---

## CRUD Payloads

Create request:
```json
{
  "code": "ITEM001",
  "name": "Item Name",
  "description": "Optional"
}
```

Create success response:
```json
{
  "result": {
    "id": "uuid",
    "code": "ITEM001",
    "name": "Item Name",
    "description": "Optional"
  },
  "message": "Item created successfully"
}
```

Read (get by id) success response:
```json
{
  "result": {
    "id": "uuid",
    "code": "ITEM001",
    "name": "Item Name",
    "description": null
  }
}
```

Update request (partial):
```json
{
  "name": "Updated Name",
  "description": null
}
```

Update success response:
```json
{
  "result": {
    "id": "uuid",
    "code": "ITEM001",
    "name": "Updated Name",
    "description": null
  },
  "message": "Item updated successfully"
}
```

Delete success response:
```json
{
  "result": null,
  "message": "Item deleted successfully"
}
```

---

## Errors

Validation failed:
```json
{
  "message": "Validation failed"
}
```

Business rule violated:
```json
{
  "message": "Item with this code already exists"
}
```

Not found:
```json
{
  "message": "Item not found"
}
```

---

## Quick Examples

List (paginated):
```json
{
  "result": {
    "data": [{ "id": "uuid", "name": "Item" }],
    "page": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "filters": [], "sorts": [], "selected": [] }
  }
}
```

Single item:
```json
{
  "result": { "id": "uuid", "code": "CODE", "name": "Name" }
}
```

Minimal create request/response:
```json
{
  "request": { "code": "CODE", "name": "Name" },
  "response": { "result": { "id": "uuid", "code": "CODE", "name": "Name" }, "message": "Created successfully" }
}
```
