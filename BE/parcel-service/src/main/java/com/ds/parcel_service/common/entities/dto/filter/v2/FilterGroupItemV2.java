package com.ds.parcel_service.common.entities.dto.filter.v2;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Nested filter group for V2 system
 * Contains a list of filter items with operators between each pair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("group")
public class FilterGroupItemV2 extends FilterItemV2 {
    /**
     * List of filter items (conditions, operators, or nested groups)
     * Items should alternate between conditions/groups and operators
     * Example: [condition, operator, condition, operator, group]
     */
    private List<FilterItemV2> items;
    
    @Override
    public FilterItemType getType() {
        return FilterItemType.GROUP;
    }
}
