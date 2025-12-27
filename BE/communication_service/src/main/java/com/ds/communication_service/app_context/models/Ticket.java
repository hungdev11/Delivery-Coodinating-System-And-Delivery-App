package com.ds.communication_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Ticket entity for reporting and managing delivery issues
 * Links to Parcel, DeliveryAssignment, and User (client/shipper)
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
public class Ticket {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Ticket type: DELIVERY_FAILED (shipper reports) or NOT_RECEIVED (client reports)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TicketType type;

    /**
     * Ticket status: OPEN, IN_PROGRESS, RESOLVED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TicketStatus status = TicketStatus.OPEN;

    /**
     * Parcel ID (from parcel-service)
     */
    @Column(name = "parcel_id", nullable = false, length = 36)
    private String parcelId;

    /**
     * Delivery Assignment ID (from session-service)
     * Can be null if ticket created before assignment
     */
    @Column(name = "assignment_id", length = 36)
    private String assignmentId;

    /**
     * User who created the ticket (client or shipper)
     */
    @Column(name = "reporter_id", nullable = false)
    private String reporterId;

    /**
     * Admin who is handling the ticket
     * Null if not assigned to admin yet
     */
    @Column(name = "assigned_admin_id")
    private String assignedAdminId;

    /**
     * Description/reason for the ticket
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Admin's resolution notes
     */
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * Action taken: REASSIGN, CANCEL, RESOLVE
     */
    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Resolved timestamp
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
