package com.ds.user.common.entities.base;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WorkingZone entity represents a zone that a delivery man works in.
 * A delivery man can have up to 5 working zones with priority order.
 */
@Entity
@Table(
    name = "working_zones",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"delivery_man_id", "zone_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WorkingZone {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Many-to-One relationship with DeliveryMan.
     * Multiple working zones belong to one delivery man.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_man_id", nullable = false, updatable = false)
    private DeliveryMan deliveryMan;

    /**
     * Zone ID from zone_service.
     * Reference to the zone this delivery man works in.
     */
    @Column(name = "zone_id", length = 36, nullable = false, updatable = false)
    @JdbcTypeCode(Types.VARCHAR)
    private String zoneId;

    /**
     * Order/priority of this zone for the delivery man (1 = highest priority).
     * Lower number means higher priority.
     * Range: 1-5 (max 5 zones per delivery man)
     */
    @Column(name = "priority_order", nullable = false)
    private Integer order;

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
}
