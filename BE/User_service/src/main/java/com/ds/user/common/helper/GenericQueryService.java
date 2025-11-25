package com.ds.user.common.helper;

import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.common.paging.Paging;
import com.ds.user.common.entities.common.sort.SortConfig;
import com.ds.user.common.utils.EnhancedQueryParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;

/**
 * Generic service for handling queries with filtering and sorting
 * Supports any entity with JPA repository
 */
@Slf4j
public class GenericQueryService {

    /**
     * Execute a paginated query with filtering and sorting
     * 
     * @param repository  JPA repository that extends JpaSpecificationExecutor
     * @param query       PagingRequest containing filters, sorts, pagination
     * @param entityClass Entity class for reflection-based sorting
     * @param <T>         Entity type
     * @return PagedData with results
     */
    public static <T> PagedData<T> executeQuery(
            JpaSpecificationExecutor<T> repository,
            PagingRequest query,
            Class<T> entityClass) {

        // Parse filters to JPA Specification using enhanced parser
        Specification<T> specification = EnhancedQueryParser.parseFilterGroup(query.getFiltersOrEmpty(), entityClass);

        // Get all entities matching the specification
        List<T> allEntities = repository.findAll(specification);

        // Apply sorting
        List<T> sortedEntities = applySorting(allEntities, query.getSortsOrEmpty(), entityClass);

        // Calculate pagination
        int totalElements = sortedEntities.size();
        int totalPages = (int) Math.ceil((double) totalElements / query.getSize());

        // Get the requested page of data
        int startIndex = query.getPage() * query.getSize();
        int endIndex = Math.min(startIndex + query.getSize(), sortedEntities.size());

        List<T> pageData = sortedEntities.subList(startIndex, endIndex);

        // Create Paging object
        var paging = Paging.<String>builder()
                .page(query.getPage())
                .size(query.getSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .filters(query.getFiltersOrEmpty())
                .sorts(query.getSortsOrEmpty())
                .selected(query.getSelectedOrEmpty())
                .build();

        // Create PagedData
        return PagedData.<T>builder()
                .data(pageData)
                .page(paging)
                .build();
    }

    /**
     * Apply sorting to a list of entities using reflection
     * 
     * @param entities    List of entities to sort
     * @param sortConfigs List of sort configurations
     * @param entityClass Entity class for reflection
     * @param <T>         Entity type
     * @return Sorted list of entities
     */
    private static <T> List<T> applySorting(List<T> entities, List<SortConfig> sortConfigs, Class<T> entityClass) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return entities;
        }

        return entities.stream()
                .sorted((a, b) -> {
                    for (SortConfig sortConfig : sortConfigs) {
                        int comparison = compareByField(a, b, sortConfig.getField(), sortConfig.getDirection(),
                                entityClass);
                        if (comparison != 0) {
                            return comparison;
                        }
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Compare two entities by a specific field using reflection
     * 
     * @param a           First entity
     * @param b           Second entity
     * @param fieldName   Field name to compare
     * @param direction   Sort direction (asc/desc)
     * @param entityClass Entity class for reflection
     * @param <T>         Entity type
     * @return Comparison result (-1, 0, 1)
     */
    private static <T> int compareByField(T a, T b, String fieldName, String direction, Class<T> entityClass) {
        try {
            // Handle nested field access (e.g., "user.name")
            Object valueA = getFieldValue(a, fieldName);
            Object valueB = getFieldValue(b, fieldName);

            int comparison = compareValues(valueA, valueB);

            // Reverse comparison for descending order
            return "desc".equalsIgnoreCase(direction) ? -comparison : comparison;

        } catch (Exception e) {
            log.debug("[user-service] [GenericQueryService.compareEntities] Error comparing field '{}' for sorting: {}", fieldName, e.getMessage());
            return 0;
        }
    }

    /**
     * Get field value using reflection, supporting nested properties
     * 
     * @param entity    Entity to get value from
     * @param fieldPath Field path (e.g., "user.name" or "address.city")
     * @return Field value
     */
    private static Object getFieldValue(Object entity, String fieldPath) {
        if (entity == null || fieldPath == null) {
            return null;
        }

        String[] parts = fieldPath.split("\\.");
        Object current = entity;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            Field field = findField(current.getClass(), part);
            if (field == null) {
                log.debug("[user-service] [GenericQueryService.getFieldValue] Field '{}' not found in class {}", part, current.getClass().getSimpleName());
                return null;
            }

            field.setAccessible(true);
            try {
                current = field.get(current);
            } catch (IllegalAccessException e) {
                log.debug("[user-service] [GenericQueryService.getFieldValue] Error accessing field '{}': {}", part, e.getMessage());
                return null;
            }
        }

        return current;
    }

    /**
     * Find field in class hierarchy
     * 
     * @param clazz     Class to search in
     * @param fieldName Field name to find
     * @return Field or null if not found
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && !clazz.equals(Object.class)) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Compare two values for sorting
     * 
     * @param a First value
     * @param b Second value
     * @return Comparison result (-1, 0, 1)
     */
    @SuppressWarnings("unchecked")
    private static int compareValues(Object a, Object b) {
        // Handle null values
        if (a == null && b == null)
            return 0;
        if (a == null)
            return -1;
        if (b == null)
            return 1;

        // Handle Comparable objects
        if (a instanceof Comparable && b instanceof Comparable) {
            try {
                return ((Comparable<Object>) a).compareTo(b);
            } catch (ClassCastException e) {
                log.debug("[user-service] [GenericQueryService.compareValues] Cannot compare {} with {}: {}", a.getClass().getSimpleName(), b.getClass().getSimpleName(),
                        e.getMessage());
            }
        }

        // Handle String comparison
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareToIgnoreCase((String) b);
        }

        // Handle Number comparison
        if (a instanceof Number && b instanceof Number) {
            double numA = ((Number) a).doubleValue();
            double numB = ((Number) b).doubleValue();
            return Double.compare(numA, numB);
        }

        // Handle Date comparison
        if (a instanceof Date && b instanceof Date) {
            return ((Date) a).compareTo((Date) b);
        }

        // Fallback to string representation
        return a.toString().compareToIgnoreCase(b.toString());
    }

    /**
     * Get available sortable fields for an entity class
     * 
     * @param entityClass Entity class to analyze
     * @return List of sortable field names
     */
    public static List<String> getSortableFields(Class<?> entityClass) {
        List<String> fields = new ArrayList<>();
        Class<?> currentClass = entityClass;

        while (currentClass != null && !currentClass.equals(Object.class)) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                // Skip synthetic fields and static fields
                if (!field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    fields.add(field.getName());
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    /**
     * Validate sort configuration against entity class
     * 
     * @param sortConfigs List of sort configurations to validate
     * @param entityClass Entity class to validate against
     * @return List of valid sort configurations
     */
    public static List<SortConfig> validateSortConfigs(List<SortConfig> sortConfigs, Class<?> entityClass) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> availableFields = getSortableFields(entityClass);

        return sortConfigs.stream()
                .filter(config -> {
                    if (config == null || config.getField() == null) {
                        return false;
                    }

                    // Check if field exists (support nested fields)
                    String[] parts = config.getField().split("\\.");
                    if (parts.length == 1) {
                        return availableFields.contains(config.getField());
                    } else {
                        // For nested fields, we'll be more lenient and let runtime handle it
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }
}
