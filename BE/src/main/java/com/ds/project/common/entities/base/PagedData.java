package com.ds.project.common.entities.base;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedData<TPage extends Page, TData> {
    private TPage page;
    private List<TData> data;
}
