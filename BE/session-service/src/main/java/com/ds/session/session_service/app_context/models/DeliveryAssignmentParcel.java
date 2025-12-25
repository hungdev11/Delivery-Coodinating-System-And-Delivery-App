package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction table for DeliveryAssignment and Parcel (1-n relationship).
 * An assignment can contain multiple parcels that share the same delivery address.
 */
@Entity
@Table(
    name = "delivery_assignment_parcels",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"assignment_id", "parcel_id"})
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignmentParcel {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Many-to-One relationship with DeliveryAssignment.
     * Multiple parcels belong to one assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false, updatable = false)
    private DeliveryAssignment assignment;

    /**
     * Parcel ID (stored as String to match Parcel.id type)
     */
    @Column(name = "parcel_id", length = 36, nullable = false, updatable = false)
    @JdbcTypeCode(Types.VARCHAR)
    private String parcelId;
}
