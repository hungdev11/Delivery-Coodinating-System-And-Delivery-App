package com.ds.project.common.entities.dto.response;

import com.ds.project.common.entities.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Login response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType;
    private long expiresIn;
    private UserDto user;
    private Set<String> roles;
}
