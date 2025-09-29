package ptithcm.graduation.apigateway.models;

import java.util.List;

public class CustomPagedResult<TItem, TPage extends Paging> {
    public TPage paging;
    public List<TItem> items;

    public CustomPagedResult(List<TItem> items, TPage paging) {
        this.items = items;
        this.paging = paging;
    }
}
