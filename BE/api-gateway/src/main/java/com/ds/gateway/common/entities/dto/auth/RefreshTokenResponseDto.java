package com.ds.gateway.common.entities.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh token response DTO with new tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponseDto {
    private String message;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
}
