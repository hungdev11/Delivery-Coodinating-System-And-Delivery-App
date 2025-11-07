# Filter System V0, V1, V2 Implementation Guide

## Overview

This guide explains the three filter/paging API versions available in the Delivery Coordination System backend:

- **V0**: Simple paging and sorting (no dynamic filters)
- **V1**: Dynamic filtering with group-level logic operators (current implementation)
- **V2**: Enhanced dynamic filtering with operations between each pair (NEW - solves the problem)

## Problem with V1

The V1 filter system has a limitation: **one group can only have one operation applied to ALL conditions**. This means you cannot have different operators between different pairs of conditions.

### V1 Example (Problematic)
```json
{
  "logic": "AND",  // One operation for the ENTIRE group
  "conditions": [
    { "field": "status", "operator": "eq", "value": "ACTIVE" },
    { "field": "age", "operator": "gte", "value": 18" },
    { "field": "role", "operator": "eq", "value": "ADMIN" }
  ]
}
```

In this example, ALL conditions are combined with AND. You cannot do: `status=ACTIVE AND age>=18 OR role=ADMIN`

## Solution: V2 Filter System

V2 allows **operations between each pair** of conditions/groups by having a flat list where items alternate between conditions and operators.

### V2 Example (Solution)
```json
{
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
```

This represents: `status=ACTIVE AND age>=18 OR role=ADMIN`

### V2 with Nested Groups
```json
{
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
```

This represents: `status=ACTIVE AND (role=ADMIN OR role=MANAGER)`

## API Endpoints

### V0 API - Simple Paging (No Dynamic Filters)

**Endpoint**: `POST /api/v0/{resource}`

**Use Case**: When you need basic paging and sorting without complex filtering. Filters must be implemented by the caller.

**Request Body**:
```json
{
  "page": 0,
  "size": 10,
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ]
}
```

**Example**:
```bash
curl -X POST http://localhost:8080/api/v0/users \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 10,
    "sorts": [{"field": "username", "direction": "asc"}]
  }'
```

### V1 API - Dynamic Filtering (Current)

**Endpoint**: `POST /api/v1/{resource}`

**Use Case**: Existing implementation with group-level logic operators.

**Request Body**:
```json
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
      }
    ]
  },
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ]
}
```

### V2 API - Enhanced Dynamic Filtering (NEW)

**Endpoint**: `POST /api/v2/{resource}`

**Use Case**: When you need operations between each pair of conditions.

**Request Body**:
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
        "field": "age",
        "operator": "GREATER_THAN_OR_EQUAL",
        "value": 18
      }
    ]
  },
  "sorts": [
    {
      "field": "createdAt",
      "direction": "desc"
    }
  ]
}
```

## Implementation Guide for Backend Services

### 1. Java/Spring Boot Services (User_service, Parcel-service)

#### Step 1: Copy V2 Filter Classes

Copy the following classes from `User_service` to your service:

```
src/main/java/com/ds/{service}/common/entities/common/filter/v2/
├── FilterItemType.java
├── FilterItemV2.java
├── FilterConditionItemV2.java
├── FilterOperatorItemV2.java
└── FilterGroupItemV2.java

src/main/java/com/ds/{service}/common/entities/common/
├── PagingRequestV0.java
└── PagingRequestV2.java

src/main/java/com/ds/{service}/common/utils/
└── EnhancedQueryParserV2.java
```

#### Step 2: Update Service Interface

Add methods to your service interface:

```java
public interface IYourService {
    // V1 (existing)
    PagedData<YourEntity> getItems(PagingRequest query);
    
    // V0 (new)
    PagedData<YourEntity> getItemsV0(PagingRequestV0 query);
    
