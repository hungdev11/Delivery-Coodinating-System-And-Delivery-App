package com.ds.project.common.entities.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User payload for JWT token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPayload {
    
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private long iat;
    private long exp;
}
