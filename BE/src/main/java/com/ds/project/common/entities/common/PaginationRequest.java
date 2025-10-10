package com.ds.project.common.entities.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common pagination request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 10;
    
    private String sortBy;
    private String sortDirection;
    
    public int getOffset() {
        return page * size;
    }
}
