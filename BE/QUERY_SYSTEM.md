# Query System Documentation

This document describes the query and filtering system used across all backend services.

## Overview

The query system supports:
- **Pagination**: Page-based pagination with configurable page size
- **Sorting**: Multi-field sorting with ascending/descending order
- **Filtering**: V2 filter system with operations between each pair of conditions
- **Global Search**: Optional text search across fields

## Request Format

All list/query endpoints use **POST** method with the following request body:

```json
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
        "field": "name",
        "operator": "CONTAINS",
        "value": "john"
      }
    ]
  },
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ],
  "search": "optional global search term",
  "selected": ["uuid-1", "uuid-2"]
}
```

## Response Format

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

## V2 Filter System

The V2 filter system allows operations between each pair of conditions/groups.

### Filter Structure

```typescript
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
```

### Supported Operators

```typescript
type FilterOperator =
  | 'EQUALS' | 'NOT_EQUALS'
  | 'CONTAINS' | 'STARTS_WITH' | 'ENDS_WITH' | 'REGEX'
  | 'IN' | 'NOT_IN'
  | 'GREATER_THAN' | 'GREATER_THAN_OR_EQUAL' | 'LESS_THAN' | 'LESS_THAN_OR_EQUAL'
  | 'BETWEEN'
  | 'IS_NULL' | 'IS_NOT_NULL'
  | 'IS_EMPTY' | 'IS_NOT_EMPTY'
  | 'ARRAY_CONTAINS' | 'ARRAY_CONTAINS_ANY' | 'ARRAY_CONTAINS_ALL'
```

### Filter Examples

#### Basic Filter
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
      }
    ]
  }
}
```

#### Multiple Conditions with Operators
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

#### Nested Groups
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

## Sorting

Sorting is specified as an array of sort configurations:

```json
{
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    },
    {
      "field": "name",
      "direction": "asc"
    }
  ]
}
```

## Pagination

Pagination uses 0-based page indexing:

- `page`: Page number (0-based, default: 0)
- `size`: Items per page (default: 10)
- `totalElements`: Total number of items across all pages
- `totalPages`: Total number of pages

## Global Search

Optional global search term that searches across multiple fields:

```json
{
  "search": "john"
}
```

The exact fields searched depend on the service implementation.

## Selected Items

Optional list of selected item IDs:

```json
{
  "selected": ["uuid-1", "uuid-2", "uuid-3"]
}
```

This is useful for bulk operations or highlighting selected items.

## Implementation Notes

### Backend Services

All backend services should implement:
- `POST /api/v2/{resource}` endpoint accepting `PagingRequestV2`
- Use `EnhancedQueryParserV2.parseFilterGroup()` to parse V2 filters
- Return `PagedData<T>` wrapped in `BaseResponse`

### Frontend

The frontend uses V2 filter format when calling V2 API endpoints. The UI components may use V1 filter format internally, but convert to V2 format when making API calls.

## Related Documentation

- Filter System Guide: `/BE/FILTER_SYSTEM_V0_V1_V2_GUIDE.md`
- RESTful API Guide: `/RESTFUL.md`