    // V2 (new)
    PagedData<YourEntity> getItemsV2(PagingRequestV2 query);
}
```

#### Step 3: Implement Service Methods

```java
@Override
public PagedData<YourEntity> getItemsV0(PagingRequestV0 query) {
    // V0: Simple paging with sorting only
    Sort sort = query.getSortsOrEmpty().isEmpty() 
        ? Sort.by(Sort.Direction.DESC, "id")
        : EnhancedQueryParser.parseSortConfigs(query.getSortsOrEmpty(), YourEntity.class);
    
    Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);
    Page<YourEntity> page = repository.findAll(pageable);
    
    return PagedData.<YourEntity>builder()
            .data(page.getContent())
            .page(new Paging<>(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    null,
                    query.getSortsOrEmpty(),
                    query.getSelectedOrEmpty()
            ))
            .build();
}

@Override
public PagedData<YourEntity> getItemsV2(PagingRequestV2 query) {
    // V2: Enhanced filtering with operations between each pair
    Specification<YourEntity> spec = Specification.where(null);
    if (query.getFiltersOrNull() != null) {
        spec = EnhancedQueryParserV2.parseFilterGroup(
            query.getFiltersOrNull(), 
            YourEntity.class
        );
    }

    Sort sort = query.getSortsOrEmpty().isEmpty()
        ? Sort.by(Sort.Direction.DESC, "id")
        : EnhancedQueryParser.parseSortConfigs(query.getSortsOrEmpty(), YourEntity.class);

    Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);
    Page<YourEntity> page = repository.findAll(spec, pageable);

    return PagedData.<YourEntity>builder()
            .data(page.getContent())
            .page(new Paging<>(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    null,
                    query.getSortsOrEmpty(),
                    query.getSelectedOrEmpty()
            ))
            .build();
}
```

#### Step 4: Create Controllers

Create `v0` and `v2` controller packages and add controllers:

```java
// v0/YourControllerV0.java
@RestController
@RequestMapping("/api/v0/your-resource")
public class YourControllerV0 {
    @Autowired
    private IYourService service;

    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<YourDto>>> getItems(
            @Valid @RequestBody PagingRequestV0 query) {
        PagedData<YourEntity> page = service.getItemsV0(query);
        // Convert to DTO and return
        return ResponseEntity.ok(BaseResponse.success(convertToDto(page)));
    }
}

// v2/YourControllerV2.java
@RestController
@RequestMapping("/api/v2/your-resource")
public class YourControllerV2 {
    @Autowired
    private IYourService service;

    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<YourDto>>> getItems(
            @Valid @RequestBody PagingRequestV2 query) {
        PagedData<YourEntity> page = service.getItemsV2(query);
        // Convert to DTO and return
        return ResponseEntity.ok(BaseResponse.success(convertToDto(page)));
    }
}
```

### 2. Node.js/TypeScript Services (zone_service)

#### Step 1: Copy V2 Filter Types

Create TypeScript interfaces in `src/common/types/filter-v2.ts`:

```typescript
export enum FilterItemType {
  CONDITION = 'condition',
  OPERATOR = 'operator',
  GROUP = 'group'
}

export interface FilterItemV2 {
  type: FilterItemType;
}

export interface FilterConditionItemV2 extends FilterItemV2 {
  type: FilterItemType.CONDITION;
  field: string;
  operator: string;
  value: any;
  caseSensitive?: boolean;
  id?: string;
}

export interface FilterOperatorItemV2 extends FilterItemV2 {
  type: FilterItemType.OPERATOR;
  value: 'AND' | 'OR';
}

export interface FilterGroupItemV2 extends FilterItemV2 {
  type: FilterItemType.GROUP;
  items: FilterItemV2[];
}

export interface PagingRequestV0 {
  page?: number;
  size?: number;
  sorts?: SortConfig[];
  search?: string;
  selected?: string[];
}

export interface PagingRequestV2 {
  page?: number;
  size?: number;
  filters?: FilterGroupItemV2;
  sorts?: SortConfig[];
  search?: string;
  selected?: string[];
}
```

#### Step 2: Create Query Parser V2

Create `src/common/utils/query-parser-v2.ts`:

```typescript
import { Prisma } from '@prisma/client';
import { FilterGroupItemV2, FilterItemV2, FilterConditionItemV2, FilterOperatorItemV2 } from '../types/filter-v2';

