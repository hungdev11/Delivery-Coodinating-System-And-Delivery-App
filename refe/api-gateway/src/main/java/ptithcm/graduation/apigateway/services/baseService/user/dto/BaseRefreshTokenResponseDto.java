package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BaseRefreshTokenResponseDto {
    private String message;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;

    // Default constructor
    public BaseRefreshTokenResponseDto() {}

    // All args constructor
    public BaseRefreshTokenResponseDto(String message, String accessToken, String refreshToken, 
                                      String tokenType, Integer expiresIn) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Integer getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
}
