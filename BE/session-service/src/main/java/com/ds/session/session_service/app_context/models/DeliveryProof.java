package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import com.ds.session.session_service.common.enums.ProofType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "delivery_proofs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryProof {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, updatable = false)
    private DeliveryAssignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ProofType type;

    /**
     * URL ảnh/video
     */
    @Column(nullable = false, updatable = false)
    private String mediaUrl;

    /**
     * public_id để delete sau này nếu cần
     */
    @Column(nullable = false, updatable = false)
    private String mediaPublicId;

    /**
     * Ai xác nhận (shipperId / system)
     */
    @Column(nullable = false, updatable = false)
    private String confirmedBy;

    /**
     * Thời điểm tạo bằng chứng
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

