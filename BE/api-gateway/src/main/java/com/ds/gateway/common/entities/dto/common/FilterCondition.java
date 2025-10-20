package com.ds.gateway.common.entities.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter condition DTO for API Gateway
 * Represents a single filter condition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCondition {
    
    @JsonProperty("field")
    private String field;
    
    @JsonProperty("operator")
    private String operator;
    
    @JsonProperty("value")
    private Object value;
    
    @JsonProperty("caseSensitive")
    private Boolean caseSensitive;
    
    @JsonProperty("id")
    private String id;
}
