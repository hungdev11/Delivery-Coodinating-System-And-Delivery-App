package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BaseUserResponseDto {
    private String message;
    private BaseUserDto user;

    // Default constructor
    public BaseUserResponseDto() {}

    // All args constructor
    public BaseUserResponseDto(String message, BaseUserDto user) {
        this.message = message;
        this.user = user;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BaseUserDto getUser() { return user; }
    public void setUser(BaseUserDto user) { this.user = user; }
}
