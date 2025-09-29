package ptithcm.graduation.apigateway.models;

public class Paging {
    public int currentPage = 1;
    public int pageSize = 10;
    public int totalItems = 0;
    public int totalPages = 0;

    public Paging() {}

    public Paging(int currentPage, int pageSize) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }
}
