package ptithcm.graduation.apigateway.models;

import java.util.List;

public class PagedResult<TItem> {
    public Paging paging;
    public List<TItem> items;

    public PagedResult(List<TItem> items, Paging paging) {
        this.items = items;
        this.paging = paging;
    }
}
