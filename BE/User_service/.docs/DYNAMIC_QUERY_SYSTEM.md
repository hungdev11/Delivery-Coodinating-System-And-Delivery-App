# 🔍 Dynamic Query System

## 📖 Overview

The Dynamic Query System provides advanced filtering, sorting, and pagination capabilities for any entity in the application. It supports:

- **Dynamic Field Discovery** - Automatically discovers filterable and sortable fields
- **Type-Safe Operators** - Each field type has appropriate operators
- **Nested Field Support** - Handle complex object relationships
- **Case Sensitivity** - Configurable string operations
- **Validation** - Field and operator validation before query execution
- **Generic Implementation** - Works with any entity type

## 🎯 Key Features

- ✅ **Auto Field Discovery** - No manual configuration needed
- ✅ **Type-Aware Operators** - Smart operator assignment based on field type
- ✅ **Nested Properties** - Support for "user.address.city" style fields
- ✅ **Case Sensitivity** - String operations can be case-sensitive/insensitive
- ✅ **Validation** - Pre-query validation of fields and operators
- ✅ **Generic Service** - Single implementation for all entities
- ✅ **REST API** - Metadata endpoints for frontend integration
- ✅ **Custom Pagination** - No dependency on Spring Page/Pageable

## 🏗️ Architecture

```
Dynamic Query System
├── Core Components
│   ├── FilterOperator.java          # 🔧 Operator definitions
│   ├── FilterCondition.java         # 📋 Single filter condition
│   ├── FilterGroup.java             # 📦 Group of conditions (AND/OR)
│   ├── SortConfig.java              # 📊 Sort configuration
│   └── PagingRequest.java           # 📄 Pagination request
├── Processing Layer
│   ├── EnhancedQueryParser.java     # 🔍 Query parsing engine
│   ├── FilterableFieldRegistry.java # 📝 Field registry
│   └── GenericQueryService.java     # 🔄 Generic query executor
├── Service Layer
│   └── QueryService.java            # 🔍 Query metadata service
└── API Layer
    └── QueryController.java         # 🌐 REST API endpoints
```

## 🚀 Quick Start

### **1. Basic Usage**

```java
// In your service class
@Service
public class ProductService {
    @Autowired
    private FilterableFieldRegistry fieldRegistry;
    
    public PagedData<Product> getProducts(PagingRequest query) {
        // Auto-discover fields for Product entity
        if (fieldRegistry.getFilterableFields(Product.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(Product.class);
        }
        
        // Set field registry for validation
        EnhancedQueryParser.setFieldRegistry(fieldRegistry);
        
        // Execute query using generic service
        return GenericQueryService.executeQuery(productRepository, query, Product.class);
    }
}
```

### **2. REST API Usage**

```bash
# Get query metadata for User entity
GET /api/v1/query/metadata/user

# Get filterable fields
GET /api/v1/query/filterable-fields/user

# Get sortable fields
GET /api/v1/query/sortable-fields/user

# Get supported operators for a field
GET /api/v1/query/supported-operators/user/username

# Validate sort configuration
POST /api/v1/query/validate-sort?entityName=user
Content-Type: application/json
[
  {"field": "username", "direction": "asc"},
  {"field": "email", "direction": "desc"}
]
```

## 📋 Supported Operators

### **String Fields**
- `EQUALS` - Exact match
- `NOT_EQUALS` - Not equal
- `CONTAINS` - Contains substring
- `STARTS_WITH` - Starts with
- `ENDS_WITH` - Ends with
- `REGEX` - Regular expression match
- `IS_NULL` - Is null
- `IS_NOT_NULL` - Is not null

### **Numeric Fields**
- `EQUALS` - Equal to
- `NOT_EQUALS` - Not equal to
- `GREATER_THAN` - Greater than
- `GREATER_THAN_OR_EQUAL` - Greater than or equal
- `LESS_THAN` - Less than
- `LESS_THAN_OR_EQUAL` - Less than or equal
- `BETWEEN` - Between two values
- `IN` - In list
- `NOT_IN` - Not in list
- `IS_NULL` - Is null
- `IS_NOT_NULL` - Is not null

