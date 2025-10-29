# 📁 Dynamic Query System - File Locations & Use Cases

## 🗂️ File Structure

```
BE/User_service/src/main/java/com/ds/user/common/
├── entities/common/                    # 📦 Core Query DTOs
│   ├── PagingRequest.java             # 📄 Main query request DTO
│   ├── BaseResponse.java              # 📤 Standard API response
│   ├── filter/                        # 🔍 Filter System
│   │   ├── FilterOperator.java        # 🔧 Operator definitions
│   │   ├── FilterCondition.java       # 📋 Single filter condition
│   │   └── FilterGroup.java           # 📦 Group of conditions (AND/OR)
│   ├── sort/                         # 📊 Sort System
│   │   └── SortConfig.java            # 📊 Sort configuration
│   └── paging/                       # 📄 Pagination System
│       ├── Paging.java                # 📄 Pagination metadata
│       └── PagedData.java             # 📄 Paginated response wrapper
├── helper/                           # 🛠️ Query Helper Classes
│   ├── FilterableFieldRegistry.java  # 📝 Field registry & discovery
│   ├── GenericQueryService.java      # 🔄 Generic query executor
│   └── QueryService.java             # 🔍 Query metadata service
├── utils/                            # 🛠️ Query Utilities
│   └── EnhancedQueryParser.java      # 🔍 Query parsing engine
└── interfaces/                       # 🔌 Service Contracts
    └── IUserService.java             # 👤 User service interface
```

## 📋 File Descriptions & Use Cases

### **Core Query DTOs**

#### **`PagingRequest.java`**
- **Location**: `common/entities/common/PagingRequest.java`
- **Purpose**: Main request DTO for all query operations
- **Use Case**: 
  - REST API request body for POST endpoints
  - Contains filters, sorts, pagination, search, and selected items
  - Used by all entity services for consistent querying

```java
// Example usage
PagingRequest request = PagingRequest.builder()
    .filters(filterGroup)
    .sorts(sortConfigs)
    .page(0)
    .size(10)
    .search("john")
    .build();
```

#### **`FilterOperator.java`**
- **Location**: `common/entities/common/filter/FilterOperator.java`
- **Purpose**: Enum defining all supported filter operators
- **Use Case**:
  - Type-safe operator definitions
  - Validation of operator-field compatibility
  - Frontend dropdown options

```java
// Example usage
FilterOperator operator = FilterOperator.CONTAINS;
boolean isStringOperator = operator.isStringOperator();
```

#### **`FilterCondition.java`**
- **Location**: `common/entities/common/filter/FilterCondition.java`
- **Purpose**: Single filter condition with field, operator, value
- **Use Case**:
  - Individual filter conditions
  - Building complex filter groups
  - API request/response serialization

```java
// Example usage
FilterCondition condition = FilterCondition.builder()
    .field("username")
    .operator(FilterOperator.CONTAINS)
    .value("john")
    .caseSensitive(false)
    .build();
```

#### **`FilterGroup.java`**
- **Location**: `common/entities/common/filter/FilterGroup.java`
- **Purpose**: Group of filter conditions with AND/OR logic
- **Use Case**:
  - Complex filter combinations
  - Nested filter groups
  - MongoDB-style query structure

```java
// Example usage
FilterGroup group = FilterGroup.builder()
    .logic("AND")
    .conditions(Arrays.asList(condition1, condition2))
    .build();
```

#### **`SortConfig.java`**
- **Location**: `common/entities/common/sort/SortConfig.java`
- **Purpose**: Sort configuration with field and direction
- **Use Case**:
  - Sort specifications
  - Multi-column sorting
  - API request/response serialization

```java
// Example usage
SortConfig sort = SortConfig.builder()
    .field("username")
    .direction("asc")
    .build();
```

#### **`Paging.java` & `PagedData.java`**
- **Location**: `common/entities/common/paging/`
- **Purpose**: Pagination metadata and response wrapper
- **Use Case**:
  - Paginated response structure
  - Total count and page information
  - Consistent pagination across all entities

```java
// Example usage
PagedData<User> result = PagedData.<User>builder()
    .data(users)
    .page(paging)
    .build();
```

### **Helper Classes**

#### **`FilterableFieldRegistry.java`**
- **Location**: `common/helper/FilterableFieldRegistry.java`
- **Purpose**: Registry for managing filterable fields per entity
- **Use Case**:
  - Auto-discovery of entity fields
  - Operator validation per field type
  - Field metadata for frontend

```java
// Example usage
fieldRegistry.autoDiscoverFields(User.class);
Set<FilterableFieldInfo> fields = fieldRegistry.getFilterableFields(User.class);
boolean isValid = fieldRegistry.validateFilterCondition(User.class, "username", FilterOperator.CONTAINS);
```

#### **`GenericQueryService.java`**
- **Location**: `common/helper/GenericQueryService.java`
- **Purpose**: Generic service for executing queries on any entity
- **Use Case**:
  - Single implementation for all entities
  - Reflection-based sorting
  - Custom pagination logic

