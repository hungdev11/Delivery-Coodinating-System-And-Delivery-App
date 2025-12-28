package com.ds.communication_service.app_context.models;

import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ticket entity for managing delivery issues
 * Links with Parcel, DeliveryAssignment, and User (client/shipper)
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_tickets_parcel_id", columnList = "parcel_id"),
    @Index(name = "idx_tickets_assignment_id", columnList = "delivery_assignment_id"),
    @Index(name = "idx_tickets_user_id", columnList = "user_id"),
    @Index(name = "idx_tickets_status", columnList = "status"),
    @Index(name = "idx_tickets_created_at", columnList = "created_at")
})
@Getter
@Setter
public class Ticket {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Reference to parcel-service.parcels.id
     */
    @Column(name = "parcel_id", nullable = false, length = 36)
    private String parcelId;

    /**
     * Reference to session-service.delivery_assignments.id
     * Optional - may not exist if ticket is created before assignment
     */
    @Column(name = "delivery_assignment_id", length = 36)
    private String deliveryAssignmentId;

    /**
     * Reference to user-service.users.id
     * Can be either client (for NOT_RECEIVED) or shipper (for DELIVERY_FAILED)
     */
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TicketStatus status = TicketStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * When ticket was resolved
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * Admin user ID who resolved the ticket
     * Reference to user-service.users.id
     */
    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    /**
     * Additional notes from admin when resolving
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
}
