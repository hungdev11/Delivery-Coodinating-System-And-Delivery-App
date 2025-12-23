package com.ds.session.session_service.business.v1.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ds.session.session_service.app_context.models.ShipperLocationTracking;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.app_context.repositories.ShipperLocationTrackingRepository;
import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.response.RoadNodeResponse;
import com.ds.session.session_service.application.client.communicationclient.CommunicationServiceClient;
import com.ds.session.session_service.common.entities.dto.event.LocationTrackingEvent;
import com.ds.session.session_service.common.entities.dto.request.LocationUpdateRequest;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IShipperLocationTrackingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý tracking vị trí shipper trong quá trình giao hàng.
 * - Lưu 5 điểm gần nhất trong cache (in-memory)
 * - Phát hiện khi shipper đi qua các nút giao (road_nodes)
 * - Lưu vào database và publish events qua Kafka/WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShipperLocationTrackingService implements IShipperLocationTrackingService {

    private final ShipperLocationTrackingRepository trackingRepository;
    private final DeliverySessionRepository sessionRepository;
    private final ZoneServiceClient zoneServiceClient;
    private final CommunicationServiceClient communicationServiceClient;

    // In-memory cache: sessionId -> List of last 5 tracking points
    private final Map<String, List<ShipperLocationTracking>> locationCache = new ConcurrentHashMap<>();

    @Value("${shipper-tracking.node-detection.radius-meters:100}")
    private Double nodeDetectionRadius;

    @Value("${shipper-tracking.node-detection.passed-threshold-meters:50}")
    private Double passedThresholdMeters;

    @Value("${shipper-tracking.cache.max-points-per-session:5}")
    private Integer maxCachePoints;

    /**
     * Thêm điểm tracking mới từ Android app
     */
    public void addTrackingPoint(String sessionId, LocationUpdateRequest request) {
        log.debug("[session-service] [ShipperLocationTrackingService.addTrackingPoint] Adding tracking point for session {}", sessionId);

        // Validate session exists and is IN_PROGRESS
        var session = sessionRepository.findById(UUID.fromString(sessionId))
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.warn("[session-service] [ShipperLocationTrackingService.addTrackingPoint] Session {} is not IN_PROGRESS (status: {}). Skipping tracking.", 
                sessionId, session.getStatus());
            return;
        }

        // Convert timestamp
        LocalDateTime timestamp = request.getTimestamp() != null
            ? LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getTimestamp()), ZoneId.systemDefault())
            : LocalDateTime.now();

        // Create tracking entity
        ShipperLocationTracking tracking = ShipperLocationTracking.builder()
            .sessionId(sessionId)
            .deliveryManId(session.getDeliveryManId())
            .latitude(request.getLat())
            .longitude(request.getLon())
            .timestamp(timestamp)
            .accuracy(request.getAccuracy())
            .speed(request.getSpeed())
            .build();

        // Find nearest node asynchronously (don't block)
        try {
            List<RoadNodeResponse> nearestNodes = zoneServiceClient.findNearestNodes(
                request.getLat(), 
                request.getLon(), 
                nodeDetectionRadius
            );

            if (!nearestNodes.isEmpty()) {
                RoadNodeResponse nearestNode = nearestNodes.get(0);
                tracking.setNearestNodeId(nearestNode.getId());
                tracking.setDistanceToNode(nearestNode.getDistance());

                // Check if shipper passed this node
                if (nearestNode.getDistance() <= passedThresholdMeters) {
                    checkIfPassedNode(sessionId, nearestNode.getId(), tracking);
                }
            }
        } catch (Exception e) {
            log.error("[session-service] [ShipperLocationTrackingService.addTrackingPoint] Failed to find nearest node for session {}", sessionId, e);
            // Continue without node info
        }

        // Save to database
        try {
            trackingRepository.save(tracking);
        } catch (Exception e) {
            log.error("[session-service] [ShipperLocationTrackingService.addTrackingPoint] Failed to save tracking point to database", e);
            // Continue - cache will still work
        }

        // Update cache (keep last 5 points)
        updateCache(sessionId, tracking);

        // Publish location update event
        publishLocationUpdate(sessionId, tracking);
    }

    /**
     * Kiểm tra xem shipper có đi qua node không (dựa trên 5 điểm gần nhất)
     */
    private void checkIfPassedNode(String sessionId, String nodeId, ShipperLocationTracking currentTracking) {
        List<ShipperLocationTracking> recentPoints = locationCache.getOrDefault(sessionId, Collections.emptyList());

        // Check if we've seen this node in recent points
        boolean alreadyPassed = recentPoints.stream()
            .anyMatch(p -> nodeId.equals(p.getNearestNodeId()) && 
                          p.getDistanceToNode() != null && 
                          p.getDistanceToNode() <= passedThresholdMeters);

        if (!alreadyPassed && currentTracking.getDistanceToNode() <= passedThresholdMeters) {
            log.info("[session-service] [ShipperLocationTrackingService.checkIfPassedNode] Shipper passed node {} at session {}", nodeId, sessionId);
            
            // Publish node passed event
            try {
                publishNodePassedEvent(sessionId, nodeId, currentTracking);
            } catch (Exception e) {
                log.error("[session-service] [ShipperLocationTrackingService.checkIfPassedNode] Failed to publish node passed event", e);
            }
        }
    }

    /**
     * Cập nhật cache với điểm tracking mới (giữ tối đa maxCachePoints điểm)
     */
    private void updateCache(String sessionId, ShipperLocationTracking tracking) {
        List<ShipperLocationTracking> points = locationCache.computeIfAbsent(sessionId, k -> new ArrayList<>());
        points.add(tracking);

        // Keep only last maxCachePoints points
        if (points.size() > maxCachePoints) {
            points.remove(0);
        }
    }

    /**
     * Publish location update event
     */
    private void publishLocationUpdate(String sessionId, ShipperLocationTracking tracking) {
        try {
            LocationTrackingEvent event = LocationTrackingEvent.builder()
                .sessionId(sessionId)
                .deliveryManId(tracking.getDeliveryManId())
                .lat(tracking.getLatitude())
                .lon(tracking.getLongitude())
                .timestamp(tracking.getTimestamp())
                .nearestNodeId(tracking.getNearestNodeId())
                .eventType("LOCATION_UPDATE")
                .build();

            communicationServiceClient.sendLocationTrackingEvent(event);
            log.debug("[session-service] [ShipperLocationTrackingService.publishLocationUpdate] Published location update for session {}", sessionId);
        } catch (Exception e) {
            log.error("[session-service] [ShipperLocationTrackingService.publishLocationUpdate] Failed to publish location update", e);
        }
    }

    /**
     * Publish node passed event
     */
    private void publishNodePassedEvent(String sessionId, String nodeId, ShipperLocationTracking tracking) {
        try {
            LocationTrackingEvent event = LocationTrackingEvent.builder()
                .sessionId(sessionId)
                .deliveryManId(tracking.getDeliveryManId())
                .lat(tracking.getLatitude())
                .lon(tracking.getLongitude())
                .timestamp(tracking.getTimestamp())
                .nearestNodeId(nodeId)
                .eventType("NODE_PASSED")
                .build();

            communicationServiceClient.sendLocationTrackingEvent(event);
            log.info("[session-service] [ShipperLocationTrackingService.publishNodePassedEvent] Published node passed event: session={}, node={}", sessionId, nodeId);
        } catch (Exception e) {
            log.error("[session-service] [ShipperLocationTrackingService.publishNodePassedEvent] Failed to publish node passed event", e);
        }
    }

    /**
     * Clear cache when session ends
     */
    public void clearCache(String sessionId) {
        locationCache.remove(sessionId);
        log.debug("[session-service] [ShipperLocationTrackingService.clearCache] Cleared cache for session {}", sessionId);
    }
}
