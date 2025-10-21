package com.ds.gateway.common.entities.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Filter group DTO for API Gateway
 * Represents a group of filter conditions with AND/OR logic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterGroup {
    
    @JsonProperty("logic")
    private String logic; // "AND" or "OR"
    
    @JsonProperty("conditions")
    private List<Object> conditions; // Can contain FilterCondition or nested FilterGroup
}
