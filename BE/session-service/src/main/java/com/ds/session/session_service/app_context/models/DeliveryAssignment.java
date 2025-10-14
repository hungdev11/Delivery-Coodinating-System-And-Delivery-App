package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ds.session.session_service.common.enums.AssignmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "delivery_assignments", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"delivery_man_id", "parcel_id", "scaned_at"})})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    // =====================================
    @Column(name = "delivery_man_id", nullable = false, updatable = false)
    private String deliveryManId;

    @Column(name = "parcel_id", nullable = false, updatable = false)
    private String parcelId;

    @Column(name = "scaned_at", nullable = false)
    private LocalDateTime scanedAt;

    private double distanceM;
    private long durationS;

    @Column(name = "fail_reason")
    private String failReason;

    @Column(columnDefinition = "json")
    private String waypoints;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
