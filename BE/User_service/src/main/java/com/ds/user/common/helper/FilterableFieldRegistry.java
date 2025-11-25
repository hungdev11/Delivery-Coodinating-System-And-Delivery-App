package com.ds.user.common.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.ds.user.common.entities.common.filter.FilterOperator;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;

/**
 * Registry for managing filterable fields for different entity types
 * Supports dynamic field discovery and validation
 */
@Slf4j
@Component
public class FilterableFieldRegistry {

    private final Map<Class<?>, Set<FilterableFieldInfo>> fieldRegistry = new ConcurrentHashMap<>();

    /**
     * Register filterable fields for an entity class
     * 
     * @param entityClass Entity class
     * @param fieldInfos Set of filterable field information
     */
    public void registerFields(Class<?> entityClass, Set<FilterableFieldInfo> fieldInfos) {
        fieldRegistry.put(entityClass, fieldInfos);
        log.debug("[user-service] [FilterableFieldRegistry.registerFields] Registered {} filterable fields for {}", fieldInfos.size(), entityClass.getSimpleName());
    }

    /**
     * Auto-discover and register filterable fields for an entity class
     * 
     * @param entityClass Entity class to analyze
     */
    public void autoDiscoverFields(Class<?> entityClass) {
        Set<FilterableFieldInfo> fieldInfos = new HashSet<>();
        
        // Analyze all fields in the class hierarchy
        Class<?> currentClass = entityClass;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                // Skip synthetic fields, static fields, and transient fields
                if (!field.isSynthetic() && 
                    !java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                    !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    
                    FilterableFieldInfo fieldInfo = createFieldInfo(field);
                    fieldInfos.add(fieldInfo);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        registerFields(entityClass, fieldInfos);
    }

    /**
     * Create FilterableFieldInfo from a Field
     * 
     * @param field Field to analyze
     * @return FilterableFieldInfo
     */
    private FilterableFieldInfo createFieldInfo(Field field) {
        Class<?> fieldType = field.getType();
        Set<FilterOperator> supportedOperators = getSupportedOperators(fieldType);
        
        return FilterableFieldInfo.builder()
                .fieldName(field.getName())
                .fieldType(fieldType)
                .supportedOperators(supportedOperators)
                .isNested(false)
                .isSearchable(isSearchableType(fieldType))
                .build();
    }

    /**
     * Get supported operators for a field type
     * 
     * @param fieldType Field type
     * @return Set of supported operators
     */
    private Set<FilterOperator> getSupportedOperators(Class<?> fieldType) {
        Set<FilterOperator> operators = new HashSet<>();
        
        if (fieldType == String.class) {
            operators.addAll(Arrays.asList(
                FilterOperator.EQUALS,
                FilterOperator.NOT_EQUALS,
                FilterOperator.CONTAINS,
                FilterOperator.STARTS_WITH,
                FilterOperator.ENDS_WITH,
                FilterOperator.REGEX,
                FilterOperator.IS_NULL,
                FilterOperator.IS_NOT_NULL
            ));
        } else if (Number.class.isAssignableFrom(fieldType) || 
                   fieldType.isPrimitive() && (fieldType == int.class || fieldType == long.class || 
                   fieldType == double.class || fieldType == float.class)) {
            operators.addAll(Arrays.asList(
                FilterOperator.EQUALS,
                FilterOperator.NOT_EQUALS,
                FilterOperator.GREATER_THAN,
                FilterOperator.GREATER_THAN_OR_EQUAL,
                FilterOperator.LESS_THAN,
                FilterOperator.LESS_THAN_OR_EQUAL,
                FilterOperator.BETWEEN,
                FilterOperator.IN,
                FilterOperator.NOT_IN,
                FilterOperator.IS_NULL,
                FilterOperator.IS_NOT_NULL
            ));
        } else if (fieldType == Date.class || 
                   fieldType.getName().startsWith("java.time.")) {
            operators.addAll(Arrays.asList(
                FilterOperator.EQUALS,
                FilterOperator.NOT_EQUALS,
                FilterOperator.GREATER_THAN,
                FilterOperator.GREATER_THAN_OR_EQUAL,
                FilterOperator.LESS_THAN,
                FilterOperator.LESS_THAN_OR_EQUAL,
                FilterOperator.BETWEEN,
                FilterOperator.IS_NULL,
                FilterOperator.IS_NOT_NULL
            ));
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            operators.addAll(Arrays.asList(
                FilterOperator.EQUALS,
                FilterOperator.NOT_EQUALS,
                FilterOperator.IS_NULL,
                FilterOperator.IS_NOT_NULL
            ));
        } else if (fieldType.isEnum()) {
            operators.addAll(Arrays.asList(
                FilterOperator.EQUALS,
                FilterOperator.NOT_EQUALS,
                FilterOperator.IN,
                FilterOperator.NOT_IN,
                FilterOperator.IS_NULL,
                FilterOperator.IS_NOT_NULL
            ));
        } else {
            // For complex objects, only support basic operators
            operators.addAll(Arrays.asList(
                FilterOperator.EQUALS,
                FilterOperator.NOT_EQUALS,
                FilterOperator.IS_NULL,
                FilterOperator.IS_NOT_NULL
            ));
        }
        
        return operators;
    }

    /**
     * Check if a field type is searchable (for global search)
     * 
     * @param fieldType Field type
     * @return true if searchable
     */
    private boolean isSearchableType(Class<?> fieldType) {
        return fieldType == String.class || 
               fieldType == Character.class || 
               fieldType == char.class;
    }

    /**
     * Get filterable fields for an entity class
     * 
     * @param entityClass Entity class
     * @return Set of filterable field information
     */
    public Set<FilterableFieldInfo> getFilterableFields(Class<?> entityClass) {
        return fieldRegistry.getOrDefault(entityClass, new HashSet<>());
    }

    /**
     * Check if a field is filterable for an entity class
     * 
     * @param entityClass Entity class
     * @param fieldName Field name
     * @return true if filterable
     */
    public boolean isFieldFilterable(Class<?> entityClass, String fieldName) {
        Set<FilterableFieldInfo> fields = getFilterableFields(entityClass);
        return fields.stream()
                .anyMatch(field -> field.getFieldName().equals(fieldName));
    }

    /**
     * Get supported operators for a specific field
     * 
     * @param entityClass Entity class
     * @param fieldName Field name
     * @return Set of supported operators
     */
    public Set<FilterOperator> getSupportedOperators(Class<?> entityClass, String fieldName) {
        Set<FilterableFieldInfo> fields = getFilterableFields(entityClass);
        return fields.stream()
                .filter(field -> field.getFieldName().equals(fieldName))
                .findFirst()
                .map(FilterableFieldInfo::getSupportedOperators)
                .orElse(new HashSet<>());
    }

    /**
     * Validate a filter condition against registered fields
     * 
     * @param entityClass Entity class
     * @param fieldName Field name
     * @param operator Filter operator
     * @return true if valid
     */
    public boolean validateFilterCondition(Class<?> entityClass, String fieldName, FilterOperator operator) {
        Set<FilterableFieldInfo> fields = getFilterableFields(entityClass);
        return fields.stream()
                .anyMatch(field -> field.getFieldName().equals(fieldName) && 
                                 field.getSupportedOperators().contains(operator));
    }

    /**
     * Get all registered entity classes
     * 
     * @return Set of registered entity classes
     */
    public Set<Class<?>> getRegisteredEntityClasses() {
        return fieldRegistry.keySet();
    }

    /**
     * Clear all registered fields
     */
    public void clear() {
        fieldRegistry.clear();
    }

    /**
     * Information about a filterable field
     */
    public static class FilterableFieldInfo {
        private String fieldName;
        private Class<?> fieldType;
        private Set<FilterOperator> supportedOperators;
        private boolean isNested;
        private boolean isSearchable;

        // Builder pattern
        public static FilterableFieldInfoBuilder builder() {
            return new FilterableFieldInfoBuilder();
        }

        // Getters and setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        
        public Class<?> getFieldType() { return fieldType; }
        public void setFieldType(Class<?> fieldType) { this.fieldType = fieldType; }
        
        public Set<FilterOperator> getSupportedOperators() { return supportedOperators; }
        public void setSupportedOperators(Set<FilterOperator> supportedOperators) { this.supportedOperators = supportedOperators; }
        
        public boolean isNested() { return isNested; }
        public void setNested(boolean nested) { isNested = nested; }
        
        public boolean isSearchable() { return isSearchable; }
        public void setSearchable(boolean searchable) { isSearchable = searchable; }

        public static class FilterableFieldInfoBuilder {
            private String fieldName;
            private Class<?> fieldType;
            private Set<FilterOperator> supportedOperators;
            private boolean isNested;
            private boolean isSearchable;

            public FilterableFieldInfoBuilder fieldName(String fieldName) {
                this.fieldName = fieldName;
                return this;
            }

            public FilterableFieldInfoBuilder fieldType(Class<?> fieldType) {
                this.fieldType = fieldType;
                return this;
            }

            public FilterableFieldInfoBuilder supportedOperators(Set<FilterOperator> supportedOperators) {
                this.supportedOperators = supportedOperators;
                return this;
            }

            public FilterableFieldInfoBuilder isNested(boolean isNested) {
                this.isNested = isNested;
                return this;
            }

            public FilterableFieldInfoBuilder isSearchable(boolean isSearchable) {
                this.isSearchable = isSearchable;
                return this;
            }

            public FilterableFieldInfo build() {
                FilterableFieldInfo info = new FilterableFieldInfo();
                info.setFieldName(fieldName);
                info.setFieldType(fieldType);
                info.setSupportedOperators(supportedOperators);
                info.setNested(isNested);
                info.setSearchable(isSearchable);
                return info;
            }
        }
    }
}
