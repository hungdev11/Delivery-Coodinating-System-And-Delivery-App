# Remaining Implementation Tasks

## ✅ Completed

### User_service (Java/Spring Boot)
- ✅ V2 filter classes created
- ✅ V0 and V2 paging request classes created
- ✅ EnhancedQueryParserV2 implemented
- ✅ Service methods: getUsersV0(), getUsersV2()
- ✅ Controllers: UserControllerV0, UserControllerV2
- ✅ Route documentation updated
- ✅ Builds successfully

### Documentation
- ✅ All .docs/route files updated for all services
- ✅ Implementation guides created
- ✅ Quick reference guide created
- ✅ API examples documented

## 🔄 To Do - Code Implementation

### 1. zone_service (Node.js/TypeScript) - PRIORITY 1

**Location**: `/BE/zone_service`

**Files to Create**:
1. `src/common/types/filter-v2.ts` - V2 filter TypeScript interfaces
2. `src/common/types/paging-v0.ts` - V0 paging interface  
3. `src/common/types/paging-v2.ts` - V2 paging interface
4. `src/common/utils/query-parser-v2.ts` - V2 query parser
5. `src/modules/zone/zone.controller.v0.ts` - V0 controller
6. `src/modules/zone/zone.controller.v2.ts` - V2 controller
7. `src/modules/zone/zone.routes.v0.ts` - V0 routes
8. `src/modules/zone/zone.routes.v2.ts` - V2 routes

**Files to Modify**:
1. `src/modules/zone/zone.service.ts` - Add getZonesV0() and getZonesV2() methods
2. `src/modules/routes.ts` - Register v0 and v2 routes

**Copy From User_service** (adapt to TypeScript):
- Filter V2 structure
- PagingRequestV0/V2 structure
- Query parser logic

**Detailed Steps**:
```bash
# 1. Create V2 types
cp BE/User_service/src/main/java/com/ds/user/common/entities/common/filter/v2/* \
   BE/zone_service/src/common/types/filter-v2.ts
# (Convert Java to TypeScript)

# 2. Create parsers
# Implement QueryParserV2.parseFilterGroup() based on User_service logic

# 3. Create services
# Add to zone.service.ts:
#   - async getZonesV0(request: PagingRequestV0)
#   - async getZonesV2(request: PagingRequestV2)

# 4. Create controllers
# Create zone.controller.v0.ts and zone.controller.v2.ts

# 5. Create routes
# Create zone.routes.v0.ts: router.post('/', controller.getZones)
# Create zone.routes.v2.ts: router.post('/', controller.getZones)

# 6. Register routes in src/modules/routes.ts
```

**Same pattern applies to**:
- `src/modules/address/` (if it has listing endpoints)
- `src/modules/center/` (if it has listing endpoints)

### 2. parcel-service (Java/Spring Boot) - PRIORITY 2

**Location**: `/BE/parcel-service`

**Files to Copy from User_service**:
1. `src/main/java/com/ds/parcel_service/common/entities/common/filter/v2/`
   - FilterItemType.java
   - FilterItemV2.java
   - FilterConditionItemV2.java
   - FilterOperatorItemV2.java
   - FilterGroupItemV2.java

2. `src/main/java/com/ds/parcel_service/common/entities/common/`
   - PagingRequestV0.java
   - PagingRequestV2.java

3. `src/main/java/com/ds/parcel_service/common/utils/`
   - EnhancedQueryParserV2.java
   - Update EnhancedQueryParser.java (if exists) or create it

**Files to Create**:
1. `src/main/java/com/ds/parcel_service/application/controllers/v0/ParcelControllerV0.java`
2. `src/main/java/com/ds/parcel_service/application/controllers/v2/ParcelControllerV2.java`

**Files to Modify**:
1. `src/main/java/com/ds/parcel_service/common/interfaces/IParcelService.java`
   - Add: `PagedData<Parcel> getParcelsV0(PagingRequestV0 query);`
   - Add: `PagedData<Parcel> getParcelsV2(PagingRequestV2 query);`

2. `src/main/java/com/ds/parcel_service/business/v1/services/ParcelService.java`
   - Implement getParcelsV0() - simple paging
   - Implement getParcelsV2() - V2 filtering

**Detailed Steps**:
```bash
# 1. Copy filter classes
mkdir -p BE/parcel-service/src/main/java/com/ds/parcel_service/common/entities/common/filter/v2
cp BE/User_service/src/main/java/com/ds/user/common/entities/common/filter/v2/* \
   BE/parcel-service/src/main/java/com/ds/parcel_service/common/entities/common/filter/v2/
# Update package names: com.ds.user -> com.ds.parcel_service

# 2. Copy paging classes
cp BE/User_service/src/main/java/com/ds/user/common/entities/common/PagingRequest*.java \
   BE/parcel-service/src/main/java/com/ds/parcel_service/common/entities/common/
# Update package names

# 3. Copy parser
cp BE/User_service/src/main/java/com/ds/user/common/utils/EnhancedQueryParserV2.java \
   BE/parcel-service/src/main/java/com/ds/parcel_service/common/utils/
# Update package names and imports

# 4. Update interface and service (see User_service examples)
# 5. Create controllers (copy from User_service pattern)
# 6. Build and test
```

