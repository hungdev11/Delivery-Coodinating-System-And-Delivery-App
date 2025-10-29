# Advanced Filter/Sort System - Backend Integration

This document provides implementation guides for integrating the advanced filter/sort system with various backend technologies.

## Overview

The system uses MongoDB-style query syntax that can be easily converted to different backend query languages. The frontend sends structured `QueryPayload` objects that contain:

- `filters`: Nested FilterGroup with AND/OR logic
- `sorts`: Array of SortConfig objects
- `page`: Pagination page number
- `size`: Items per page

## MongoDB Query Format

### Filter Structure
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
            "field": "email",
            "operator": "contains",
            "value": "@gmail.com",
            "caseSensitive": false
          },
          {
            "field": "email",
            "operator": "contains",
            "value": "@yahoo.com",
            "caseSensitive": false
          }
        ]
      }
    ]
  },
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
  "page": 0,
  "size": 10
}
```

### Converted MongoDB Query
```javascript
{
  "$and": [
    { "status": { "$eq": "ACTIVE" } },
    {
      "$or": [
        { "email": { "$regex": "@gmail.com", "$options": "i" } },
        { "email": { "$regex": "@yahoo.com", "$options": "i" } }
      ]
    }
  ]
}
```

### Converted MongoDB Sort
```javascript
{
  "username": 1,
  "createdAt": -1
}
```

## Node.js Implementation

### 1. Prisma Integration

**File**: `BE/shared/utils/prisma-query-parser.ts`

```typescript
import { Prisma } from '@prisma/client'
import type { FilterGroup, SortConfig, QueryPayload } from './types'

export function parseMongoQueryToPrisma(group: FilterGroup): Prisma.UserWhereInput {
  if (!group.conditions || group.conditions.length === 0) {
    return {}
  }

  const conditions = group.conditions.map(condition => {
    if ('field' in condition) {
      return parseConditionToPrisma(condition)
    } else {
      return parseMongoQueryToPrisma(condition)
    }
  })

  if (conditions.length === 1) {
    return conditions[0]
  }

  const operator = group.logic === 'AND' ? 'AND' : 'OR'
  return { [operator]: conditions }
}

function parseConditionToPrisma(condition: FilterCondition): Prisma.UserWhereInput {
  const { field, operator, value, caseSensitive } = condition

  switch (operator) {
    case 'eq':
      return { [field]: { equals: value } }
    case 'ne':
      return { [field]: { not: { equals: value } } }
    case 'contains':
      return { [field]: { contains: value, mode: caseSensitive ? 'default' : 'insensitive' } }
    case 'startsWith':
      return { [field]: { startsWith: value, mode: caseSensitive ? 'default' : 'insensitive' } }
    case 'endsWith':
      return { [field]: { endsWith: value, mode: caseSensitive ? 'default' : 'insensitive' } }
    case 'in':
      return { [field]: { in: Array.isArray(value) ? value : [value] } }
    case 'notIn':
      return { [field]: { notIn: Array.isArray(value) ? value : [value] } }
    case 'gt':
      return { [field]: { gt: value } }
    case 'gte':
      return { [field]: { gte: value } }
    case 'lt':
      return { [field]: { lt: value } }
    case 'lte':
      return { [field]: { lte: value } }
    case 'between':
      return { [field]: { gte: value[0], lte: value[1] } }
    case 'isNull':
      return { [field]: { equals: null } }
    case 'isNotNull':
      return { [field]: { not: { equals: null } } }
    default:
      throw new Error(`Unsupported operator: ${operator}`)
  }
}

export function parseSortsToPrisma(sorts: SortConfig[]): Prisma.UserOrderByWithRelationInput[] {
  return sorts.map(sort => ({
    [sort.field]: sort.direction
  }))
}
```

### 2. TypeORM Integration

**File**: `BE/shared/utils/typeorm-query-parser.ts`

```typescript
import { FindConditions, FindManyOptions, Like, Between, In, IsNull, Not } from 'typeorm'
import type { FilterGroup, SortConfig } from './types'

export function parseMongoQueryToTypeORM(group: FilterGroup): FindConditions {
  if (!group.conditions || group.conditions.length === 0) {
    return {}
  }

  const conditions = group.conditions.map(condition => {
    if ('field' in condition) {
      return parseConditionToTypeORM(condition)
    } else {
      return parseMongoQueryToTypeORM(condition)
    }
  })

  if (conditions.length === 1) {
    return conditions[0]
  }

  // TypeORM doesn't have direct AND/OR support, use query builder
  throw new Error('Complex queries require QueryBuilder')
}

