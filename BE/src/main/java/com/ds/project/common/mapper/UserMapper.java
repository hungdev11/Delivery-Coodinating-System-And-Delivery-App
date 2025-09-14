package com.ds.project.common.mapper;

import com.ds.project.app_context.models.User;
import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.entities.dto.UserDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper for User entity and DTOs
 */
@Component
public class UserMapper {
    
    /**
     * Maps UserRequest to User entity
     */
    public User mapToEntity(UserRequest userRequest) {
        return User.builder()
            .email(userRequest.getEmail())
            .username(userRequest.getUsername())
            .firstName(userRequest.getFirstName())
            .lastName(userRequest.getLastName())
            .password(userRequest.getPassword()) // Add missing password mapping
            .deleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Maps User entity to UserResponse
     */
    public UserDto mapToDto(User user) {
        Set<String> roleNames = Set.of(); 
        
        try {
            if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
                roleNames = user.getUserRoles().stream()
                    .filter(userRole -> userRole.getRole() != null && !userRole.getRole().getDeleted())
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            // Log error but continue with empty roles to prevent StackOverflowError
            org.slf4j.LoggerFactory.getLogger(UserMapper.class)
                .warn("Failed to map user roles for user {}: {}", user.getId(), e.getMessage());
        }
        
        return UserDto.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roles(roleNames)
            .createdAt(user.getCreatedAt().toString())
            .updatedAt(user.getUpdatedAt().toString())
            .build();
    }
}
