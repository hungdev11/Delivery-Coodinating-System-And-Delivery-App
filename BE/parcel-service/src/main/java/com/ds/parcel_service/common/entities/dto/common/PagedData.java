package com.ds.parcel_service.common.entities.dto.common;

import com.ds.parcel_service.common.entities.dto.filter.v2.FilterGroupItemV2;
import com.ds.parcel_service.common.entities.dto.sort.SortConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated data wrapper
 * Follows the standard defined in RESTFUL.md
 * 
 * @param <T> The type of items in the data list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedData<T> {
    /**
     * List of items for the current page
     */
    private List<T> data;
    
    /**
     * Pagination information
     */
    private Paging<String> page;
    
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
    public static class Paging<TKey> {
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
         * Applied filters (optional) - V2 system with operations between each pair
         */
        private FilterGroupItemV2 filters;
        
        /**
         * Sort configuration (optional)
         */
        @Builder.Default
        private List<SortConfig> sorts = List.of();
        
        /**
         * Selected item IDs (optional)
         */
        private List<TKey> selected;
    }
}
