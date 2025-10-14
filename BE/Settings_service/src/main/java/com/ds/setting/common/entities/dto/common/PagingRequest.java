package com.ds.setting.common.entities.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Paging request parameters for query strings
 * Follows the standard defined in RESTFUL.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
     * Applied filters (optional)
     */
    private List<Object> filters;
    
    /**
     * Sort configuration (optional)
     */
    private List<Object> sorts;
    
    /**
     * Selected item IDs (optional)
     */
    private List<String> selected;
    
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
}
