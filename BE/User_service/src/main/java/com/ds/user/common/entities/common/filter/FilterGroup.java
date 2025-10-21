package com.ds.user.common.entities.common.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filter group with AND/OR logic
 * Follows the standard defined in RESTFUL.md
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterGroup {
    /**
     * Logic operator: AND or OR
     */
    private String logic; // "AND" or "OR"
    
    /**
     * List of conditions (can be FilterCondition or nested FilterGroup)
     */
    private List<Object> conditions;
}
