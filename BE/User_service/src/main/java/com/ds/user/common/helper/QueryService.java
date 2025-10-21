package com.ds.user.common.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ds.user.common.entities.common.filter.FilterOperator;
import com.ds.user.common.entities.common.sort.SortConfig;

import java.util.List;
import java.util.Set;

/**
 * Service for providing query-related information and utilities
 */
@Slf4j
@Service
public class QueryService {

    @Autowired
    private FilterableFieldRegistry fieldRegistry;

    /**
     * Get filterable fields for an entity class
     * 
     * @param entityClass Entity class
     * @return Set of filterable field information
     */
    public Set<FilterableFieldRegistry.FilterableFieldInfo> getFilterableFields(Class<?> entityClass) {
        // Auto-discover fields if not already registered
        if (fieldRegistry.getFilterableFields(entityClass).isEmpty()) {
            fieldRegistry.autoDiscoverFields(entityClass);
        }
        
        return fieldRegistry.getFilterableFields(entityClass);
    }

    /**
     * Get sortable fields for an entity class
     * 
     * @param entityClass Entity class
     * @return List of sortable field names
     */
    public List<String> getSortableFields(Class<?> entityClass) {
        return GenericQueryService.getSortableFields(entityClass);
    }

    /**
     * Validate sort configuration against entity class
     * 
     * @param sortConfigs List of sort configurations to validate
     * @param entityClass Entity class to validate against
     * @return List of valid sort configurations
     */
    public List<SortConfig> validateSortConfigs(List<SortConfig> sortConfigs, Class<?> entityClass) {
        return GenericQueryService.validateSortConfigs(sortConfigs, entityClass);
    }

    /**
     * Check if a field is filterable for an entity class
     * 
     * @param entityClass Entity class
     * @param fieldName Field name
     * @return true if filterable
     */
    public boolean isFieldFilterable(Class<?> entityClass, String fieldName) {
        return fieldRegistry.isFieldFilterable(entityClass, fieldName);
    }

    /**
     * Get supported operators for a specific field
     * 
     * @param entityClass Entity class
     * @param fieldName Field name
     * @return Set of supported operators
     */
    public Set<FilterOperator> getSupportedOperators(
            Class<?> entityClass, String fieldName) {
        return fieldRegistry.getSupportedOperators(entityClass, fieldName);
    }

    /**
     * Get query metadata for an entity class
     * 
     * @param entityClass Entity class
     * @return QueryMetadata containing filterable and sortable fields
     */
    public QueryMetadata getQueryMetadata(Class<?> entityClass) {
        Set<FilterableFieldRegistry.FilterableFieldInfo> filterableFields = getFilterableFields(entityClass);
        List<String> sortableFields = getSortableFields(entityClass);
        
        return QueryMetadata.builder()
                .entityClass(entityClass.getSimpleName())
                .filterableFields(filterableFields)
                .sortableFields(sortableFields)
                .build();
    }

    /**
     * Query metadata for an entity
     */
    public static class QueryMetadata {
        private String entityClass;
        private Set<FilterableFieldRegistry.FilterableFieldInfo> filterableFields;
        private List<String> sortableFields;

        // Builder pattern
        public static QueryMetadataBuilder builder() {
            return new QueryMetadataBuilder();
        }

        // Getters and setters
        public String getEntityClass() { return entityClass; }
        public void setEntityClass(String entityClass) { this.entityClass = entityClass; }
        
        public Set<FilterableFieldRegistry.FilterableFieldInfo> getFilterableFields() { return filterableFields; }
        public void setFilterableFields(Set<FilterableFieldRegistry.FilterableFieldInfo> filterableFields) { 
            this.filterableFields = filterableFields; 
        }
        
        public List<String> getSortableFields() { return sortableFields; }
        public void setSortableFields(List<String> sortableFields) { this.sortableFields = sortableFields; }

        public static class QueryMetadataBuilder {
            private String entityClass;
            private Set<FilterableFieldRegistry.FilterableFieldInfo> filterableFields;
            private List<String> sortableFields;

            public QueryMetadataBuilder entityClass(String entityClass) {
                this.entityClass = entityClass;
                return this;
            }

            public QueryMetadataBuilder filterableFields(Set<FilterableFieldRegistry.FilterableFieldInfo> filterableFields) {
                this.filterableFields = filterableFields;
                return this;
            }

            public QueryMetadataBuilder sortableFields(List<String> sortableFields) {
                this.sortableFields = sortableFields;
                return this;
            }

            public QueryMetadata build() {
                QueryMetadata metadata = new QueryMetadata();
                metadata.setEntityClass(entityClass);
                metadata.setFilterableFields(filterableFields);
                metadata.setSortableFields(sortableFields);
                return metadata;
            }
        }
    }
}
