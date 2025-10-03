package com.ds.gateway.common.entities.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a new user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDto {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private List<String> roles;
}
