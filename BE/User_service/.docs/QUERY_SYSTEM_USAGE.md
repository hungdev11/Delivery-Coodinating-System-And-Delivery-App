# 🚀 Dynamic Query System - Usage Guide

## 📖 Table of Contents

1. [Quick Start](#-quick-start)
2. [Basic Filtering](#-basic-filtering)
3. [Advanced Filtering](#-advanced-filtering)
4. [Sorting](#-sorting)
5. [Pagination](#-pagination)
6. [Nested Fields](#-nested-fields)
7. [API Integration](#-api-integration)
8. [Error Handling](#-error-handling)
9. [Performance Tips](#-performance-tips)
10. [Troubleshooting](#-troubleshooting)

## 🚀 Quick Start

### **1. Add to Your Service**

```java
@Service
public class ProductService {
    @Autowired
    private FilterableFieldRegistry fieldRegistry;
    
    public PagedData<Product> getProducts(PagingRequest query) {
        // Initialize field registry for Product entity
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

### **2. Add to Your Controller**

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    
    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<ProductDto>>> getProducts(
            @Valid @RequestBody PagingRequest query) {
        
        PagedData<Product> result = productService.getProducts(query);
        
        // Convert to DTO
        List<ProductDto> productDtos = result.getData().stream()
                .map(ProductDto::from)
                .toList();
        
        PagedData<ProductDto> pagedData = PagedData.<ProductDto>builder()
                .data(productDtos)
                .page(result.getPage())
                .build();
        
        return ResponseEntity.ok(BaseResponse.success(pagedData));
    }
}
```

## 🔍 Basic Filtering

### **Simple String Filter**

```java
// Filter: username contains "john" (case-insensitive)
FilterCondition condition = FilterCondition.builder()
    .field("username")
    .operator(FilterOperator.CONTAINS)
    .value("john")
    .caseSensitive(false)
    .build();

FilterGroup filterGroup = FilterGroup.builder()
    .logic("AND")
    .conditions(List.of(condition))
    .build();

PagingRequest request = PagingRequest.builder()
    .filters(filterGroup)
    .page(0)
    .size(10)
    .build();
```

### **Numeric Range Filter**

```java
// Filter: price between 100 and 500
FilterCondition priceCondition = FilterCondition.builder()
    .field("price")
    .operator(FilterOperator.BETWEEN)
    .value(List.of(100, 500))
    .build();

FilterGroup filterGroup = FilterGroup.builder()
    .logic("AND")
    .conditions(List.of(priceCondition))
    .build();
```

### **Date Range Filter (JSON Example)**

```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "createdAt",
        "operator": "BETWEEN",
        "value": ["2024-01-01", "2024-12-31"]
      },
      {
        "field": "updatedAt",
        "operator": "GREATER_THAN_OR_EQUAL",
        "value": "2024-06-01"
      }
    ]
  }
}
```

### **Complex Date Filter (JSON Example)**

```json
{
  "filters": {
    "logic": "OR",
    "conditions": [
      {
        "field": "createdAt",
        "operator": "BETWEEN",
        "value": ["2024-01-01", "2024-03-31"]
      },
      {
        "field": "createdAt",
        "operator": "BETWEEN",
        "value": ["2024-07-01", "2024-09-30"]
      }
    ]
  }
}
```

### **Multiple Conditions**

```java
// Filter: status = ACTIVE AND (username contains "john" OR email contains "john")
FilterCondition statusCondition = FilterCondition.builder()
    .field("status")
    .operator(FilterOperator.EQUALS)
    .value("ACTIVE")
    .build();

FilterCondition usernameCondition = FilterCondition.builder()
    .field("username")
    .operator(FilterOperator.CONTAINS)
    .value("john")
    .caseSensitive(false)
    .build();

FilterCondition emailCondition = FilterCondition.builder()
    .field("email")
    .operator(FilterOperator.CONTAINS)
    .value("john")
    .caseSensitive(false)
    .build();

// Nested group for OR logic
FilterGroup orGroup = FilterGroup.builder()
    .logic("OR")
    .conditions(List.of(usernameCondition, emailCondition))
    .build();

// Main group for AND logic
FilterGroup mainGroup = FilterGroup.builder()
    .logic("AND")
    .conditions(List.of(statusCondition, orGroup))
    .build();
```

## 🔍 Advanced Filtering

### **Date Range Filter**

```java
// Filter: created between 2024-01-01 and 2024-12-31
FilterCondition dateCondition = FilterCondition.builder()
    .field("createdAt")
    .operator(FilterOperator.BETWEEN)
    .value(List.of("2024-01-01", "2024-12-31"))
    .build();
```

### **Date Comparison Filters**

```java
// Filter: created after 2024-01-01
FilterCondition afterDateCondition = FilterCondition.builder()
    .field("createdAt")
    .operator(FilterOperator.GREATER_THAN)
    .value("2024-01-01")
    .build();

// Filter: created before 2024-12-31
FilterCondition beforeDateCondition = FilterCondition.builder()
    .field("createdAt")
    .operator(FilterOperator.LESS_THAN)
    .value("2024-12-31")
    .build();

// Filter: created on or after 2024-06-01
FilterCondition onOrAfterCondition = FilterCondition.builder()
    .field("createdAt")
    .operator(FilterOperator.GREATER_THAN_OR_EQUAL)
    .value("2024-06-01")
    .build();
```

### **Supported Date Formats**

The system supports multiple date formats:

```java
// ISO Date (yyyy-MM-dd)
.value("2024-01-15")

// ISO DateTime (yyyy-MM-dd HH:mm:ss)
.value("2024-01-15 10:30:00")

// ISO DateTime with T (yyyy-MM-dd'T'HH:mm:ss)
.value("2024-01-15T10:30:00")

// European format (dd/MM/yyyy)
.value("15/01/2024")

// US format (MM/dd/yyyy)
.value("01/15/2024")

// European with dashes (dd-MM-yyyy)
.value("15-01-2024")

// Alternative format (yyyy/MM/dd)
.value("2024/01/15")

// Timestamp (number)
.value(1705276800000L)  // Milliseconds since epoch
```

### **Enum Filter**

```java
// Filter: status in [ACTIVE, PENDING]
FilterCondition statusCondition = FilterCondition.builder()
    .field("status")
    .operator(FilterOperator.IN)
    .value(List.of("ACTIVE", "PENDING"))
    .build();
```

### **Null Check Filter**

```java
// Filter: description is not null
FilterCondition nullCondition = FilterCondition.builder()
    .field("description")
    .operator(FilterOperator.IS_NOT_NULL)
    .build();
```

### **Regex Filter**

```java
// Filter: email matches regex pattern
FilterCondition regexCondition = FilterCondition.builder()
    .field("email")
    .operator(FilterOperator.REGEX)
    .value("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    .caseSensitive(false)
    .build();
```

## 📊 Sorting

### **Single Column Sort**

```java
// Sort: by username ascending
SortConfig sortConfig = SortConfig.builder()
    .field("username")
    .direction("asc")
    .build();

PagingRequest request = PagingRequest.builder()
    .sorts(List.of(sortConfig))
    .page(0)
    .size(10)
    .build();
```

### **Multi-Column Sort**

```java
// Sort: by status ascending, then by createdAt descending
SortConfig statusSort = SortConfig.builder()
    .field("status")
    .direction("asc")
    .build();

SortConfig dateSort = SortConfig.builder()
    .field("createdAt")
    .direction("desc")
    .build();

PagingRequest request = PagingRequest.builder()
    .sorts(List.of(statusSort, dateSort))
    .page(0)
    .size(10)
    .build();
```

## 📄 Pagination

### **Basic Pagination**

```java
// Page 2, 20 items per page
PagingRequest request = PagingRequest.builder()
    .page(1)  // 0-based index
    .size(20)
    .build();
```

### **With Search**

```java
// Global search across all searchable fields
PagingRequest request = PagingRequest.builder()
    .page(0)
    .size(10)
    .search("john")  // Searches in username, email, firstName, lastName
    .build();
```

### **With Selected Items**

```java
// Include specific item IDs in results
PagingRequest request = PagingRequest.builder()
    .page(0)
    .size(10)
    .selected(List.of("user1", "user2", "user3"))
    .build();
```

## 🔗 Nested Fields

### **Simple Nested Field**

```java
// Filter: user.profile.firstName contains "john"
FilterCondition nestedCondition = FilterCondition.builder()
    .field("user.profile.firstName")
    .operator(FilterOperator.CONTAINS)
    .value("john")
    .caseSensitive(false)
    .build();
```

### **Complex Nested Filter**

```java
// Filter: user.address.city = "New York" AND user.address.country = "USA"
FilterCondition cityCondition = FilterCondition.builder()
    .field("user.address.city")
    .operator(FilterOperator.EQUALS)
    .value("New York")
    .build();

FilterCondition countryCondition = FilterCondition.builder()
    .field("user.address.country")
    .operator(FilterOperator.EQUALS)
    .value("USA")
    .build();

FilterGroup addressGroup = FilterGroup.builder()
    .logic("AND")
    .conditions(List.of(cityCondition, countryCondition))
    .build();
```

## 🌐 API Integration

### **REST API Endpoints**

```bash
# Get all products with filtering and sorting
POST /api/v1/products
Content-Type: application/json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "name",
        "operator": "CONTAINS",
        "value": "laptop",
        "caseSensitive": false
      },
      {
        "field": "price",
        "operator": "BETWEEN",
        "value": [500, 2000]
      }
    ]
  },
  "sorts": [
    {
      "field": "price",
      "direction": "asc"
    }
  ],
  "page": 0,
  "size": 20
}
```

### **Metadata Endpoints**

```bash
# Get query metadata for Product entity
GET /api/v1/query/metadata/product

# Get filterable fields
GET /api/v1/query/filterable-fields/product

# Get sortable fields
GET /api/v1/query/sortable-fields/product

# Get supported operators for a field
GET /api/v1/query/supported-operators/product/name

# Validate sort configuration
POST /api/v1/query/validate-sort?entityName=product
Content-Type: application/json
[
  {"field": "name", "direction": "asc"},
  {"field": "price", "direction": "desc"}
]
```

## ⚠️ Error Handling

### **Validation Errors**

```java
try {
    PagedData<Product> result = productService.getProducts(query);
    return ResponseEntity.ok(BaseResponse.success(result));
} catch (IllegalArgumentException e) {
    return ResponseEntity.badRequest()
            .body(BaseResponse.error("Invalid query: " + e.getMessage()));
} catch (Exception e) {
    log.error("Error executing query", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(BaseResponse.error("Query execution failed"));
}
```

### **Field Validation**

```java
// Check if field is filterable
boolean isFilterable = fieldRegistry.isFieldFilterable(Product.class, "name");

// Check if operator is supported
Set<FilterOperator> operators = fieldRegistry.getSupportedOperators(Product.class, "name");
boolean isSupported = operators.contains(FilterOperator.CONTAINS);

// Validate sort field
List<String> sortableFields = queryService.getSortableFields(Product.class);
boolean canSort = sortableFields.contains("name");
```

## 🚀 Performance Tips

### **1. Use Appropriate Page Sizes**

```java
// Good: Reasonable page size
PagingRequest request = PagingRequest.builder()
    .page(0)
    .size(50)  // Not too large, not too small
    .build();

// Bad: Too large page size
PagingRequest request = PagingRequest.builder()
    .page(0)
    .size(10000)  // Too large, slow query
    .build();
```

### **2. Use Indexed Fields for Sorting**

```java
// Good: Sort by indexed field
SortConfig sort = SortConfig.builder()
    .field("id")  // Usually indexed
    .direction("asc")
    .build();

// Bad: Sort by non-indexed field
SortConfig sort = SortConfig.builder()
    .field("description")  // May not be indexed
    .direction("asc")
    .build();
```

### **3. Use Specific Filters**

```java
// Good: Specific filter
FilterCondition condition = FilterCondition.builder()
    .field("status")
    .operator(FilterOperator.EQUALS)
    .value("ACTIVE")
    .build();

// Bad: Too broad filter
FilterCondition condition = FilterCondition.builder()
    .field("description")
    .operator(FilterOperator.CONTAINS)
    .value("a")  // Too broad, matches many records
    .build();
```

### **4. Avoid Nested Fields When Possible**

```java
// Good: Direct field access
FilterCondition condition = FilterCondition.builder()
    .field("status")
    .operator(FilterOperator.EQUALS)
    .value("ACTIVE")
    .build();

// Bad: Deep nested field (if not necessary)
FilterCondition condition = FilterCondition.builder()
    .field("user.profile.settings.preferences.theme")
    .operator(FilterOperator.EQUALS)
    .value("dark")
    .build();
```

## 🔧 Troubleshooting

### **Common Issues**

#### **1. Field Not Found**

```
Error: Field 'invalidField' not found in entity Product
```

**Solution**: Check field name spelling and entity structure

```java
// Check available fields
Set<FilterableFieldInfo> fields = fieldRegistry.getFilterableFields(Product.class);
fields.forEach(field -> System.out.println(field.getFieldName()));
```

#### **2. Operator Not Supported**

```
Error: Operator 'CONTAINS' not supported for field 'id'
```

**Solution**: Use appropriate operator for field type

```java
// Check supported operators
Set<FilterOperator> operators = fieldRegistry.getSupportedOperators(Product.class, "id");
System.out.println("Supported operators: " + operators);
```

#### **3. Nested Field Error**

```
Error: Cannot navigate into primitive type for field 'user.id.name'
```

**Solution**: Check intermediate field types

```java
// Check field type
Field field = Product.class.getDeclaredField("user");
Class<?> fieldType = field.getType();
System.out.println("Field type: " + fieldType.getSimpleName());
```

#### **4. Case Sensitivity Issues**

```java
// Problem: Case-sensitive search
FilterCondition condition = FilterCondition.builder()
    .field("username")
    .operator(FilterOperator.CONTAINS)
    .value("JOHN")
    .caseSensitive(true)  // May not match "john"
    .build();

// Solution: Use case-insensitive search
FilterCondition condition = FilterCondition.builder()
    .field("username")
    .operator(FilterOperator.CONTAINS)
    .value("JOHN")
    .caseSensitive(false)  // Matches "john", "John", "JOHN"
    .build();
```

### **Debug Mode**

Enable debug logging to see query processing:

```yaml
# application.yml
logging:
  level:
    com.ds.user.common.utils.EnhancedQueryParser: DEBUG
    com.ds.user.common.helper.FilterableFieldRegistry: DEBUG
    com.ds.user.common.helper.GenericQueryService: DEBUG
```

### **Query Validation**

Use validation endpoints to debug query structure:

```bash
# Validate sort configuration
curl -X POST "http://localhost:8080/api/v1/query/validate-sort?entityName=product" \
  -H "Content-Type: application/json" \
  -d '[{"field": "name", "direction": "asc"}]'

# Get field metadata
curl "http://localhost:8080/api/v1/query/metadata/product"
```

## 📚 Examples

### **Complete Example: Product Search**

```java
// Service
@Service
public class ProductService {
    @Autowired
    private FilterableFieldRegistry fieldRegistry;
    
    public PagedData<Product> searchProducts(PagingRequest query) {
        // Initialize field registry
        if (fieldRegistry.getFilterableFields(Product.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(Product.class);
        }
        
        // Set field registry for validation
        EnhancedQueryParser.setFieldRegistry(fieldRegistry);
        
        // Execute query
        return GenericQueryService.executeQuery(productRepository, query, Product.class);
    }
}

// Controller
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    
    @PostMapping("/search")
    public ResponseEntity<BaseResponse<PagedData<ProductDto>>> searchProducts(
            @Valid @RequestBody PagingRequest query) {
        
        try {
            PagedData<Product> result = productService.searchProducts(query);
            
            // Convert to DTO
            List<ProductDto> productDtos = result.getData().stream()
                    .map(ProductDto::from)
                    .toList();
            
            PagedData<ProductDto> pagedData = PagedData.<ProductDto>builder()
                    .data(productDtos)
                    .page(result.getPage())
                    .build();
            
            return ResponseEntity.ok(BaseResponse.success(pagedData));
            
        } catch (Exception e) {
            log.error("Error searching products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Search failed: " + e.getMessage()));
        }
    }
}
```

### **Frontend Integration Example**

```javascript
// JavaScript frontend example
async function searchProducts(filters, sorts, page = 0, size = 20) {
    const query = {
        filters: filters,
        sorts: sorts,
        page: page,
        size: size
    };
    
    try {
        const response = await fetch('/api/v1/products/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(query)
        });
        
        const result = await response.json();
        return result.data;
    } catch (error) {
        console.error('Search failed:', error);
        throw error;
    }
}

// Usage
const filters = {
    logic: "AND",
    conditions: [
        {
            field: "name",
            operator: "CONTAINS",
            value: "laptop",
            caseSensitive: false
        },
        {
            field: "price",
            operator: "BETWEEN",
            value: [500, 2000]
        }
    ]
};

const sorts = [
    {
        field: "price",
        direction: "asc"
    }
];

const products = await searchProducts(filters, sorts, 0, 20);
```

This completes the comprehensive usage guide for the Dynamic Query System! 🎉
