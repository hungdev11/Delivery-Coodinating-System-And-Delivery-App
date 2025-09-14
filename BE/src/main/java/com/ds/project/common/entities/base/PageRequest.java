package com.ds.project.common.entities.base;

import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest<TKey> extends Page{
    private Optional<List<TKey>> selected;
}
