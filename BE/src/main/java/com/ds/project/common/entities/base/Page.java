package com.ds.project.common.entities.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Page {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String filters;
    private String sorts;

    public Page(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public Page(int page, int size, long totalElements, int totalPages) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
}
