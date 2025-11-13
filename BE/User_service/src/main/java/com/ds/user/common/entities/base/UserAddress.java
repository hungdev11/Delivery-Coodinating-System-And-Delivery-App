package com.ds.user.common.entities.base;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_addresses",
    indexes = {
        @Index(name = "idx_user_addresses_user_id", columnList = "user_id"),
        @Index(name = "idx_user_addresses_destination_id", columnList = "destination_id"),
        @Index(name = "idx_user_addresses_is_primary", columnList = "user_id, is_primary")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserAddress {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false)
    @JdbcTypeCode(Types.VARCHAR)
    private String userId;

    @Column(name = "destination_id", length = 36, nullable = false)
    @JdbcTypeCode(Types.VARCHAR)
    private String destinationId; // Reference to zone_service addresses table

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "tag", length = 50)
    private String tag; // e.g., "Home", "Work", "Other"

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdateTimestamps() {
        updatedAt = LocalDateTime.now();
    }

    // Many-to-One relationship with User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}
