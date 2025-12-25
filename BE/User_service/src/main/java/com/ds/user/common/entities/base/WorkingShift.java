package com.ds.user.common.entities.base;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * WorkingShift entity represents a work shift schedule for a delivery man.
 * Default schedule: 8h-18h, Monday-Friday (T2-T6)
 */
@Entity
@Table(name = "working_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WorkingShift {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Many-to-One relationship with DeliveryMan.
     * Multiple shifts can belong to one delivery man (e.g., morning and afternoon shifts).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_man_id", nullable = false, updatable = false)
    private DeliveryMan deliveryMan;

    /**
     * Day of week (1 = Monday, 7 = Sunday)
     * Default: Monday-Friday (1-5)
     */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 1-7 (Monday-Sunday)

    /**
     * Start time of the shift (e.g., 08:00)
     * Default: 08:00
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time of the shift (e.g., 18:00)
     * Default: 18:00
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Maximum session time in hours for this shift.
     * Morning shift: 3.5h, Afternoon shift: 4.5h
     */
    @Column(name = "max_session_time_hours", nullable = false)
    @Builder.Default
    private Double maxSessionTimeHours = 4.0; // Default 4 hours

    /**
     * Whether this shift is active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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

    /**
     * Get DayOfWeek enum from integer
     */
    public DayOfWeek getDayOfWeekEnum() {
        return DayOfWeek.of(dayOfWeek);
    }
}
