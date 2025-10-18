package com.ds.user.common.entities.dto.common;

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
    
}
