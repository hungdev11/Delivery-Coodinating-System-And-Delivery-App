package com.ds.communication_service.common.dto.filter;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of supported filter operators
 */
public enum FilterOperator {
    // Basic comparison operators
    EQUALS,
    NOT_EQUALS,
    
    // String operators
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    REGEX,
    
    // Numeric operators
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    BETWEEN,
    
    // Collection operators
    IN,
    NOT_IN,
    
    // Null operators
    IS_NULL,
    IS_NOT_NULL,
    
    // Array operators
    CONTAINS_ANY,
    CONTAINS_ALL,
    IS_EMPTY,
    IS_NOT_EMPTY;

    /**
     * Deserialize from JSON string (case-insensitive)
     */
    @JsonCreator
    public static FilterOperator fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String normalized = value.trim().toUpperCase().replace("-", "_");
        try {
            return FilterOperator.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown FilterOperator value: " + value);
        }
    }
}
