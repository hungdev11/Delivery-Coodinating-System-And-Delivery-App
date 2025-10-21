package com.ds.gateway.common.entities.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sort configuration DTO for API Gateway
 * Represents a single sort configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortConfig {
    
    @JsonProperty("field")
    private String field;
    
    @JsonProperty("direction")
    private String direction; // "asc" or "desc"
    
    @JsonProperty("priority")
    private Integer priority;
}
