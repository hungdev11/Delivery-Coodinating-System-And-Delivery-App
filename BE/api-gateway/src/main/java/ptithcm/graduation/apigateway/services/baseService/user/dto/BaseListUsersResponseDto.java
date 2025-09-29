package ptithcm.graduation.apigateway.services.baseService.user.dto;

import java.util.List;

public class BaseListUsersResponseDto {
    private String message;
    private List<BaseUserDto> users;
    private Integer total;

    // Default constructor
    public BaseListUsersResponseDto() {}

    // All args constructor
    public BaseListUsersResponseDto(String message, List<BaseUserDto> users, Integer total) {
        this.message = message;
        this.users = users;
        this.total = total;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<BaseUserDto> getUsers() { return users; }
    public void setUsers(List<BaseUserDto> users) { this.users = users; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
}
