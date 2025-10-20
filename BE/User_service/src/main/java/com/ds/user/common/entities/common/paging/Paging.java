package com.ds.user.common.entities.common.paging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.ds.user.common.entities.common.filter.FilterGroup;
import com.ds.user.common.entities.common.sort.SortConfig;

/**
 * Pagination information
 * Follows the standard defined in RESTFUL.md
 * 
 * @param <TKey> The type of the key for selected items
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paging<TKey> {
    /**
     * Current page number (0-based)
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int size;
    
    /**
     * Total number of items across all pages
     */
    private long totalElements;
    
    /**
     * Total number of pages
     */
    private int totalPages;
    
    /**
     * Applied filters (optional) - Updated to support FilterGroup
     */
    private FilterGroup filters;
    
    /**
     * Sort configuration (optional) - Updated to support SortConfig
     */
    @Builder.Default
    private List<SortConfig> sorts = List.of();
    
    /**
     * Selected item IDs (optional)
     */
    private List<TKey> selected;
}
