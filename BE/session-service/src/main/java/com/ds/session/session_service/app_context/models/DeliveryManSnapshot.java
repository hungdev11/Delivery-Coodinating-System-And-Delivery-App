package com.ds.session.session_service.app_context.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * DeliveryMan snapshot for caching delivery man information locally
 * Updated via Kafka events from User service
 */
@Entity
@Table(name = "delivery_man_snapshots", indexes = {
    @Index(name = "idx_delivery_man_snapshots_user_id", columnList = "userId")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryManSnapshot {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String userId; // Keycloak ID

    // User information
    @Column(nullable = false)
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // DeliveryMan specific fields
    private String vehicleType;
    private Double capacityKg;
    private Boolean enabled;

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
        return "DeliveryMan " + (userId != null ? userId.substring(0, Math.min(4, userId.length())) : "Unknown");
    }
}
