package com.ds.user.common.entities.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated data wrapper
 * Follows the standard defined in RESTFUL.md
 * 
 * @param <T> The type of items in the data list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedData<T> {
    /**
     * List of items for the current page
     */
    private List<T> data;
    
    /**
     * Pagination information
     */
    private Paging<String> page;
    
    /**
     * Create PagedData from Spring Data Page
     */
    public static <T> PagedData<T> of(org.springframework.data.domain.Page<T> springPage) {
        return PagedData.<T>builder()
                .data(springPage.getContent())
                .page(Paging.<String>builder()
                        .page(springPage.getNumber())
                        .size(springPage.getSize())
                        .totalElements(springPage.getTotalElements())
                        .totalPages(springPage.getTotalPages())
                        .filters(List.of())
                        .sorts(List.of())
                        .build())
                .build();
    }
}
