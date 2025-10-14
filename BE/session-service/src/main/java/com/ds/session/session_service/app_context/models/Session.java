package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "sessions", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"delivery_man_id", "task_id", "assigned_at"})})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    // =====================================
    @Column(name = "delivery_man_id", nullable = false, updatable = false)
    private String deliveryManId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    private double distanceM;
    private long durationS;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "fail_reason")
    private String failReason;

    private LocalTime windowStart;
    private LocalTime windowEnd;

    @Column(columnDefinition = "json")
    private String waypoints;


    @PrePersist
    @PreUpdate
    private void validateWindow() {
        if (windowStart != null && windowEnd != null && !windowStart.isBefore(windowEnd)) {
            throw new IllegalArgumentException("windowStart must be before windowEnd");
        }
    }
    // =====================================

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