function parseConditionToTypeORM(condition: FilterCondition): FindConditions {
  const { field, operator, value, caseSensitive } = condition

  switch (operator) {
    case 'eq':
      return { [field]: value }
    case 'ne':
      return { [field]: Not(value) }
    case 'contains':
      return { [field]: Like(`%${value}%`) }
    case 'startsWith':
      return { [field]: Like(`${value}%`) }
    case 'endsWith':
      return { [field]: Like(`%${value}`) }
    case 'in':
      return { [field]: In(Array.isArray(value) ? value : [value]) }
    case 'notIn':
      return { [field]: Not(In(Array.isArray(value) ? value : [value])) }
    case 'gt':
      return { [field]: MoreThan(value) }
    case 'gte':
      return { [field]: MoreThanOrEqual(value) }
    case 'lt':
      return { [field]: LessThan(value) }
    case 'lte':
      return { [field]: LessThanOrEqual(value) }
    case 'between':
      return { [field]: Between(value[0], value[1]) }
    case 'isNull':
      return { [field]: IsNull() }
    case 'isNotNull':
      return { [field]: Not(IsNull()) }
    default:
      throw new Error(`Unsupported operator: ${operator}`)
  }
}
```

### 3. Express.js Route Handler

**File**: `BE/User_service/src/routes/users.ts`

```typescript
import { Request, Response } from 'express'
import { parseMongoQueryToPrisma, parseSortsToPrisma } from '../../shared/utils/prisma-query-parser'
import { prisma } from '../lib/prisma'
import type { QueryPayload } from '../../shared/types'

