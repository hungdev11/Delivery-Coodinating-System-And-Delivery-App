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
 * LeaveRequest entity represents a leave/absence request from a delivery man.
 * Used to mark when a delivery man cannot work during a shift period.
 */
@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class LeaveRequest {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Many-to-One relationship with DeliveryMan.
     * Multiple leave requests can belong to one delivery man.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_man_id", nullable = false, updatable = false)
    private DeliveryMan deliveryMan;

    /**
     * Optional reference to WorkingShift.
     * If null, the leave applies to all shifts during the time period.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = true)
    private WorkingShift shift;

    /**
     * Start time of the leave period
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * End time of the leave period
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * Reason for the leave
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Status of the leave request
     * PENDING, APPROVED, REJECTED
     */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

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
