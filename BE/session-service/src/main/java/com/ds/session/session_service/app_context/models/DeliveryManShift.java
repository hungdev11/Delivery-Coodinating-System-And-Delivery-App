package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(
    name = "delivery_man_shift", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"delivery_man_id", "shift_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryManShift {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "delivery_man_id", nullable = false, updatable = false)
    private String deliveryManId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;
    
    @Column(name = "is_active")
    private boolean isActive;
}
