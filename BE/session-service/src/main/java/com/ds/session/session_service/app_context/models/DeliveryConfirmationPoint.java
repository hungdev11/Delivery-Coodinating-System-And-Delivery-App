package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_confirmation_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryConfirmationPoint {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "assignment_id", nullable = false, length = 36)
    private UUID assignmentId; // FK to delivery_assignments

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId; // FK to delivery_sessions

    @Column(nullable = false)
    private Double latitude; // Vị trí shipper khi xác nhận

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Column(name = "confirmation_type", nullable = false, length = 20)
    private String confirmationType; // DELIVERED, RETURNED, FAILED

    @Column(name = "distance_from_parcel")
    private Double distanceFromParcel; // Khoảng cách từ vị trí shipper đến địa chỉ giao
}
