package com.ds.user.common.entities.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ds.user.common.entities.common.filter.v2.FilterGroupItemV2;
import com.ds.user.common.entities.common.sort.SortConfig;

import java.util.List;
import java.util.Optional;

/**
 * Paging request with V2 filter system
 * V2 allows operations between each pair of conditions/groups
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagingRequestV2 {
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
     * Applied filters (optional) - V2 system with operations between each pair
     */
    private Optional<FilterGroupItemV2> filters = Optional.empty();
    
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
     * Convert to Spring Pageable
     */
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
    
    /**
     * Convert to Spring Pageable with sort
     */
    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * Get filters or null
     */
    public FilterGroupItemV2 getFiltersOrNull() {
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
