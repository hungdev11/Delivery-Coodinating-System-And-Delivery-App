package com.ds.user.common.entities.common.filter.v2;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for V2 filter items
 * V2 allows operations between each pair of conditions/groups
 */
@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = FilterConditionItemV2.class, name = "condition"),
    @JsonSubTypes.Type(value = FilterOperatorItemV2.class, name = "operator"),
    @JsonSubTypes.Type(value = FilterGroupItemV2.class, name = "group")
})
public abstract class FilterItemV2 {
    private FilterItemType type;
}
