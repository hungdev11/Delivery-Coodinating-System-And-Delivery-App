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
  filters: FilterGroup | null; // applied filters
  sorts: SortConfig[];   // sort config
  selected?: TKey[];     // optional selected IDs
}

// Query payload for POST endpoints
interface QueryPayload {
  filters?: FilterGroup | null;  // MongoDB-style filter groups
  sorts?: SortConfig[];          // sort configuration
  page?: number;                 // default 0
  size?: number;                 // default 10
  search?: string;               // global search term
  selected?: string[];           // selected item IDs
}

// Filter system types
interface FilterGroup {
  logic: 'AND' | 'OR';
  conditions: (FilterCondition | FilterGroup)[];
}

interface FilterCondition {
  field: string;
  operator: FilterOperator;
  value: any;
  caseSensitive?: boolean;
  id?: string;
}

interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

// Supported filter operators
type FilterOperator =
  | 'eq' | 'ne'                    // equals, not equals
  | 'contains' | 'startsWith' | 'endsWith' | 'regex'  // string operations
  | 'in' | 'notIn'                 // array operations
  | 'gt' | 'gte' | 'lt' | 'lte'   // comparison
  | 'between'                      // range
  | 'isNull' | 'isNotNull'        // null checks
  | 'isEmpty' | 'isNotEmpty'      // empty checks
  | 'containsAny' | 'containsAll' // array contains
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

## Pagination & Filtering

### GET Endpoints (Legacy - Deprecated)
Request (query string):
```
?page=0&size=10&filters=[]&sorts=[]&selected=[]
```

### POST Endpoints (New Standard)
Request (POST body):
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "status",
        "operator": "eq",
        "value": "ACTIVE"
      },
      {
        "field": "name",
        "operator": "contains",
        "value": "john",
        "caseSensitive": false
      }
    ]
  },
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    },
    {
      "field": "name",
      "direction": "asc"
    }
  ],
  "page": 0,
  "size": 10,
  "search": "global search term",
  "selected": ["uuid-1", "uuid-2"]
}
```

Response (BaseResponse<PagedData<T>>):
```json
{
  "result": {
    "data": [
      { "id": "uuid-1", "name": "Item 1", "status": "ACTIVE" },
      { "id": "uuid-2", "name": "Item 2", "status": "ACTIVE" }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10,
      "filters": {
        "logic": "AND",
        "conditions": [
          {
            "field": "status",
            "operator": "eq",
            "value": "ACTIVE"
          }
        ]
      },
      "sorts": [
        {
          "field": "createdAt",
          "direction": "desc"
        }
      ],
      "selected": ["uuid-1", "uuid-2"]
    }
  }
}
```

---

## API Migration Guide: GET → POST

### Why POST for List/Query Operations?

1. **Complex Filtering**: GET query strings have length limits and encoding issues with complex nested filters
2. **Security**: Sensitive filter data is not exposed in URL logs
3. **Flexibility**: JSON payload allows for complex nested structures
4. **Consistency**: All query operations use the same endpoint pattern

### Migration Steps

#### 1. Update Frontend API Calls
```typescript
// OLD (GET)
const response = await apiClient.get('/v1/users', {
  params: { page: 0, size: 10, search: 'john' }
})

// NEW (POST)
const response = await apiClient.post('/v1/users', {
  page: 0,
  size: 10,
  search: 'john',
  filters: {
    logic: 'AND',
    conditions: [
      { field: 'status', operator: 'eq', value: 'ACTIVE' }
    ]
  },
  sorts: [
    { field: 'name', direction: 'asc' }
  ]
})
```

#### 2. Update Backend Controllers
```java
// OLD (Spring Boot)
@GetMapping("/users")
public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String search
) { ... }

// NEW (Spring Boot)
@PostMapping("/users")
public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(
    @RequestBody QueryPayload query
) { ... }
```

```typescript
// OLD (Node.js/Express)
app.get('/v1/users', async (req, res) => {
  const { page = 0, size = 10, search } = req.query
  // ...
})

// NEW (Node.js/Express)
app.post('/v1/users', async (req, res) => {
  const { page = 0, size = 10, search, filters, sorts } = req.body
  // ...
})
```

#### 3. Update API Documentation
- Change HTTP method from GET to POST
- Move parameters from query string to request body
- Update examples to show JSON payload structure

### Backward Compatibility

During migration period, maintain both endpoints:
- `GET /v1/users` (legacy, deprecated)
- `POST /v1/users` (new standard)

Add deprecation warnings to GET endpoints:
```json
{
  "result": { ... },
  "message": "This endpoint is deprecated. Use POST /v1/users instead."
}
```

---

## Filter Examples

### Basic Filters
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "status",
        "operator": "eq",
        "value": "ACTIVE"
      },
      {
        "field": "age",
        "operator": "gte",
        "value": 18
      }
    ]
  }
}
```

### String Operations
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "email",
        "operator": "contains",
        "value": "@gmail.com",
        "caseSensitive": false
      },
      {
        "field": "name",
        "operator": "startsWith",
        "value": "John",
        "caseSensitive": true
      }
    ]
  }
}
```

### Complex Nested Filters
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "status",
        "operator": "eq",
        "value": "ACTIVE"
      },
      {
        "logic": "OR",
        "conditions": [
          {
            "field": "role",
            "operator": "in",
            "value": ["ADMIN", "MANAGER"]
          },
          {
            "field": "department",
            "operator": "eq",
            "value": "IT"
          }
        ]
      }
    ]
  }
}
```

