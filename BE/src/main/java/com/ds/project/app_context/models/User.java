package com.ds.project.app_context.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * User entity for authentication and authorization
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String password;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<UserRoles> userRoles;
    
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    /**
     * Get all roles associated with this user
     */
    public Set<Role> getRoles() {
        if (userRoles == null) {
            return Set.of();
        }
        return userRoles.stream()
            .map(UserRoles::getRole)
            .collect(Collectors.toSet());
    }
    
    /**
     * Add a role to this user
     */
    public void addRole(Role role) {
        if (userRoles == null) {
            userRoles = new java.util.HashSet<>();
        }
        UserRoles userRole = UserRoles.builder()
            .user(this)
            .role(role)
            .createdAt(java.time.LocalDateTime.now())
            .build();
        userRoles.add(userRole);
    }
    
    /**
     * Remove a role from this user
     */
    public void removeRole(Role role) {
        if (userRoles != null) {
            userRoles.removeIf(userRole -> userRole.getRole().equals(role));
        }
    }
    
    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return getRoles().stream()
            .anyMatch(role -> role.getName().equals(roleName) && !role.getDeleted());
    }
}
