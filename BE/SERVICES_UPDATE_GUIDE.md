# Services Update Guide

This guide documents the updates made to support the new paging/filter/sort system across all microservices.

## Overview

All services have been updated to support:
- **Advanced Filtering**: MongoDB-style filter groups with AND/OR logic
- **Dynamic Sorting**: Multi-field sorting with priority support
- **Custom Pagination**: Replacing Spring's Page/Pageable with custom PagedData
- **API Standardization**: POST endpoints for list operations, consistent naming

## Updated Services

### 1. API Gateway (`BE/api-gateway/`)

**New Files:**
- `src/main/java/com/ds/gateway/common/entities/dto/common/PagingRequest.java`
- `src/main/java/com/ds/gateway/common/entities/dto/common/FilterGroup.java`
- `src/main/java/com/ds/gateway/common/entities/dto/common/FilterCondition.java`
- `src/main/java/com/ds/gateway/common/entities/dto/common/SortConfig.java`
- `src/main/java/com/ds/gateway/common/entities/dto/common/PagedData.java`
- `src/main/java/com/ds/gateway/business/services/UserServiceProxy.java`
- `src/main/java/com/ds/gateway/application/controllers/UserController.java`
- `src/main/java/com/ds/gateway/application/config/RestTemplateConfig.java`

**Key Features:**
- Proxies requests to User Service with new paging/filter/sort support
- Maintains API consistency across the gateway
- Handles error propagation and logging

### 2. Settings Service (`BE/Settings_service/`)

**New Files:**
- `src/main/java/com/ds/setting/common/entities/dto/common/PagingRequest.java`
- `src/main/java/com/ds/setting/common/entities/dto/common/FilterGroup.java`
- `src/main/java/com/ds/setting/common/entities/dto/common/FilterCondition.java`
- `src/main/java/com/ds/setting/common/entities/dto/common/SortConfig.java`
- `src/main/java/com/ds/setting/common/entities/dto/common/PagedData.java`

**Next Steps:**
- Implement controllers and services using these DTOs
- Add JPA Specification support for dynamic queries
- Create query parser similar to User Service

### 3. Zone Service (`BE/zone_service/`)

**New Files:**
- `src/common/types/filter.ts` - TypeScript types for filtering/sorting
- `src/common/utils/query-parser.ts` - Prisma query parser
- `src/common/services/zone-service.ts` - Zone service with advanced queries
- `src/modules/zone/zone.controller.ts` - Zone controller
- `src/modules/zone/zone.routes.ts` - Zone routes

**Key Features:**
- Prisma-based query building for dynamic filtering
- TypeScript type safety for all operations
- Support for global search across multiple fields
- Consistent API response format

## API Standards

### Endpoint Naming
- **List Operations**: `POST /api/v1/{resource}` (with PagingRequest body)
- **Get by ID**: `GET /api/v1/{resource}/{id}`
- **Create**: `POST /api/v1/{resource}/create`
- **Update**: `PUT /api/v1/{resource}/{id}`
- **Delete**: `DELETE /api/v1/{resource}/{id}`

### Request/Response Format

**List Request (POST):**
```json
{
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
      "field": "name",
      "direction": "asc"
    }
  ],
  "page": 0,
  "size": 10,
  "search": "global search term",
  "selected": ["id1", "id2"]
}
```

**List Response:**
```json
{
  "result": {
    "data": [...],
    "page": {
      "page": 0,
      "size": 10,
      "totalElements": 100,
      "totalPages": 10,
      "filters": {...},
      "sorts": [...],
      "selected": [...]
    }
  }
}
```

## Implementation Status

| Service | DTOs | Controllers | Services | Query Parser | Status |
|---------|------|-------------|----------|--------------|--------|
| User Service | ✅ | ✅ | ✅ | ✅ | Complete |
| API Gateway | ✅ | ✅ | ✅ | N/A | Complete |
| Settings Service | ✅ | ❌ | ❌ | ❌ | DTOs Only |
| Zone Service | ✅ | ✅ | ✅ | ✅ | Complete |

## Next Steps

### For Settings Service:
1. Create entity classes and repositories
2. Implement JPA Specification support
3. Create controllers and services
4. Add query parser for dynamic filtering

### For All Services:
1. Add comprehensive error handling
2. Implement validation for filter/sort parameters
3. Add logging and monitoring
4. Create API documentation
5. Add unit and integration tests

## Migration Notes

### From Legacy GET to POST:
- Old: `GET /api/v1/users?page=0&size=10&search=john`
- New: `POST /api/v1/users` with JSON body containing PagingRequest

### From Spring Page to PagedData:
- Old: `Page<User>` with Spring pagination
- New: `PagedData<User>` with custom pagination metadata

### Benefits:
- **Flexibility**: Complex nested filters in JSON
- **Consistency**: Same API pattern across all services
- **Type Safety**: Strong typing for all operations
- **Extensibility**: Easy to add new filter operators and sort options
