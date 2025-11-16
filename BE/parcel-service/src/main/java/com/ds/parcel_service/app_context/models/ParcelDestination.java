package com.ds.parcel_service.app_context.models;

import java.sql.Types;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.ds.parcel_service.common.enums.DestinationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "parcel_destinations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelDestination {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;

    @Column(nullable = false)
    private String destinationId;

    @Column(nullable = false)
    private DestinationType destinationType;

    @Column(nullable = false)
    private boolean isCurrent;

    @Column(nullable = false)
    private boolean isOriginal;
    
    /**
     * Business logic: Ensure only one destination is current per parcel.
     * This should be enforced at the service layer.
     */
    public void setCurrent(boolean current) {
        this.isCurrent = current;
    }
}
