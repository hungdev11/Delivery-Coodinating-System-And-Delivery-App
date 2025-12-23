package com.ds.session.session_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shipper_location_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperLocationTracking {
    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId; // FK to delivery_sessions

    @Column(name = "delivery_man_id", nullable = false)
    private String deliveryManId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private Double accuracy; // GPS accuracy in meters

    @Column
    private Double speed; // Optional: speed in m/s

    @Column(name = "nearest_node_id", length = 36)
    private String nearestNodeId; // FK to zone_service.road_nodes (optional)

    @Column(name = "distance_to_node")
    private Double distanceToNode; // Distance to nearest node in meters
}
