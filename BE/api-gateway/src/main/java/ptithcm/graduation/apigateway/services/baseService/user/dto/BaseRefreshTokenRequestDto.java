package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BaseRefreshTokenRequestDto {
    private String refreshToken;

    // Default constructor
    public BaseRefreshTokenRequestDto() {}

    // All args constructor
    public BaseRefreshTokenRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
