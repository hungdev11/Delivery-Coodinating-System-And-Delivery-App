package com.ds.parcel_service.common.entities.dto.filter.v2;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Type of filter item in V2 filter system
 */
public enum FilterItemType {
    CONDITION,  // A filter condition (field, operator, value)
    OPERATOR,   // A logical operator (AND/OR) between conditions
    GROUP;      // A nested group of filter items
    
    /**
     * Deserialize from JSON string (case-insensitive)
     * Handles both "group" (lowercase) and "GROUP" (uppercase)
     */
    @JsonCreator
    public static FilterItemType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String normalized = value.trim().toUpperCase();
        try {
            return FilterItemType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown FilterItemType value: " + value + ". Expected one of: CONDITION, OPERATOR, GROUP");
        }
    }
}
