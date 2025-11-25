package com.ds.session.session_service.common.entities.dto.filter.v2;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for V2 filter items
 * V2 allows operations between each pair of conditions/groups
 */
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = FilterGroupItemV2.class, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FilterConditionItemV2.class, name = "condition"),
    @JsonSubTypes.Type(value = FilterOperatorItemV2.class, name = "operator"),
    @JsonSubTypes.Type(value = FilterGroupItemV2.class, name = "group")
})
public abstract class FilterItemV2 {
    private FilterItemType type;
    
    /**
     * Get the type of this filter item
     * Subclasses should override this to return their specific type
     */
    public abstract FilterItemType getType();
    
    /**
     * Set the type (for Jackson deserialization)
     */
    public void setType(FilterItemType type) {
        this.type = type;
    }
}
