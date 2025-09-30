package com.ds.user.common.entities.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a user from external identity provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalUserDto {
    private String id;
    private String username;
    private String email;
    private Boolean emailVerified;
    private Boolean enabled;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Map<String, Object> attributes;
    private Long createdTimestamp;
}
