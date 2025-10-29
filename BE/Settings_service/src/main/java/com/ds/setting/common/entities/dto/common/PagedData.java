package com.ds.setting.common.entities.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paged data response DTO for Settings Service
 * Wraps paginated data with metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedData<T> {
    
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("page")
    private Paging page;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Paging {
        
        @JsonProperty("page")
        private Integer page;
        
        @JsonProperty("size")
        private Integer size;
        
        @JsonProperty("totalElements")
        private Long totalElements;
        
        @JsonProperty("totalPages")
        private Integer totalPages;
        
        @JsonProperty("filters")
        private FilterGroup filters;
        
        @JsonProperty("sorts")
        private List<SortConfig> sorts;
        
        @JsonProperty("selected")
        private List<String> selected;
    }
}
