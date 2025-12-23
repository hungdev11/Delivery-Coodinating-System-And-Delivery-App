package com.ds.session.session_service.business.v1.services;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.ShipperLocationTracking;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.app_context.repositories.ShipperLocationTrackingRepository;
import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.request.RouteRequest;
import com.ds.session.session_service.application.client.zoneclient.response.RouteResponse;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to build actual route for a delivery session from tracking history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionRouteService {

    private final ShipperLocationTrackingRepository trackingRepository;
    private final DeliverySessionRepository sessionRepository;
    private final ZoneServiceClient zoneServiceClient;

    /**
     * Build actual route for a session using all tracking points in chronological order.
     */
    @Transactional(readOnly = true)
    public RouteResponse getActualRouteForSession(UUID sessionId) {
        log.debug("[session-service] [SessionRouteService.getActualRouteForSession] Building actual route for session {}", sessionId);

        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId));

        List<ShipperLocationTracking> pointsDesc = trackingRepository
                .findBySessionIdOrderByTimestampDesc(sessionId.toString());

        if (pointsDesc.isEmpty()) {
            throw new ResourceNotFound("No tracking points found for session " + sessionId);
        }

        // Sort ascending by timestamp to respect real travel order
        List<ShipperLocationTracking> points = pointsDesc.stream()
                .sorted(Comparator.comparing(ShipperLocationTracking::getTimestamp))
                .collect(Collectors.toList());

        List<RouteRequest.Waypoint> waypoints = points.stream()
                .map(p -> RouteRequest.Waypoint.builder()
                        .lat(p.getLatitude())
                        .lon(p.getLongitude())
                        .parcelId(null)
                        .moveToEnd(null)
                        .build())
                .collect(Collectors.toList());

        // If only one point, duplicate so routing engine has at least start & end
        if (waypoints.size() == 1) {
            waypoints.add(waypoints.get(0));
        }

        RouteRequest routeRequest = RouteRequest.builder()
                .waypoints(waypoints)
                .steps(true)
                .annotations(true)
                .build();

        log.debug("[session-service] [SessionRouteService.getActualRouteForSession] Calling zone-service /routing/actual-route for session {} with {} points",
                sessionId, waypoints.size());

        return zoneServiceClient.calculateActualRoute(routeRequest);
    }
}
