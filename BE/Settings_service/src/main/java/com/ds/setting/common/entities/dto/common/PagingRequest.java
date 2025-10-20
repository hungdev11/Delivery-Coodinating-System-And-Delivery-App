package com.ds.setting.common.entities.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagingRequest {
    
    @JsonProperty("filters")
    @Builder.Default
    private Optional<FilterGroup> filters = Optional.empty();
    
    @JsonProperty("sorts")
    @Builder.Default
    private Optional<List<SortConfig>> sorts = Optional.empty();
    
    @JsonProperty("page")
    @Builder.Default
    private Optional<Integer> page = Optional.empty();
    
    @JsonProperty("size")
    @Builder.Default
    private Optional<Integer> size = Optional.empty();
    
    @JsonProperty("search")
    @Builder.Default
    private Optional<String> search = Optional.empty();
    
    @JsonProperty("selected")
    @Builder.Default
    private Optional<List<String>> selected = Optional.empty();
    
    public FilterGroup getFiltersOrEmpty() {
        return filters.orElse(FilterGroup.builder().logic("AND").conditions(List.of()).build());
    }
    
    public List<SortConfig> getSortsOrEmpty() {
        return sorts.orElse(List.of());
    }
    
    public int getPageOrDefault() {
        return page.orElse(0);
    }
    
    public int getSizeOrDefault() {
        return size.orElse(10);
    }
    
    public String getSearchOrEmpty() {
        return search.orElse("");
    }
    
    public List<String> getSelectedOrEmpty() {
        return selected.orElse(List.of());
    }
}
