package com.ds.parcel_service.common.entities.dto.filter.v2;

import com.ds.parcel_service.common.entities.dto.filter.FilterOperator;
import com.ds.parcel_service.common.entities.dto.filter.FilterOperatorDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Filter condition item for V2 system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterConditionItemV2 extends FilterItemV2 {
    /**
     * Field name to filter on
     */
    private String field;
    
    /**
     * Filter operator
     */
    @JsonDeserialize(using = FilterOperatorDeserializer.class)
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
    
    @Override
    public FilterItemType getType() {
        return FilterItemType.CONDITION;
    }
}
