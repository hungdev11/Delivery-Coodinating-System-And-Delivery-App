package com.ds.parcel_service.app_context.models;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "assignment_snapshots",
    indexes = {
        @Index(name = "idx_assignment_snapshots_parcel_id", columnList = "parcelId"),
        @Index(name = "idx_assignment_snapshots_session_id", columnList = "sessionId")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSnapshot {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID assignmentId;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID parcelId;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(length = 36)
    private UUID sessionId;

    @Column(length = 36)
    private String deliveryManId;

    @Column(length = 32)
    private String status;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

