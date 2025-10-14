package com.ds.user.common.entities.dto;

import com.ds.user.app_context.models.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    
    @Email(message = "Email must be valid")
    private String email;
    
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private User.UserStatus status;
}
