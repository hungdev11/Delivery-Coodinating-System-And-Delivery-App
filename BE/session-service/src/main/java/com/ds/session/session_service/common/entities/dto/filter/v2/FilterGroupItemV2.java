package com.ds.session.session_service.common.entities.dto.filter.v2;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Nested filter group for V2 system
 * Contains a list of filter items with operators between each pair
 * 
 * Supports both old format (logic + conditions) and new format (type + items)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("group")
@JsonDeserialize(using = FilterGroupItemV2Deserializer.class)
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
