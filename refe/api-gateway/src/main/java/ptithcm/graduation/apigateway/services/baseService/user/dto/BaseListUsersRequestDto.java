package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BaseListUsersRequestDto {
    private Integer page;
    private Integer size;

    // Default constructor
    public BaseListUsersRequestDto() {}

    // All args constructor
    public BaseListUsersRequestDto(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }

    // Getters and Setters
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