export class QueryParserV2 {
  static parseFilterGroup(filterGroup: FilterGroupItemV2): any {
    if (!filterGroup || !filterGroup.items || filterGroup.items.length === 0) {
      return {};
    }

    const predicates: any[] = [];
    const operators: string[] = [];

    for (const item of filterGroup.items) {
      if (item.type === 'condition') {
        const condition = item as FilterConditionItemV2;
        predicates.push(this.parseCondition(condition));
      } else if (item.type === 'group') {
        const group = item as FilterGroupItemV2;
        predicates.push(this.parseFilterGroup(group));
      } else if (item.type === 'operator') {
        const operator = item as FilterOperatorItemV2;
        operators.push(operator.value);
      }
    }

    return this.combinePredicates(predicates, operators);
  }

  private static combinePredicates(predicates: any[], operators: string[]): any {
    if (predicates.length === 0) return {};
    if (predicates.length === 1) return predicates[0];

    let result = predicates[0];
    for (let i = 1; i < predicates.length; i++) {
      const operator = operators[i - 1] || 'AND';
      const nextPredicate = predicates[i];

      if (operator === 'OR') {
        result = { OR: [result, nextPredicate] };
      } else {
        result = { AND: [result, nextPredicate] };
      }
    }

    return result;
  }

  private static parseCondition(condition: FilterConditionItemV2): any {
    // Similar to existing query parser logic
    // ... implement condition parsing
  }
}
```

#### Step 3: Create Service Methods

```typescript
export class YourService {
  static async getItemsV0(request: PagingRequestV0): Promise<PagedData<any>> {
    const skip = (request.page || 0) * (request.size || 10);
    const take = request.size || 10;

    const data = await prisma.yourTable.findMany({
      skip,
      take,
      orderBy: request.sorts ? parseSortConfigs(request.sorts) : { id: 'desc' }
    });

    const total = await prisma.yourTable.count();

    return {
      data,
      page: {
        page: request.page || 0,
        size: request.size || 10,
        totalElements: total,
        totalPages: Math.ceil(total / (request.size || 10)),
        filters: null,
        sorts: request.sorts || []
      }
    };
  }

  static async getItemsV2(request: PagingRequestV2): Promise<PagedData<any>> {
    const where = request.filters 
      ? QueryParserV2.parseFilterGroup(request.filters)
      : {};

    const skip = (request.page || 0) * (request.size || 10);
    const take = request.size || 10;

    const [data, total] = await Promise.all([
      prisma.yourTable.findMany({
        where,
        skip,
        take,
        orderBy: request.sorts ? parseSortConfigs(request.sorts) : { id: 'desc' }
      }),
      prisma.yourTable.count({ where })
    ]);

    return {
      data,
      page: {
        page: request.page || 0,
        size: request.size || 10,
        totalElements: total,
        totalPages: Math.ceil(total / (request.size || 10)),
        filters: null,
        sorts: request.sorts || []
      }
    };
  }
}
```

#### Step 4: Create Routes

```typescript
// routes/v0/your-routes.ts
router.post('/', async (req: Request, res: Response) => {
  const request: PagingRequestV0 = req.body;
  const result = await YourService.getItemsV0(request);
  res.json({ result });
});

// routes/v2/your-routes.ts
router.post('/', async (req: Request, res: Response) => {
  const request: PagingRequestV2 = req.body;
  const result = await YourService.getItemsV2(request);
  res.json({ result });
});
```

## Testing

### V0 API Test
```bash
curl -X POST http://localhost:8080/api/v0/users \
  -H "Content-Type: application/json" \
  -d '{
    "page": 0,
    "size": 10,
    "sorts": [{"field": "username", "direction": "asc"}]
  }'
```

### V2 API Test
```bash
curl -X POST http://localhost:8080/api/v2/users \
  -H "Content-Type: application/json" \
  -d '{
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
        }
      ]
    },
    "sorts": [{"field": "createdAt", "direction": "desc"}]
  }'
```

## Summary

- **V0**: Use for simple paging and sorting without filters
- **V1**: Existing implementation (keep for backward compatibility)
- **V2**: Use when you need complex filtering with different operators between pairs

All three versions can coexist in the same service, allowing clients to choose the appropriate version for their needs.