```java
// Example usage
PagedData<User> result = GenericQueryService.executeQuery(
    userRepository, 
    pagingRequest, 
    User.class
);
```

#### **`QueryService.java`**
- **Location**: `common/helper/QueryService.java`
- **Purpose**: Service for providing query metadata and validation
- **Use Case**:
  - Field discovery for frontend
  - Sort configuration validation
  - Operator support queries

```java
// Example usage
QueryMetadata metadata = queryService.getQueryMetadata(User.class);
List<String> sortableFields = queryService.getSortableFields(User.class);
Set<FilterOperator> operators = queryService.getSupportedOperators(User.class, "username");
```

### **Utility Classes**

#### **`EnhancedQueryParser.java`**
- **Location**: `common/utils/EnhancedQueryParser.java`
- **Purpose**: Core query parsing engine
- **Use Case**:
  - Convert FilterGroup to JPA Specification
  - Parse SortConfig to JPA Sort
  - Field validation and nested property support

```java
// Example usage
Specification<User> spec = EnhancedQueryParser.parseFilterGroup(filterGroup, User.class);
Sort sort = EnhancedQueryParser.parseSortConfigs(sortConfigs, User.class);
```

## 🎯 Use Case Scenarios

### **1. New Entity Integration**

When adding a new entity (e.g., `Product`):

1. **Create Entity**: `Product.java` in `common/entities/base/`
2. **Create Repository**: `ProductRepository.java` in `app_context/repositories/`
3. **Create Service**: `ProductService.java` in `business/v1/services/`
4. **Auto-discover fields**: `fieldRegistry.autoDiscoverFields(Product.class)`
5. **Use GenericQueryService**: `GenericQueryService.executeQuery(productRepository, query, Product.class)`

### **2. Frontend Integration**

For frontend developers:

1. **Get metadata**: `GET /api/v1/query/metadata/{entityName}`
2. **Get filterable fields**: `GET /api/v1/query/filterable-fields/{entityName}`
3. **Get sortable fields**: `GET /api/v1/query/sortable-fields/{entityName}`
4. **Validate queries**: `POST /api/v1/query/validate-sort`
5. **Execute queries**: `POST /api/v1/{entityName}` with `PagingRequest`

### **3. Custom Field Configuration**

For special field requirements:

1. **Create custom field info**: `FilterableFieldRegistry.FilterableFieldInfo`
2. **Register custom fields**: `fieldRegistry.registerFields(Entity.class, customFields)`
3. **Use in queries**: Standard query execution with custom validation

### **4. Complex Query Building**

For advanced query scenarios:

1. **Build FilterGroup**: Combine multiple conditions with AND/OR logic
2. **Add nested fields**: Use dot notation for complex relationships
3. **Configure sorting**: Multi-column sorting with priority
4. **Set pagination**: Page size and offset configuration
5. **Execute query**: Use GenericQueryService for consistent execution

## 🔧 File Dependencies

```
PagingRequest
├── FilterGroup
│   ├── FilterCondition
│   │   └── FilterOperator
│   └── FilterCondition (recursive)
├── SortConfig[]
└── PagingRequest

EnhancedQueryParser
├── FilterGroup
├── FilterCondition
├── FilterOperator
├── SortConfig
└── FilterableFieldRegistry

GenericQueryService
├── PagingRequest
├── EnhancedQueryParser
└── FilterableFieldRegistry

QueryService
├── FilterableFieldRegistry
└── GenericQueryService
```

## 📊 File Size & Complexity

| File | Lines | Complexity | Purpose |
|------|-------|------------|---------|
| `FilterOperator.java` | ~118 | Low | Enum definitions |
| `FilterCondition.java` | ~42 | Low | Simple DTO |
| `FilterGroup.java` | ~29 | Low | Simple DTO |
| `SortConfig.java` | ~27 | Low | Simple DTO |
| `PagingRequest.java` | ~100 | Medium | Main request DTO |
| `EnhancedQueryParser.java` | ~386 | High | Core parsing logic |
| `FilterableFieldRegistry.java` | ~315 | High | Field management |
| `GenericQueryService.java` | ~288 | High | Generic execution |
| `QueryService.java` | ~153 | Medium | Metadata service |

## 🚀 Quick Reference

### **Essential Files for Basic Usage:**
1. `PagingRequest.java` - Main request DTO
2. `FilterOperator.java` - Operator definitions
3. `FilterCondition.java` - Single condition
4. `FilterGroup.java` - Condition groups
5. `SortConfig.java` - Sort configuration
6. `EnhancedQueryParser.java` - Query parsing
7. `GenericQueryService.java` - Query execution

### **Advanced Files for Customization:**
1. `FilterableFieldRegistry.java` - Field management
2. `QueryService.java` - Metadata service
3. `Paging.java` & `PagedData.java` - Pagination
4. `BaseResponse.java` - API responses
