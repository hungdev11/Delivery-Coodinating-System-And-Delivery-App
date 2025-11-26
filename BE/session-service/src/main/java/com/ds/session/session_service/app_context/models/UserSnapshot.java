package com.ds.session.session_service.app_context.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * User snapshot for caching user information locally
 * Updated via Kafka events from User service
 */
@Entity
@Table(name = "user_snapshots", indexes = {
    @Index(name = "idx_user_snapshots_user_id", columnList = "userId"),
    @Index(name = "idx_user_snapshots_username", columnList = "username")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSnapshot {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String userId; // Keycloak ID

    @Column(nullable = false)
    private String username;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String identityNumber;
    private String status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Get full name (firstName + lastName)
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else if (username != null) {
            return username;
        }
        return "User " + (userId != null ? userId.substring(0, Math.min(4, userId.length())) : "Unknown");
    }
}
