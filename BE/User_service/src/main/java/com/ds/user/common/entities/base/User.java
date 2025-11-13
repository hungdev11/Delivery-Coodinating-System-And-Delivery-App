package com.ds.user.common.entities.base;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"email", "username"}),
        @UniqueConstraint(columnNames = {"username"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private String id; // Keycloak user ID (UUID format as string)

    @PrePersist
    public void prePersist() {
        // ID must be set from Keycloak ID before persisting
        if (id == null) {
            throw new IllegalStateException("User ID must be set from Keycloak ID before persisting");
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        else if (updatedAt == null) {
            updatedAt = now;
        }
    }

    private String firstName;
    private String lastName;

    private String email;
    private String phone;
    private String address;
    private String identityNumber;

    @Column(unique = true, nullable = false)
    private String username;

    @Enumerated(EnumType.ORDINAL)
    @ColumnDefault("1")
    private UserStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdateTimestamps() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * One-to-One relationship with DeliveryMan.
     * A user can optionally be a delivery man (shipper).
     */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private DeliveryMan deliveryMan;

    public enum UserStatus {
        BLOCKED, ACTIVE, PENDING
    }
}
