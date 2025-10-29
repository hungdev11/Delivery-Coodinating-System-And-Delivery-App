package com.ds.user.common.entities.common.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Individual filter condition
 * Follows the standard defined in RESTFUL.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterCondition {
    /**
     * Field name to filter on
     */
    private String field;
    
    /**
     * Filter operator
     */
    private FilterOperator operator;
    
    /**
     * Filter value
     */
    private Object value;
    
    /**
     * Case sensitive flag for string operations
     */
    private Boolean caseSensitive;
    
    /**
     * Unique identifier for this condition
     */
    private String id;
}
