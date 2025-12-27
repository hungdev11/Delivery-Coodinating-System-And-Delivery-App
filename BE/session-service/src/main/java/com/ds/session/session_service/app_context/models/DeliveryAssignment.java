package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.expression.spel.ast.Assign;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    name = "delivery_assignments"
    //uniqueConstraints = {@UniqueConstraint(columnNames = {"session_id", "parcel_id"})}
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
    @JoinColumn(name = "session_id")
    private DeliverySession session;

    /**
     * @deprecated Use parcels relationship instead. Kept for backward compatibility.
     * Will be removed in future version.
     */
    @Deprecated
    @Column(name = "parcel_id", nullable = true, updatable = false)
    private String parcelId;

    /**
     * Delivery address ID (UserAddress ID) - all parcels in this assignment share this delivery address
     */
    @Column(name = "delivery_address_id", length = 36, nullable = false, updatable = false)
    @JdbcTypeCode(Types.VARCHAR)
    private String deliveryAddressId;

    @Column(name = "shipper_id", nullable = false, updatable = false)
    private String shipperId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "scaned_at")
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

    /**
     * One-to-Many relationship with DeliveryAssignmentParcel.
     * An assignment can contain multiple parcels that share the same delivery address.
     */
    @OneToMany(
        mappedBy = "assignment",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<DeliveryAssignmentParcel> parcels = new ArrayList<>();

    @OneToMany(
        mappedBy = "assignment",
        fetch = FetchType.LAZY,
        cascade = CascadeType.PERSIST
    )
    private List<DeliveryProof> proofs = new ArrayList<>();

    // Thời gian hết hạn để shipper phản hồi (accept/reject)
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    // Thời gian chờ trước khi có thể assign đơn cho shipper này lại (tính bằng phút)
    private int expiredCooldown;

    // helper method (rất quan trọng)
    public void addProof(DeliveryProof proof) {
        if (!inExecuteStatus()) {
            throw new IllegalArgumentException("Cannot add proof when assignment is not in execute status");
        }
        proofs.add(proof);
        proof.setAssignment(this);
    }

    private boolean inExecuteStatus() {
        return List.of(
            AssignmentStatus.IN_PROGRESS,
            AssignmentStatus.COMPLETED,
            AssignmentStatus.FAILED)
            .contains(this.status);
    }

    /**
     * Accept task - called when shipper accepts the assignment
     * Status transition: PENDING -> ACCEPTED (or ASSIGNED -> ACCEPTED for backward compatibility)
     */
    public void acceptTask() {
        if (this.status != AssignmentStatus.PENDING && this.status != AssignmentStatus.ASSIGNED) {
            throw new IllegalArgumentException("Only assignments in PENDING or ASSIGNED status can be accepted. Current status: " + this.status);
        }
        this.status = AssignmentStatus.ACCEPTED;
        this.scanedAt = LocalDateTime.now();
    }

    /**
     * Start task - called when shipper starts the delivery session
     * Status transition: ACCEPTED -> IN_PROGRESS
     */
    public void startTask(DeliverySession session) {
        if (this.status != AssignmentStatus.ACCEPTED) {
            throw new IllegalArgumentException("Only assignments in ACCEPTED status can be started. Current status: " + this.status);
        }
        this.status = AssignmentStatus.IN_PROGRESS;
        this.session = session;
    }

    /**
     * Helper method to add a parcel to this assignment
     */
    public void addParcel(DeliveryAssignmentParcel parcel) {
        if (parcels == null) {
            parcels = new ArrayList<>();
        }
        parcels.add(parcel);
        parcel.setAssignment(this);
    }
}
