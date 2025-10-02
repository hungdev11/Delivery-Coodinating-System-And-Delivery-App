package com.ds.user.common.entities.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncUserRequest {

    @NotBlank
    private String keycloakId;

    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
