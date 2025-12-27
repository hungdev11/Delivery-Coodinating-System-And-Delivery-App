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
  filters: FilterGroupItemV2 | null; // applied filters (V2 format)
  sorts: SortConfig[];   // sort config
  selected?: TKey[];     // optional selected IDs
}

// Query payload for POST endpoints (V2)
interface QueryPayload {
  filters?: FilterGroupItemV2 | null;  // V2 filter groups
  sorts?: SortConfig[];          // sort configuration
  page?: number;                 // default 0
  size?: number;                 // default 10
  search?: string;               // global search term
  selected?: string[];           // selected item IDs
}

// V2 Filter system types
interface FilterGroupItemV2 {
  type: 'group';
  items: FilterItemV2[];
}

type FilterItemV2 = FilterConditionItemV2 | FilterOperatorItemV2 | FilterGroupItemV2;

interface FilterConditionItemV2 {
  type: 'condition';
  field: string;
  operator: FilterOperator;
  value: any;
  caseSensitive?: boolean;
  id?: string;
}

interface FilterOperatorItemV2 {
  type: 'operator';
  value: 'AND' | 'OR';
}

interface SortConfig {
  field: string;
  direction: 'asc' | 'desc';
}

// Supported filter operators (enum values)
type FilterOperator =
  | 'EQUALS' | 'NOT_EQUALS'                    // equals, not equals
  | 'CONTAINS' | 'STARTS_WITH' | 'ENDS_WITH' | 'REGEX'  // string operations
  | 'IN' | 'NOT_IN'                 // array operations
  | 'GREATER_THAN' | 'GREATER_THAN_OR_EQUAL' | 'LESS_THAN' | 'LESS_THAN_OR_EQUAL'   // comparison
  | 'BETWEEN'                      // range
  | 'IS_NULL' | 'IS_NOT_NULL'        // null checks
  | 'IS_EMPTY' | 'IS_NOT_EMPTY'      // empty checks
  | 'CONTAINS_ANY' | 'CONTAINS_ALL' // array contains
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

### POST Endpoints (Standard - V2 Filter System)

All list/query endpoints use POST with V2 filter system.

Request (POST body):
```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "name",
        "operator": "CONTAINS",
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
        "type": "group",
        "items": [
          {
            "type": "condition",
            "field": "status",
            "operator": "EQUALS",
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

## V2 Filter Examples

### Basic Filters

```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "age",
        "operator": "GREATER_THAN_OR_EQUAL",
        "value": 18
      }
    ]
  }
}
```

This represents: `status = ACTIVE AND age >= 18`

### String Operations

```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "email",
        "operator": "CONTAINS",
        "value": "@gmail.com",
        "caseSensitive": false
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "name",
        "operator": "STARTS_WITH",
        "value": "John",
        "caseSensitive": true
      }
    ]
  }
}
```

### Complex Filters with Different Operators

V2 allows different operators between pairs:
```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "age",
        "operator": "GREATER_THAN_OR_EQUAL",
        "value": 18
      },
      {
        "type": "operator",
        "value": "OR"
      },
      {
        "type": "condition",
        "field": "role",
        "operator": "EQUALS",
        "value": "ADMIN"
      }
    ]
  }
}
```

This represents: `status = ACTIVE AND age >= 18 OR role = ADMIN`

### Nested Groups

```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "group",
        "items": [
          {
            "type": "condition",
            "field": "role",
            "operator": "EQUALS",
            "value": "ADMIN"
          },
          {
            "type": "operator",
            "value": "OR"
          },
          {
            "type": "condition",
            "field": "role",
            "operator": "EQUALS",
            "value": "MANAGER"
          }
        ]
      }
    ]
  }
}
```

This represents: `status = ACTIVE AND (role = ADMIN OR role = MANAGER)`

### Range Filters

```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "salary",
        "operator": "BETWEEN",
        "value": [50000, 100000]
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "createdAt",
        "operator": "GREATER_THAN_OR_EQUAL",
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
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "createdAt",
        "operator": "BETWEEN",
        "value": ["2024-01-01", "2024-12-31"]
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "updatedAt",
        "operator": "GREATER_THAN_OR_EQUAL",
        "value": "2024-06-01"
      }
    ]
  }
}
```

### Null/Empty Checks

```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "phone",
        "operator": "IS_NOT_NULL"
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "description",
        "operator": "IS_NOT_EMPTY"
      }
    ]
  }
}
```

### Array Operations

```json
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "role",
        "operator": "IN",
        "value": ["ADMIN", "MANAGER", "USER"]
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "tags",
        "operator": "CONTAINS_ANY",
        "value": ["urgent", "important"]
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
POST /api/v2/users
{
  "filters": {
    "type": "group",
    "items": [
      {
        "type": "condition",
        "field": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      }
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
        "type": "group",
        "items": [
          {
            "type": "condition",
            "field": "status",
            "operator": "EQUALS",
            "value": "ACTIVE"
          }
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
GET /api/v1/users/uuid-1

{
  "result": { "id": "uuid-1", "name": "John Doe", "status": "ACTIVE" }
}
```

### Create Item (POST)
Request:
```json
POST /api/v1/users
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
