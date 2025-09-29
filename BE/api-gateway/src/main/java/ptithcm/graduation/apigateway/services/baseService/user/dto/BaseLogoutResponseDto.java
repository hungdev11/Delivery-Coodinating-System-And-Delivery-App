package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BaseLogoutResponseDto {
    private String message;

    // Default constructor
    public BaseLogoutResponseDto() {}

    // All args constructor
    public BaseLogoutResponseDto(String message) {
        this.message = message;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
