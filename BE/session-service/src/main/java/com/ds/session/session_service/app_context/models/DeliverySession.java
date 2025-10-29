package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // Thêm import
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import com.ds.session.session_service.common.enums.SessionStatus;

import jakarta.persistence.CascadeType; // Thêm import
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany; // Thêm import
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverySession {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String deliveryManId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startTime; // Được đặt khi shipper bắt đầu phiên (scan đơn)

    private LocalDateTime endTime; // Được đặt khi phiên kết thúc

    // --- BỔ SUNG MỐI QUAN HỆ HAI CHIỀU ---
    /**
     * Danh sách các lượt giao hàng (task) thuộc phiên này.
     * - mappedBy = "session": Báo cho JPA biết rằng mối quan hệ này 
     * được quản lý bởi trường "session" bên class DeliveryAssignment.
     * - cascade = CascadeType.ALL: Tự động lưu/cập nhật/xóa các Assignment
     * khi Session được lưu/cập nhật/xóa.
     * - orphanRemoval = true: Tự động xóa một Assignment khỏi DB
     * nếu nó bị xóa khỏi danh sách 'assignments' này.
     */
    @OneToMany(
        mappedBy = "session",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY 
    )
    @Builder.Default 
    private List<DeliveryAssignment> assignments = new ArrayList<>();

    public void addAssignment(DeliveryAssignment assignment) {
        assignments.add(assignment);
        assignment.setSession(this);
    }
}