package com.ds.setting.common.entities.dto.filter.v2;

/**
 * Type of filter item in V2 filter system
 */
public enum FilterItemType {
    CONDITION,  // A filter condition (field, operator, value)
    OPERATOR,   // A logical operator (AND/OR) between conditions
    GROUP       // A nested group of filter items
}
