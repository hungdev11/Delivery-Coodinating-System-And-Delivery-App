package com.ds.gateway.common.entities.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Paging request DTO for API Gateway
 * Supports filtering, sorting, and pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagingRequest {
        /**
     * Page number (0-based), default 0
     */
    @Builder.Default
    private int page = 0;
    
    /**
     * Items per page, default 10
     */
    @Builder.Default
    private int size = 10;
    
    /**
     * Applied filters (optional) - MongoDB-style groups
     */
    private Optional<FilterGroup> filters = Optional.empty();
    
    /**
     * Sort configuration (optional)
     */
    private Optional<List<SortConfig>> sorts = Optional.empty();
    
    /**
     * Global search term (optional)
     */
    private Optional<String> search = Optional.empty();
    
    /**
     * Selected item IDs (optional)
     */
    private Optional<List<String>> selected = Optional.empty();
    
    
    /**
     * Get filters or empty list
     */
    public FilterGroup getFiltersOrEmpty() {
        return filters.orElse(null);
    }
    
    /**
     * Get sorts or empty list
     */
    public List<SortConfig> getSortsOrEmpty() {
        return sorts.orElse(List.of());
    }
    
    /**
     * Get search term or empty string
     */
    public String getSearchOrEmpty() {
        return search.orElse("");
    }
    
    /**
     * Get selected IDs or empty list
     */
    public List<String> getSelectedOrEmpty() {
        return selected.orElse(List.of());
    }
}
