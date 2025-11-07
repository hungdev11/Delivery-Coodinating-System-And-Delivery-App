package com.ds.session.session_service.common.entities.dto.filter.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Logical operator item for V2 system
 * Represents AND/OR between conditions/groups
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterOperatorItemV2 extends FilterItemV2 {
    /**
     * Logical operator value: "AND" or "OR"
     */
    private String value;
    
    @Override
    public FilterItemType getType() {
        return FilterItemType.OPERATOR;
    }
}
