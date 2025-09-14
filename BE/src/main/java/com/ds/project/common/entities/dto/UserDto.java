package com.ds.project.common.entities.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * User Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Boolean deleted;
    private String createdAt;
    private String updatedAt;
}
