# Filter API V0, V1, V2 - Quick Reference

## API Comparison

| Feature | V0 | V1 | V2 |
|---------|----|----|-----|
| Endpoint | `/api/v0/{resource}` | `/api/v1/{resource}` | `/api/v2/{resource}` |
| Method | POST | POST | POST |
| Dynamic Filters | ❌ No | ✅ Yes (group-level) | ✅ Yes (pair-level) |
| Sorting | ✅ Yes | ✅ Yes | ✅ Yes |
| Paging | ✅ Yes | ✅ Yes | ✅ Yes |
| Operations Between Pairs | ❌ No | ❌ No | ✅ Yes |
| Use Case | Simple lists | Basic filtering | Complex filtering |

## V0 - Simple Paging & Sorting

### Request
```json
POST /api/v0/users
Content-Type: application/json

{
  "page": 0,
  "size": 10,
  "sorts": [
    {
      "field": "username",
      "direction": "asc"
    },
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ],
  "search": "john",
  "selected": ["uuid-1", "uuid-2"]
}
```

### Response
```json
{
  "result": {
    "data": [
      {
        "id": "uuid-1",
        "username": "john.doe",
        "email": "john@example.com",
        "status": "ACTIVE"
      }
    ],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 1,
      "totalPages": 1,
      "filters": null,
      "sorts": [
        {"field": "username", "direction": "asc"},
        {"field": "createdAt", "direction": "desc"}
      ]
    }
  }
}
```

## V1 - Dynamic Filtering (Group-Level Operations)

### Simple Filter
```json
POST /api/v1/users
Content-Type: application/json

{
  "page": 0,
  "size": 10,
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
  },
  "sorts": [
    {"field": "createdAt", "direction": "desc"}
  ]
}
```

### Nested Groups (V1 Limitation Example)
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
            "operator": "eq",
            "value": "ADMIN"
          },
          {
            "field": "role",
            "operator": "eq",
            "value": "MANAGER"
          }
        ]
      }
    ]
  }
}
```
**Result**: `status=ACTIVE AND (role=ADMIN OR role=MANAGER)`

**Limitation**: Cannot express `status=ACTIVE AND role=ADMIN OR role=MANAGER` (without parentheses)

## V2 - Enhanced Filtering (Pair-Level Operations)

### Simple Filter with Mixed Operations
```json
POST /api/v2/users
Content-Type: application/json

{
  "page": 0,
  "size": 10,
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
  },
  "sorts": [
    {"field": "createdAt", "direction": "desc"}
  ]
}
```
**Result**: `status=ACTIVE AND age>=18 OR role=ADMIN`

### Complex Nested Filter
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
            "field": "email",
            "operator": "CONTAINS",
            "value": "@gmail.com",
            "caseSensitive": false
          },
          {
            "type": "operator",
            "value": "OR"
          },
          {
            "type": "condition",
            "field": "email",
            "operator": "CONTAINS",
            "value": "@yahoo.com",
            "caseSensitive": false
          }
        ]
      },
      {
        "type": "operator",
        "value": "AND"
      },
      {
        "type": "condition",
        "field": "age",
        "operator": "BETWEEN",
        "value": [18, 65]
      }
    ]
  }
}
```
**Result**: `status=ACTIVE AND (email contains @gmail.com OR email contains @yahoo.com) AND age BETWEEN 18 AND 65`

## Filter Operators

All versions support the same operators:

### Comparison
- `EQUALS` / `eq` - Equal to
- `NOT_EQUALS` / `ne` - Not equal to

### Numeric
- `GREATER_THAN` / `gt` - Greater than
- `GREATER_THAN_OR_EQUAL` / `gte` - Greater than or equal
- `LESS_THAN` / `lt` - Less than
- `LESS_THAN_OR_EQUAL` / `lte` - Less than or equal
- `BETWEEN` / `between` - Between two values

### String
- `CONTAINS` / `contains` - Contains substring
- `STARTS_WITH` / `startsWith` - Starts with
- `ENDS_WITH` / `endsWith` - Ends with
- `REGEX` / `regex` - Regular expression match

### Collection
- `IN` / `in` - In list
- `NOT_IN` / `notIn` - Not in list

### Null
- `IS_NULL` / `isNull` - Is null
- `IS_NOT_NULL` / `isNotNull` - Is not null

## Common Use Cases

### 1. Simple List (V0)
Get all users with sorting:
```json
POST /api/v0/users
{
  "page": 0,
  "size": 20,
  "sorts": [{"field": "username", "direction": "asc"}]
}
```

### 2. Active Users (V1)
Get all active users:
```json
POST /api/v1/users
{
  "page": 0,
  "size": 10,
  "filters": {
    "logic": "AND",
    "conditions": [
      {"field": "status", "operator": "eq", "value": "ACTIVE"}
    ]
  }
}
```

### 3. Complex Search (V2)
Get active adults OR admins:
```json
POST /api/v2/users
{
  "page": 0,
  "size": 10,
  "filters": {
    "type": "group",
    "items": [
      {"type": "condition", "field": "status", "operator": "EQUALS", "value": "ACTIVE"},
      {"type": "operator", "value": "AND"},
      {"type": "condition", "field": "age", "operator": "GREATER_THAN_OR_EQUAL", "value": 18},
      {"type": "operator", "value": "OR"},
      {"type": "condition", "field": "role", "operator": "EQUALS", "value": "ADMIN"}
    ]
  }
}
```

### 4. Date Range (V1 or V2)
Get users created in 2024:
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
      }
    ]
  }
}
```

### 5. Text Search with OR (V2)
Search in multiple fields:
```json
{
  "filters": {
    "type": "group",
    "items": [
      {"type": "condition", "field": "username", "operator": "CONTAINS", "value": "john"},
      {"type": "operator", "value": "OR"},
      {"type": "condition", "field": "email", "operator": "CONTAINS", "value": "john"},
      {"type": "operator", "value": "OR"},
      {"type": "condition", "field": "firstName", "operator": "CONTAINS", "value": "john"}
    ]
  }
}
```

## Testing with cURL

### V0
```bash
curl -X POST http://localhost:8080/api/v0/users \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"sorts":[{"field":"username","direction":"asc"}]}'
```

### V1
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"filters":{"logic":"AND","conditions":[{"field":"status","operator":"eq","value":"ACTIVE"}]}}'
```

### V2
```bash
curl -X POST http://localhost:8080/api/v2/users \
  -H "Content-Type: application/json" \
  -d '{"page":0,"size":10,"filters":{"type":"group","items":[{"type":"condition","field":"status","operator":"EQUALS","value":"ACTIVE"},{"type":"operator","value":"AND"},{"type":"condition","field":"age","operator":"GREATER_THAN_OR_EQUAL","value":18}]}}'
```

## When to Use Each Version

- **Use V0** when:
  - You only need simple paging and sorting
  - Filters will be implemented by the caller (e.g., predefined queries)
  - You want minimal payload size

- **Use V1** when:
  - You need basic dynamic filtering
  - All conditions in a group use the same operator (AND or OR)
  - You're maintaining backward compatibility

- **Use V2** when:
  - You need different operators between different pairs of conditions
  - You're building complex search interfaces
  - You need maximum flexibility in filter expressions