### 3. session-service (Node.js/TypeScript) - PRIORITY 3

**Location**: `/BE/session-service`

**Files to Create**:
Same pattern as zone_service:
1. `src/common/types/filter-v2.ts`
2. `src/common/types/paging-v0.ts`
3. `src/common/types/paging-v2.ts`
4. `src/common/utils/query-parser-v2.ts`
5. Controllers and routes for sessions and assignments modules

**Modules to Update**:
- Sessions module (if it has listing)
- Assignments module (if it has listing)

### 4. Settings_service (Node.js/TypeScript) - PRIORITY 4

**Location**: `/BE/Settings_service`

**Same pattern as zone_service** - create V2 types, parsers, controllers, and routes.

### 5. communication_service (Node.js/TypeScript) - PRIORITY 5

**Location**: `/BE/communication_service`

**Same pattern as zone_service** - create V2 types, parsers, controllers, and routes.

### 6. api-gateway - PRIORITY 6

**Location**: `/BE/api-gateway`

**Task**: Update routing configuration to proxy v0 and v2 endpoints

**Files to Modify**:
1. Gateway routing configuration
2. Add routes for:
   - `/api/v0/*` -> forward to respective services
   - `/api/v2/*` -> forward to respective services

**Example**:
```typescript
// Add to gateway config
{
  path: '/api/v0/users',
  target: 'http://user-service:8080/api/v0/users',
  methods: ['POST']
},
{
  path: '/api/v2/users',
  target: 'http://user-service:8080/api/v2/users',
  methods: ['POST']
}
// Repeat for zones, parcels, sessions, etc.
```

## TypeScript Interface Templates

### filter-v2.ts
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
```

### paging-v0.ts
```typescript
import { SortConfig } from './filter';

export interface PagingRequestV0 {
  page?: number;
  size?: number;
  sorts?: SortConfig[];
  search?: string;
  selected?: string[];
}
```

### paging-v2.ts
```typescript
import { FilterGroupItemV2 } from './filter-v2';
import { SortConfig } from './filter';

export interface PagingRequestV2 {
  page?: number;
  size?: number;
  filters?: FilterGroupItemV2;
  sorts?: SortConfig[];
  search?: string;
  selected?: string[];
}
```

### query-parser-v2.ts
```typescript
import { FilterGroupItemV2, FilterConditionItemV2, FilterOperatorItemV2 } from '../types/filter-v2';

export class QueryParserV2 {
  static parseFilterGroup(filterGroup: FilterGroupItemV2): any {
    if (!filterGroup?.items || filterGroup.items.length === 0) {
      return {};
    }

    const predicates: any[] = [];
    const operators: string[] = [];

    for (const item of filterGroup.items) {
      if (item.type === 'condition') {
        predicates.push(this.parseCondition(item as FilterConditionItemV2));
      } else if (item.type === 'group') {
        predicates.push(this.parseFilterGroup(item as FilterGroupItemV2));
      } else if (item.type === 'operator') {
        operators.push((item as FilterOperatorItemV2).value);
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
      if (operator === 'OR') {
        result = { OR: [result, predicates[i]] };
      } else {
        result = { AND: [result, predicates[i]] };
      }
    }
    return result;
  }

  private static parseCondition(condition: FilterConditionItemV2): any {
    // Implement based on QueryParser.parseFilterCondition from zone_service
    // Map operators to Prisma query syntax
  }
}
```

## Testing Checklist

After implementing each service:

- [ ] Service compiles without errors
- [ ] V0 endpoint works: `POST /api/v0/{resource}`
- [ ] V2 endpoint works: `POST /api/v2/{resource}`
- [ ] V1 endpoint still works (backward compatibility)
- [ ] Simple filter test (V2): single condition
- [ ] Complex filter test (V2): multiple conditions with different operators
- [ ] Nested group test (V2): groups within groups
- [ ] Sorting works in all versions
- [ ] Paging works in all versions

## Summary

**Total Services**: 6 (User_service + 5 remaining)
**Completed**: 1 (User_service)
**Remaining**: 5

**Estimated Effort per Service**:
- Node.js services: 2-3 hours each
- Java services: 1-2 hours each (copy & adapt pattern)
- API Gateway: 30 minutes

**Priority Order**:
1. zone_service (most commonly used, already has filters)
2. parcel-service (business critical)
3. session-service
4. Settings_service
5. communication_service
6. api-gateway (routing only)

All documentation is complete. Implementation pattern is established in User_service.