### **Date Fields**
- `EQUALS` - Equal to
- `NOT_EQUALS` - Not equal to
- `GREATER_THAN` - After date
- `GREATER_THAN_OR_EQUAL` - On or after date
- `LESS_THAN` - Before date
- `LESS_THAN_OR_EQUAL` - On or before date
- `BETWEEN` - Between two dates
- `IS_NULL` - Is null
- `IS_NOT_NULL` - Is not null

### **Boolean Fields**
- `EQUALS` - Equal to
- `NOT_EQUALS` - Not equal to
- `IS_NULL` - Is null
- `IS_NOT_NULL` - Is not null

### **Enum Fields**
- `EQUALS` - Equal to
- `NOT_EQUALS` - Not equal to
- `IN` - In list
- `NOT_IN` - Not in list
- `IS_NULL` - Is null
- `IS_NOT_NULL` - Is not null

## 🔧 Configuration

### **Field Registry Setup**

```java
@Configuration
public class QueryConfig {
    @Autowired
    private FilterableFieldRegistry fieldRegistry;
    
    @PostConstruct
    public void initializeFieldRegistry() {
        // Auto-discover fields for all entities
        fieldRegistry.autoDiscoverFields(User.class);
        fieldRegistry.autoDiscoverFields(Product.class);
        fieldRegistry.autoDiscoverFields(Order.class);
    }
}
```

### **Custom Field Configuration**

```java
// Register custom field configuration
Set<FilterableFieldRegistry.FilterableFieldInfo> customFields = new HashSet<>();
customFields.add(FilterableFieldRegistry.FilterableFieldInfo.builder()
    .fieldName("customField")
    .fieldType(String.class)
    .supportedOperators(Set.of(FilterOperator.EQUALS, FilterOperator.CONTAINS))
    .isSearchable(true)
    .build());

fieldRegistry.registerFields(CustomEntity.class, customFields);
```

## 📊 Example Queries

### **Simple Filter**

```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "username",
        "operator": "CONTAINS",
        "value": "john",
        "caseSensitive": false
      },
      {
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
  "page": 0,
  "size": 10
}
```

### **Complex Filter with Nested Fields**

```json
{
  "filters": {
    "logic": "OR",
    "conditions": [
      {
        "field": "user.profile.firstName",
        "operator": "CONTAINS",
        "value": "john"
      },
      {
        "field": "user.profile.lastName",
        "operator": "CONTAINS",
        "value": "doe"
      }
    ]
  },
  "sorts": [
    {
      "field": "user.profile.firstName",
      "direction": "asc"
    }
  ]
}
```

### **Range Filter**

```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "price",
        "operator": "BETWEEN",
        "value": [100, 500]
      },
      {
        "field": "createdAt",
        "operator": "GREATER_THAN",
        "value": "2024-01-01"
      }
    ]
  }
}
```

## 🎯 Best Practices

1. **Always validate** - Use the validation endpoints before executing queries
2. **Use appropriate operators** - Match operators to field types
3. **Handle nested fields** - Use dot notation for complex relationships
4. **Consider performance** - Use pagination for large datasets
5. **Case sensitivity** - Set `caseSensitive: false` for user-friendly searches
6. **Error handling** - Implement proper error handling for invalid queries

## 🔍 Troubleshooting

### **Common Issues**

1. **Field not found** - Check field name spelling and entity structure
2. **Operator not supported** - Verify operator is appropriate for field type
3. **Nested field errors** - Ensure intermediate objects are not primitives
4. **Case sensitivity** - Check `caseSensitive` flag for string operations
5. **Validation errors** - Use validation endpoints to debug query structure

### **Debug Mode**

Enable debug logging to see query processing:

```yaml
logging:
  level:
    com.ds.user.common.utils.EnhancedQueryParser: DEBUG
    com.ds.user.common.helper.FilterableFieldRegistry: DEBUG
```
