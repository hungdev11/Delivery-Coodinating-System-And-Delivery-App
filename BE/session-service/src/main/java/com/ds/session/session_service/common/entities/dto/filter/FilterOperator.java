package com.ds.session.session_service.common.entities.dto.filter;

/**
 * Enumeration of supported filter operators
 */
public enum FilterOperator {
    // Basic comparison operators
    EQUALS("equals", "Equal to"),
    NOT_EQUALS("not_equals", "Not equal to"),
    
    // String operators
    CONTAINS("contains", "Contains"),
    STARTS_WITH("starts_with", "Starts with"),
    ENDS_WITH("ends_with", "Ends with"),
    REGEX("regex", "Regular expression match"),
    
    // Numeric operators
    GREATER_THAN("gt", "Greater than"),
    GREATER_THAN_OR_EQUAL("gte", "Greater than or equal to"),
    LESS_THAN("lt", "Less than"),
    LESS_THAN_OR_EQUAL("lte", "Less than or equal to"),
    BETWEEN("between", "Between two values"),
    
    // Collection operators
    IN("in", "In list"),
    NOT_IN("not_in", "Not in list"),
    
    // Null operators
    IS_NULL("is_null", "Is null"),
    IS_NOT_NULL("is_not_null", "Is not null"),
    
    // Array operators
    ARRAY_CONTAINS("array_contains", "Array contains"),
    ARRAY_NOT_CONTAINS("array_not_contains", "Array does not contain"),
    ARRAY_OVERLAPS("array_overlaps", "Array overlaps"),
    ARRAY_CONTAINS_ALL("array_contains_all", "Array contains all"),
    ARRAY_CONTAINS_ANY("array_contains_any", "Array contains any");

    private final String value;
    private final String description;

    FilterOperator(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get FilterOperator by value
     * 
     * @param value String value
     * @return FilterOperator or null if not found
     */
    public static FilterOperator fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (FilterOperator operator : values()) {
            if (operator.getValue().equals(value)) {
                return operator;
            }
        }
        return null;
    }

    /**
     * Check if operator is for string fields
     * 
     * @return true if string operator
     */
    public boolean isStringOperator() {
        return this == CONTAINS || this == STARTS_WITH || this == ENDS_WITH || this == REGEX;
    }

    /**
     * Check if operator is for numeric fields
     * 
     * @return true if numeric operator
     */
    public boolean isNumericOperator() {
        return this == GREATER_THAN || this == GREATER_THAN_OR_EQUAL || 
               this == LESS_THAN || this == LESS_THAN_OR_EQUAL || this == BETWEEN;
    }

    /**
     * Check if operator is for collection fields
     * 
     * @return true if collection operator
     */
    public boolean isCollectionOperator() {
        return this == IN || this == NOT_IN || this == ARRAY_CONTAINS || 
               this == ARRAY_NOT_CONTAINS || this == ARRAY_OVERLAPS || 
               this == ARRAY_CONTAINS_ALL || this == ARRAY_CONTAINS_ANY;
    }

    /**
     * Check if operator is for null checking
     * 
     * @return true if null operator
     */
    public boolean isNullOperator() {
        return this == IS_NULL || this == IS_NOT_NULL;
    }

    @Override
    public String toString() {
        return value;
    }
}
