package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BaseLogoutRequestDto {
    private String refreshToken;

    // Default constructor
    public BaseLogoutRequestDto() {}

    // All args constructor
    public BaseLogoutRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