### Range Filters
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "salary",
        "operator": "between",
        "value": [50000, 100000]
      },
      {
        "field": "createdAt",
        "operator": "gte",
        "value": "2024-01-01"
      }
    ]
  }
}
```

### Date Range Filters
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "createdAt",
        "operator": "between",
        "value": ["2024-01-01", "2024-12-31"]
      },
      {
        "field": "updatedAt",
        "operator": "gte",
        "value": "2024-06-01"
      }
    ]
  }
}
```

### Date Comparison Filters
```json
{
  "filters": {
    "logic": "OR",
    "conditions": [
      {
        "field": "createdAt",
        "operator": "gt",
        "value": "2024-01-01"
      },
      {
        "field": "createdAt",
        "operator": "lt",
        "value": "2024-12-31"
      }
    ]
  }
}
```

### Null/Empty Checks
```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "phone",
        "operator": "isNotNull"
      },
      {
        "field": "description",
        "operator": "isNotEmpty"
      }
    ]
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

### List with Filtering (POST)
Request:
```json
POST /v1/users
{
  "filters": {
    "logic": "AND",
    "conditions": [
      { "field": "status", "operator": "eq", "value": "ACTIVE" }
    ]
  },
  "sorts": [
    { "field": "name", "direction": "asc" }
  ],
  "page": 0,
  "size": 10
}
```

Response:
```json
{
  "result": {
    "data": [
      { "id": "uuid-1", "name": "John Doe", "status": "ACTIVE" },
      { "id": "uuid-2", "name": "Jane Smith", "status": "ACTIVE" }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 2,
      "totalPages": 1,
      "filters": {
        "logic": "AND",
        "conditions": [
          { "field": "status", "operator": "eq", "value": "ACTIVE" }
        ]
      },
      "sorts": [
        { "field": "name", "direction": "asc" }
      ],
      "selected": []
    }
  }
}
```

### Single Item (GET)
```json
GET /v1/users/uuid-1

{
  "result": { "id": "uuid-1", "name": "John Doe", "status": "ACTIVE" }
}
```

### Create Item (POST)
Request:
```json
POST /v1/users
{
  "name": "John Doe",
  "email": "john@example.com",
  "status": "ACTIVE"
}
```

Response:
```json
{
  "result": {
    "id": "uuid-1",
    "name": "John Doe",
    "email": "john@example.com",
    "status": "ACTIVE"
  },
  "message": "User created successfully"
}
```

---

## Header Rules

### Standard Headers

All API requests and WebSocket connections must follow these header conventions:

#### For REST API Requests

**Clients (ManagementSystem, DeliveryApp):**
- **Authorization**: `Bearer <JWT_TOKEN>` - JWT token for authentication
- **Content-Type**: `application/json` (for POST/PUT requests)

**API Gateway:**
- Extracts user ID and roles from JWT token
- Forwards to downstream services:
  - **X-User-Id**: User ID extracted from JWT token
  - **X-User-Roles**: Comma-separated list of user roles (e.g., `ADMIN,SHIPPER,CLIENT`)
  - Filters out Keycloak default roles (`default-roles-delivery-system`, `offline_access`, `uma_authorization`)

**Downstream Services:**
- Receive headers from API Gateway:
  - **X-User-Id**: User ID (required for authenticated endpoints)
  - **X-User-Roles**: Comma-separated roles (optional, for role-based authorization)

#### For WebSocket Connections

**Clients (ManagementSystem, DeliveryApp):**
- **Authorization**: `Bearer <USER_ID>` - User ID (not JWT token) for WebSocket authentication
- **X-User-Id**: User ID (for consistency)
- **X-User-Roles**: Comma-separated list of user roles (optional)
- **Client-Type**: `WEB` (ManagementSystem) or `ANDROID` (DeliveryApp)

**Communication Service:**
- Accepts WebSocket connections with:
  - **Authorization** header (preferred): `Bearer <USER_ID>`
  - **X-User-Id** header (fallback): User ID if Authorization header is missing
  - **X-User-Roles** header (optional): For logging and future role-based features
  - **Client-Type** header: To identify client type for targeted notifications

### Header Examples

#### REST API Request (from Client)
```http
POST /api/v1/parcels
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "code": "PARCEL001",
  "senderId": "user-123"
}
```

#### API Gateway → Downstream Service
```http
POST /api/v1/parcels
X-User-Id: user-123
X-User-Roles: ADMIN,SHIPPER
Content-Type: application/json

{
  "code": "PARCEL001",
  "senderId": "user-123"
}
```

#### WebSocket Connection (ManagementSystem)
```javascript
const client = new Client({
  connectHeaders: {
    Authorization: `Bearer ${userId}`,
    'X-User-Id': userId,
    'X-User-Roles': 'ADMIN,CLIENT',
    'Client-Type': 'WEB'
  }
})
```

#### WebSocket Connection (DeliveryApp)
```java
Map<String, String> headers = new HashMap<>();
headers.put("Authorization", "Bearer " + userId);
headers.put("X-User-Id", userId);
headers.put("X-User-Roles", String.join(",", userRoles));
headers.put("Client-Type", "ANDROID");
```

### Important Notes

1. **JWT Token vs User ID**: 
   - REST API uses JWT token in `Authorization` header
   - WebSocket uses User ID in `Authorization` header (not JWT token)

2. **Role Format**: 
   - Roles are comma-separated strings: `"ADMIN,SHIPPER,CLIENT"`
   - No spaces after commas
   - Sorted alphabetically for consistency

3. **Header Forwarding**:
   - API Gateway automatically extracts and forwards `X-User-Id` and `X-User-Roles`
   - Clients should NOT manually set these headers for REST API requests
   - Clients MUST set these headers for WebSocket connections

4. **Backward Compatibility**:
   - Services should handle missing `X-User-Roles` header gracefully
   - `X-User-Id` is required for authenticated endpoints
