package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ds.session.session_service.common.enums.AssignmentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn; // Thêm import
import jakarta.persistence.ManyToOne; // Thêm import
import jakarta.persistence.OneToMany;
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
    uniqueConstraints = {@UniqueConstraint(columnNames = {"session_id", "parcel_id"})}
)
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
    
    /**
     * Phiên giao hàng mà lượt giao này thuộc về.
     * - @ManyToOne: Nhiều Assignment thuộc 1 Session.
     * - fetch = FetchType.LAZY: Chỉ tải thông tin Session khi
     * ta gọi assignment.getSession().
     * - @JoinColumn: Chỉ định tên cột khóa ngoại trong DB
     * là "session_id".
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private DeliverySession session;

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

    @OneToMany(
        mappedBy = "assignment",
        fetch = FetchType.LAZY,
        cascade = CascadeType.PERSIST
    )
    private List<DeliveryProof> proofs = new ArrayList<>();

    // helper method (rất quan trọng)
    public void addProof(DeliveryProof proof) {
        proofs.add(proof);
        proof.setAssignment(this);
    }
}
