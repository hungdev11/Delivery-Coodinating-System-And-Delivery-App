package com.ds.session.session_service.common.entities.dto.sort;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sort configuration
 * Follows the standard defined in RESTFUL.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortConfig {
    /**
     * Field name to sort by
     */
    private String field;
    
    /**
     * Sort direction: asc or desc
     */
    private String direction;
}
