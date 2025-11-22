package com.ds.parcel_service.app_context.models;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;

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
import lombok.*;

@Entity
@Table(name = "parcels",uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcel {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String senderId;
        
    @Column(nullable = false)
    private String receiverId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;

    @Column(nullable = false)
    private String receiveFrom;

    @Column(nullable = false)
    private String sendTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParcelStatus status;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false)
    private BigDecimal value;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    @Column(length = 512)
    private String confirmationNote;
    private LocalTime windowStart;
    private LocalTime windowEnd;
    
    // Priority for delivery routing (higher = more urgent)
    // Maps to DeliveryType priorities: URGENT=10, EXPRESS=4, FAST=3, NORMAL=2, ECONOMY=1
    @Column(name = "priority")
    private Integer priority;
    
    // Delay/postpone flag - when true, parcel is temporarily hidden from routing
    @Column(name = "is_delayed", nullable = false)
    @Builder.Default
    private Boolean isDelayed = false;
    
    // When parcel should be available for routing again
    @Column(name = "delayed_until")
    private LocalDateTime delayedUntil;
}