export async function searchUsers(req: Request, res: Response) {
  try {
    const query: QueryPayload = req.body
    
    // Parse filters to Prisma format
    const where = query.filters ? parseMongoQueryToPrisma(query.filters) : {}
    
    // Parse sorts to Prisma format
    const orderBy = query.sorts ? parseSortsToPrisma(query.sorts) : []
    
    // Calculate pagination
    const skip = (query.page || 0) * (query.size || 10)
    const take = query.size || 10
    
    // Execute query
    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where,
        orderBy,
        skip,
        take
      }),
      prisma.user.count({ where })
    ])
    
    // Return response
    res.json({
      result: {
        data: users,
        page: {
          page: query.page || 0,
          size: query.size || 10,
          totalElements: total,
          totalPages: Math.ceil(total / (query.size || 10)),
          filters: query.filters || null,
          sorts: query.sorts || []
        }
      }
    })
  } catch (error) {
    res.status(500).json({
      message: 'Failed to search users',
      error: error.message
    })
  }
}
```

## Spring Boot Implementation

### 1. JPA Specification Parser

**File**: `BE/User_service/src/main/java/utils/QueryParser.java`

```java
package com.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class QueryParser {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static <T> Specification<T> parseMongoQuery(JsonNode query) {
        if (query == null || query.isNull()) {
            return Specification.where(null);
        }
        
        return parseFilterGroup(query);
    }
    
    private static <T> Specification<T> parseFilterGroup(JsonNode group) {
        String logic = group.get("logic").asText();
        JsonNode conditions = group.get("conditions");
        
        if (conditions == null || !conditions.isArray()) {
            return Specification.where(null);
        }
        
        List<Specification<T>> specs = new ArrayList<>();
        
        for (JsonNode condition : conditions) {
            if (condition.has("field")) {
                // It's a FilterCondition
                specs.add(parseCondition(condition));
            } else {
                // It's a FilterGroup (recursive)
                specs.add(parseFilterGroup(condition));
            }
        }
        
        if (specs.isEmpty()) {
            return Specification.where(null);
        }
        
        if (specs.size() == 1) {
            return specs.get(0);
        }
        
        if ("AND".equals(logic)) {
            return specs.stream().reduce(Specification::and).orElse(null);
        } else {
            return specs.stream().reduce(Specification::or).orElse(null);
        }
    }
    
    private static <T> Specification<T> parseCondition(JsonNode condition) {
        String field = condition.get("field").asText();
        String operator = condition.get("operator").asText();
        JsonNode value = condition.get("value");
        boolean caseSensitive = condition.has("caseSensitive") && condition.get("caseSensitive").asBoolean();
        
        return (root, query, cb) -> {
            Path<Object> fieldPath = root.get(field);
            
            switch (operator) {
                case "eq":
                    return cb.equal(fieldPath, parseValue(value));
                case "ne":
                    return cb.notEqual(fieldPath, parseValue(value));
                case "contains":
                    if (caseSensitive) {
                        return cb.like(fieldPath.as(String.class), "%" + value.asText() + "%");
                    } else {
                        return cb.like(cb.lower(fieldPath.as(String.class)), 
                                     "%" + value.asText().toLowerCase() + "%");
                    }
                case "startsWith":
                    if (caseSensitive) {
                        return cb.like(fieldPath.as(String.class), value.asText() + "%");
                    } else {
                        return cb.like(cb.lower(fieldPath.as(String.class)), 
                                     value.asText().toLowerCase() + "%");
                    }
                case "endsWith":
                    if (caseSensitive) {
                        return cb.like(fieldPath.as(String.class), "%" + value.asText());
                    } else {
                        return cb.like(cb.lower(fieldPath.as(String.class)), 
                                     "%" + value.asText().toLowerCase());
                    }
                case "in":
                    return fieldPath.in(parseArrayValue(value));
                case "notIn":
                    return cb.not(fieldPath.in(parseArrayValue(value)));
                case "gt":
                    return cb.greaterThan(fieldPath.as(Comparable.class), parseValue(value));
                case "gte":
                    return cb.greaterThanOrEqualTo(fieldPath.as(Comparable.class), parseValue(value));
                case "lt":
                    return cb.lessThan(fieldPath.as(Comparable.class), parseValue(value));
                case "lte":
                    return cb.lessThanOrEqualTo(fieldPath.as(Comparable.class), parseValue(value));
                case "between":
                    JsonNode values = value;
                    if (values.isArray() && values.size() == 2) {
                        return cb.between(fieldPath.as(Comparable.class), 
                                        parseValue(values.get(0)), 
                                        parseValue(values.get(1)));
                    }
                    return cb.conjunction();
                case "isNull":
                    return cb.isNull(fieldPath);
                case "isNotNull":
                    return cb.isNotNull(fieldPath);
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        };
    }
    
    private static Object parseValue(JsonNode value) {
        if (value.isTextual()) {
            return value.asText();
        } else if (value.isNumber()) {
            return value.numberValue();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isNull()) {
            return null;
        }
        return value.asText();
    }
    
    private static List<Object> parseArrayValue(JsonNode value) {
        List<Object> result = new ArrayList<>();
        if (value.isArray()) {
            for (JsonNode item : value) {
                result.add(parseValue(item));
            }
        } else {
            result.add(parseValue(value));
        }
        return result;
    }
    
    public static Sort parseSorts(JsonNode sorts) {
        if (sorts == null || !sorts.isArray()) {
            return Sort.unsorted();
        }
        
        List<Sort.Order> orders = new ArrayList<>();
        for (JsonNode sort : sorts) {
            String field = sort.get("field").asText();
            String direction = sort.get("direction").asText();
            
            if ("asc".equals(direction)) {
                orders.add(Sort.Order.asc(field));
            } else {
                orders.add(Sort.Order.desc(field));
            }
        }
        
        return Sort.by(orders);
    }
    
    public static Pageable parsePagination(JsonNode query) {
        int page = query.has("page") ? query.get("page").asInt() : 0;
        int size = query.has("size") ? query.get("size").asInt() : 10;
        
        return PageRequest.of(page, size);
    }
}
```

### 2. Spring Boot Controller

**File**: `BE/User_service/src/main/java/controller/UserController.java`

```java
package com.example.controller;

import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.utils.QueryParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @PostMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestBody JsonNode query) {
        try {
            // Parse filters
            Specification<User> spec = QueryParser.parseMongoQuery(query.get("filters"));
            
            // Parse sorts
            Sort sort = QueryParser.parseSorts(query.get("sorts"));
            
            // Parse pagination
            Pageable pageable = QueryParser.parsePagination(query);
            
            // Execute query
            Page<User> result = userRepository.findAll(spec, pageable);
            
            // Build response
            return ResponseEntity.ok(Map.of(
                "result", Map.of(
                    "data", result.getContent(),
                    "page", Map.of(
                        "page", result.getNumber(),
                        "size", result.getSize(),
                        "totalElements", result.getTotalElements(),
                        "totalPages", result.getTotalPages(),
                        "filters", query.get("filters"),
                        "sorts", query.get("sorts")
                    )
                )
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid query: " + e.getMessage()));
        }
    }
}
```

## API Endpoint Changes

### Current Endpoint
```
GET /v1/users?page=0&size=10&search=text
```

### New Endpoint
```
POST /v1/users/search
Content-Type: application/json

{
  "filters": { ... },
  "sorts": [ ... ],
  "page": 0,
  "size": 10
}
```

## Migration Strategy

1. **Phase 1**: Add new endpoint alongside existing one
2. **Phase 2**: Update frontend to use new endpoint
3. **Phase 3**: Deprecate old endpoint
4. **Phase 4**: Remove old endpoint

## Testing

### Example Test Cases

```javascript
// Test 1: Simple filter
{
  "filters": {
    "logic": "AND",
    "conditions": [
      { "field": "status", "operator": "eq", "value": "ACTIVE" }
    ]
  }
}

// Test 2: Complex filter with nested groups
{
  "filters": {
    "logic": "AND",
    "conditions": [
      { "field": "status", "operator": "eq", "value": "ACTIVE" },
      {
        "logic": "OR",
        "conditions": [
          { "field": "email", "operator": "contains", "value": "@gmail.com" },
          { "field": "email", "operator": "contains", "value": "@yahoo.com" }
        ]
      }
    ]
  }
}

// Test 3: Date range filter
{
  "filters": {
    "logic": "AND",
    "conditions": [
      { "field": "createdAt", "operator": "between", "value": ["2024-01-01", "2024-12-31"] }
    ]
  }
}
```

## Performance Considerations

1. **Indexing**: Ensure database indexes on commonly filtered fields
2. **Query Optimization**: Use query builders for complex queries
3. **Pagination**: Always implement proper pagination
4. **Caching**: Consider caching for frequently used filter combinations

## Security Considerations

1. **Input Validation**: Validate all filter inputs
2. **SQL Injection**: Use parameterized queries
3. **Field Access**: Restrict which fields can be filtered
4. **Rate Limiting**: Implement rate limiting on search endpoints
