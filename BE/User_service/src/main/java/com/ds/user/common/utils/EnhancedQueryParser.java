package com.ds.user.common.utils;

import com.ds.user.common.entities.common.filter.FilterCondition;
import com.ds.user.common.entities.common.filter.FilterGroup;
import com.ds.user.common.entities.common.filter.FilterOperator;
import com.ds.user.common.entities.common.sort.SortConfig;
import com.ds.user.common.helper.FilterableFieldRegistry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.Expression;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Enhanced query parser with dynamic field validation and improved filtering logic
 */
@Slf4j
public class EnhancedQueryParser {

    private static FilterableFieldRegistry fieldRegistry;

    /**
     * Set the field registry for validation
     */
    public static void setFieldRegistry(FilterableFieldRegistry registry) {
        fieldRegistry = registry;
    }

    /**
     * Parse FilterGroup to JPA Specification with validation
     */
    public static <T> Specification<T> parseFilterGroup(FilterGroup filterGroup, Class<T> entityClass) {
        if (filterGroup == null || filterGroup.getConditions() == null || filterGroup.getConditions().isEmpty()) {
            return Specification.where(null);
        }

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            for (Object condition : filterGroup.getConditions()) {
                if (condition instanceof FilterCondition) {
                    FilterCondition filterCondition = (FilterCondition) condition;
                    
                    // Validate filter condition
                    if (isValidFilterCondition(filterCondition, entityClass)) {
                        Predicate predicate = parseFilterCondition(root, criteriaBuilder, filterCondition, entityClass);
                        if (predicate != null) {
                            predicates.add(predicate);
                        }
                    } else {
                        log.debug("[user-service] [EnhancedQueryParser.parseFilterGroup] Invalid filter condition for field '{}' with operator '{}'", 
                                filterCondition.getField(), filterCondition.getOperator());
                    }
                } else if (condition instanceof Map) {
                    // Handle nested FilterGroup
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedGroup = (Map<String, Object>) condition;
                    if ("logic".equals(nestedGroup.get("logic"))) {
                        FilterGroup nested = convertMapToFilterGroup(nestedGroup);
                        Specification<T> nestedSpec = parseFilterGroup(nested, entityClass);
                        if (nestedSpec != null) {
                            predicates.add(nestedSpec.toPredicate(root, query, criteriaBuilder));
                        }
                    }
                }
            }
            
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            
            // Apply logic (AND/OR)
            if ("OR".equalsIgnoreCase(filterGroup.getLogic())) {
                return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
            } else {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }
        };
    }

    /**
     * Validate filter condition against entity class
     */
    private static <T> boolean isValidFilterCondition(FilterCondition condition, Class<T> entityClass) {
        if (condition == null || condition.getField() == null || condition.getOperator() == null) {
            return false;
        }

        // Check if field exists in entity
        if (!isFieldExists(entityClass, condition.getField())) {
            return false;
        }

        // Check if operator is supported for this field
        if (fieldRegistry != null) {
            return fieldRegistry.validateFilterCondition(entityClass, condition.getField(), condition.getOperator());
        }

        return true; // If no registry, allow all
    }

    /**
     * Check if field exists in entity class (including nested fields)
     */
    private static <T> boolean isFieldExists(Class<T> entityClass, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        Class<?> currentClass = entityClass;
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            Field field = findField(currentClass, part);
            
            if (field == null) {
                return false;
            }
            
            // If this is not the last part, the field must be a complex object
            if (i < parts.length - 1) {
                currentClass = field.getType();
                if (currentClass.isPrimitive() || 
                    currentClass == String.class || 
                    currentClass == Date.class ||
                    Number.class.isAssignableFrom(currentClass)) {
                    return false; // Cannot navigate into primitive/complex types
                }
            }
        }
        
        return true;
    }

    /**
     * Find field in class hierarchy
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
     * Build predicate from operator, value, and path (public for V2 parser)
     */
    public static Predicate buildPredicate(CriteriaBuilder criteriaBuilder, Path<?> fieldPath, 
                                           FilterOperator operator, Object value, Boolean caseSensitive) {
        try {
            // Handle different operators
            switch (operator) {
                case EQUALS:
                    return criteriaBuilder.equal(fieldPath, value);
                    
                case NOT_EQUALS:
                    return criteriaBuilder.notEqual(fieldPath, value);
                    
                case CONTAINS:
                    return createStringPredicate(criteriaBuilder, fieldPath, value, 
                            (cb, path, val) -> cb.like(path, "%" + val + "%"), caseSensitive);
                    
                case STARTS_WITH:
                    return createStringPredicate(criteriaBuilder, fieldPath, value,
                            (cb, path, val) -> cb.like(path, val + "%"), caseSensitive);
                    
                case ENDS_WITH:
                    return createStringPredicate(criteriaBuilder, fieldPath, value,
                            (cb, path, val) -> cb.like(path, "%" + val), caseSensitive);
                    
                case GREATER_THAN:
                    return createDateComparisonPredicate(criteriaBuilder, fieldPath, value, 
                            (cb, path, val) -> cb.greaterThan(path, val));
                    
                case GREATER_THAN_OR_EQUAL:
                    return createDateComparisonPredicate(criteriaBuilder, fieldPath, value,
                            (cb, path, val) -> cb.greaterThanOrEqualTo(path, val));
                    
                case LESS_THAN:
                    return createDateComparisonPredicate(criteriaBuilder, fieldPath, value,
                            (cb, path, val) -> cb.lessThan(path, val));
                    
                case LESS_THAN_OR_EQUAL:
                    return createDateComparisonPredicate(criteriaBuilder, fieldPath, value,
                            (cb, path, val) -> cb.lessThanOrEqualTo(path, val));
                    
                case BETWEEN:
                    return createBetweenPredicate(criteriaBuilder, fieldPath, value);
                    
                case IN:
                    return fieldPath.in(convertToList(value));
                    
                case NOT_IN:
                    return criteriaBuilder.not(fieldPath.in(convertToList(value)));
                    
                case IS_NULL:
                    return criteriaBuilder.isNull(fieldPath);
                    
                case IS_NOT_NULL:
                    return criteriaBuilder.isNotNull(fieldPath);
                    
                case REGEX:
                    return createRegexPredicate(criteriaBuilder, fieldPath, value, caseSensitive);
                    
                // Array operators (simplified implementation)
                case ARRAY_CONTAINS:
                case ARRAY_NOT_CONTAINS:
                case ARRAY_OVERLAPS:
                case ARRAY_CONTAINS_ALL:
                case ARRAY_CONTAINS_ANY:
                    log.debug("[user-service] [EnhancedQueryParser.buildPredicate] Array operators not yet implemented: {}", operator);
                    return null;
                    
                default:
                    log.debug("[user-service] [EnhancedQueryParser.buildPredicate] Unsupported operator: {}", operator);
                    return null;
            }
        } catch (Exception e) {
            log.error("[user-service] [EnhancedQueryParser.buildPredicate] Error building predicate: operator={}, value={}", 
                    operator, value, e);
            return null;
        }
    }

    /**
     * Parse individual filter condition with enhanced logic
     */
    private static <T> Predicate parseFilterCondition(Root<T> root, CriteriaBuilder criteriaBuilder, 
                                                    FilterCondition condition, Class<T> entityClass) {
        try {
            String field = condition.getField();
            FilterOperator operator = condition.getOperator();
            Object value = condition.getValue();
            Boolean caseSensitive = condition.getCaseSensitive();

            // Get field path
            Path<?> fieldPath = getFieldPath(root, field);
            if (fieldPath == null) {
                log.debug("[user-service] [EnhancedQueryParser.parseFilterCondition] Field path not found: {}", field);
                return null;
            }

            // Use the shared buildPredicate method
            return buildPredicate(criteriaBuilder, fieldPath, operator, value, caseSensitive);
            
        } catch (Exception e) {
            log.error("[user-service] [EnhancedQueryParser.parseFilterCondition] Error parsing filter condition: field={}, operator={}, value={}", 
                    condition.getField(), condition.getOperator(), condition.getValue(), e);
            return null;
        }
    }

    /**
     * Create string predicate with case sensitivity handling
     */
    private static Predicate createStringPredicate(CriteriaBuilder criteriaBuilder, Path<?> fieldPath, 
                                                 Object value, StringPredicateFunction function, Boolean caseSensitive) {
        if (value == null) {
            return criteriaBuilder.isNull(fieldPath);
        }
        
        String stringValue = value.toString();
        Path<String> stringPath = (Path<String>) fieldPath;
        
        if (Boolean.FALSE.equals(caseSensitive)) {
            return function.apply(criteriaBuilder, 
                    criteriaBuilder.lower(stringPath), 
                    stringValue.toLowerCase());
        } else {
            return function.apply(criteriaBuilder, stringPath, stringValue);
        }
    }

    /**
     * Create between predicate with date support
     */
    private static Predicate createBetweenPredicate(CriteriaBuilder criteriaBuilder, Path<?> fieldPath, Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() >= 2) {
                Object startValue = parseDateValue(list.get(0));
                Object endValue = parseDateValue(list.get(1));
                
                if (startValue != null && endValue != null) {
                    return criteriaBuilder.between((Path<Comparable>) fieldPath, 
                            (Comparable) startValue, (Comparable) endValue);
                }
            }
        }
        return null;
    }

    /**
     * Create regex predicate
     */
    private static Predicate createRegexPredicate(CriteriaBuilder criteriaBuilder, Path<?> fieldPath, 
                                                Object value, Boolean caseSensitive) {
        if (value == null) {
            return criteriaBuilder.isNull(fieldPath);
        }
        
        try {
            String pattern = value.toString();
            Path<String> stringPath = (Path<String>) fieldPath;
            
            if (Boolean.FALSE.equals(caseSensitive)) {
                Expression<Boolean> regexExpr = criteriaBuilder.function("REGEXP", Boolean.class, 
                        criteriaBuilder.lower(stringPath), 
                        criteriaBuilder.literal(pattern.toLowerCase()));
                return criteriaBuilder.equal(regexExpr, true);
            } else {
                Expression<Boolean> regexExpr = criteriaBuilder.function("REGEXP", Boolean.class, 
                        stringPath, 
                        criteriaBuilder.literal(pattern));
                return criteriaBuilder.equal(regexExpr, true);
            }
        } catch (Exception e) {
            log.debug("[user-service] [EnhancedQueryParser.createRegexPredicate] Error creating regex predicate: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get field path from root, handling nested properties
     */
    private static <T> Path<?> getFieldPath(Root<T> root, String field) {
        try {
            String[] parts = field.split("\\.");
            Path<?> path = root;
            for (String part : parts) {
                path = path.get(part);
            }
            return path;
        } catch (Exception e) {
            log.debug("[user-service] [EnhancedQueryParser.getFieldPath] Error getting field path for: {}, error: {}", field, e.getMessage());
            return null;
        }
    }

    /**
     * Convert value to list for IN/NOT_IN operations
     */
    private static List<?> convertToList(Object value) {
        if (value instanceof List) {
            return (List<?>) value;
        } else if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        } else {
            return Collections.singletonList(value);
        }
    }

    /**
     * Parse SortConfig list to JPA Sort with validation
     */
    public static <T> Sort parseSortConfigs(List<SortConfig> sortConfigs, Class<T> entityClass) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (SortConfig sortConfig : sortConfigs) {
            if (sortConfig.getField() != null && sortConfig.getDirection() != null) {
                // Validate field exists
                if (isFieldExists(entityClass, sortConfig.getField())) {
                    Sort.Direction direction = "desc".equalsIgnoreCase(sortConfig.getDirection()) 
                            ? Sort.Direction.DESC : Sort.Direction.ASC;
                    orders.add(new Sort.Order(direction, sortConfig.getField()));
                } else {
                    log.debug("[user-service] [EnhancedQueryParser.parseSortConfigs] Sort field '{}' not found in entity {}", sortConfig.getField(), entityClass.getSimpleName());
                }
            }
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * Convert Map to FilterGroup (for handling nested groups from JSON)
     */
    @SuppressWarnings("unchecked")
    private static FilterGroup convertMapToFilterGroup(Map<String, Object> map) {
        FilterGroup.FilterGroupBuilder builder = FilterGroup.builder();
        
        if (map.containsKey("logic")) {
            builder.logic((String) map.get("logic"));
        }
        
        if (map.containsKey("conditions")) {
            builder.conditions((List<Object>) map.get("conditions"));
        }
        
        return builder.build();
    }

    /**
     * Create date comparison predicate with proper date parsing
     */
    private static Predicate createDateComparisonPredicate(CriteriaBuilder criteriaBuilder, Path<?> fieldPath, 
                                                         Object value, DateComparisonFunction function) {
        if (value == null) {
            return criteriaBuilder.isNull(fieldPath);
        }
        
        Object parsedValue = parseDateValue(value);
        if (parsedValue == null) {
            log.debug("[user-service] [EnhancedQueryParser.parseDateValue] Unable to parse date value: {}", value);
            return null;
        }
        
        return function.apply(criteriaBuilder, (Path<Comparable>) fieldPath, (Comparable) parsedValue);
    }

    /**
     * Parse date value from various formats
     */
    private static Object parseDateValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // If already a Date or LocalDate/LocalDateTime, return as is
        if (value instanceof Date || value instanceof LocalDate || value instanceof LocalDateTime) {
            return value;
        }
        
        // If it's a String, try to parse it
        if (value instanceof String) {
            String dateStr = (String) value;
            
            // Try different date formats
            String[] dateFormats = {
                "yyyy-MM-dd",           // 2024-01-15
                "yyyy-MM-dd HH:mm:ss",  // 2024-01-15 10:30:00
                "yyyy-MM-dd'T'HH:mm:ss", // 2024-01-15T10:30:00
                "dd/MM/yyyy",           // 15/01/2024
                "MM/dd/yyyy",           // 01/15/2024
                "dd-MM-yyyy",           // 15-01-2024
                "yyyy/MM/dd"            // 2024/01/15
            };
            
            for (String format : dateFormats) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    
                    // Try LocalDate first
                    if (format.equals("yyyy-MM-dd") || format.equals("dd/MM/yyyy") || 
                        format.equals("MM/dd/yyyy") || format.equals("dd-MM-yyyy") || 
                        format.equals("yyyy/MM/dd")) {
                        LocalDate localDate = LocalDate.parse(dateStr, formatter);
                        return localDate;
                    }
                    // Try LocalDateTime
                    else {
                        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, formatter);
                        return localDateTime;
                    }
                } catch (DateTimeParseException e) {
                    // Continue to next format
                }
            }
            
            log.debug("[user-service] [EnhancedQueryParser.parseDateValue] Unable to parse date string: {}", dateStr);
            return null;
        }
        
        // If it's a Number (timestamp), convert to Date
        if (value instanceof Number) {
            long timestamp = ((Number) value).longValue();
            return new Date(timestamp);
        }
        
        return value;
    }

    /**
     * Functional interface for date comparison predicates
     */
    @FunctionalInterface
    private interface DateComparisonFunction {
        Predicate apply(CriteriaBuilder cb, Path<Comparable> path, Comparable value);
    }

    /**
     * Functional interface for string predicates
     */
    @FunctionalInterface
    private interface StringPredicateFunction {
        Predicate apply(CriteriaBuilder cb, Expression<String> path, String value);
    }
}
