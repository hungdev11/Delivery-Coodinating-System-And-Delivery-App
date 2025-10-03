package com.ds.gateway.common.entities.dto.user;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {
    private String username;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private List<String> roles;
}
