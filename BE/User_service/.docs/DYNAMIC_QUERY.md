# Dynamic Query System

This document provides a complete guide to the Dynamic Query System, a powerful feature for advanced filtering, sorting, and pagination.

## 1. Overview

The Dynamic Query System allows you to perform complex queries on any entity through the REST API. It is designed to be generic, type-safe, and easily extensible.

### Key Features

*   **Dynamic Field Discovery**: Automatically discovers filterable and sortable fields from entities.
*   **Type-Safe Operators**: Provides appropriate filter operators based on field types (String, Number, Date, etc.).
*   **Nested Field Support**: Filter and sort on nested properties (e.g., `user.profile.firstName`).
*   **Generic Implementation**: A single set of services and controllers can query any entity.
*   **Metadata API**: Endpoints to provide frontend applications with information about filterable fields and supported operators.

## 2. API Request Structure

All dynamic queries are sent as a `POST` request with a `PagingRequest` object in the body.

```json
{
  "filters": { ... },
  "sorts": [ ... ],
  "page": 0,
  "size": 10,
  "search": "...",
  "selected": [ ... ]
}
```

### `filters` Object

Defines the filtering criteria.

*   `logic`: `"AND"` or `"OR"`
*   `conditions`: An array of `FilterCondition` objects or nested `FilterGroup` objects.

#### `FilterCondition` Object

*   `field`: The name of the field to filter on (e.g., `"username"`).
*   `operator`: The filter operator (e.g., `"CONTAINS"`).
*   `value`: The value to compare against.
*   `caseSensitive`: (Optional) `true` or `false` for string comparisons.

### `sorts` Array

An array of `SortConfig` objects.

#### `SortConfig` Object

*   `field`: The name of the field to sort by.
*   `direction`: `"asc"` or `"desc"`.

## 3. Supported Operators

### String Fields
*   `EQUALS`, `NOT_EQUALS`, `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `REGEX`, `IS_NULL`, `IS_NOT_NULL`

### Numeric & Date Fields
*   `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `BETWEEN`, `IS_NULL`, `IS_NOT_NULL`

### Enum & Boolean Fields
*   `EQUALS`, `NOT_EQUALS`, `IN`, `NOT_IN`, `IS_NULL`, `IS_NOT_NULL`

## 4. Usage Guide

### Basic Filtering

To filter for users with `status = "ACTIVE"`:

```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      {
        "field": "status",
        "operator": "EQUALS",
        "value": "ACTIVE"
      }
    ]
  }
}
```

### Advanced Filtering (Nested Logic)

To filter for users where `status = "ACTIVE"` AND (`username` contains `"admin"` OR `email` contains `"admin"`):

```json
{
  "filters": {
    "logic": "AND",
    "conditions": [
      { "field": "status", "operator": "EQUALS", "value": "ACTIVE" },
      {
        "logic": "OR",
        "conditions": [
          { "field": "username", "operator": "CONTAINS", "value": "admin" },
          { "field": "email", "operator": "CONTAINS", "value": "admin" }
        ]
      }
    ]
  }
}
```

### Sorting

To sort by `createdAt` descending, then `username` ascending:

```json
{
  "sorts": [
    { "field": "createdAt", "direction": "desc" },
    { "field": "username", "direction": "asc" }
  ]
}
```

### Pagination

To get the second page of 20 items:

```json
{
  "page": 1, // 0-based index
  "size": 20
}
```

## 5. Metadata API for Frontend Integration

To build dynamic UI for filtering, the frontend can use these endpoints:

*   `GET /api/v1/query/metadata/{entityName}`: Get all query metadata for an entity.
*   `GET /api/v1/query/filterable-fields/{entityName}`: Get a list of filterable fields.
*   `GET /api/v1/query/sortable-fields/{entityName}`: Get a list of sortable fields.
*   `GET /api/v1/query/supported-operators/{entityName}/{fieldName}`: Get the supported operators for a specific field.

## 6. Architecture & File Structure

The core components of the Dynamic Query System are located in `src/main/java/com/ds/user/common/`.

*   **DTOs** (`entities/common/`): `PagingRequest.java`, `FilterGroup.java`, `FilterCondition.java`, `SortConfig.java`.
*   **Parsing Engine** (`utils/EnhancedQueryParser.java`): Converts the `PagingRequest` into a JPA `Specification` and `Sort` object.
*   **Field Registry** (`helper/FilterableFieldRegistry.java`): Manages which fields of an entity are filterable/sortable and their supported operators.
*   **Generic Executor** (`helper/GenericQueryService.java`): A generic service that can execute a query for any repository and entity type.
*   **Metadata Service** (`helper/QueryService.java`): Provides the logic for the metadata API endpoints.

### How to Integrate a New Entity

1.  **Enable Discovery**: In a `@Configuration` class, call `fieldRegistry.autoDiscoverFields(NewEntity.class);` to make the entity queryable.
2.  **Use in Service**: In your new service, inject `GenericQueryService` and use it to execute queries.

    ```java
    @Service
    public class NewEntityService {
        @Autowired
        private GenericQueryService queryService;

        public PagedData<NewEntity> getEntities(PagingRequest query) {
            return queryService.executeQuery(newEntityRepository, query, NewEntity.class);
        }
    }
    ```
