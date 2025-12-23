package com.ds.session.session_service.application.client.zoneclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ds.session.session_service.application.client.zoneclient.request.RouteRequest;
import com.ds.session.session_service.application.client.zoneclient.response.RouteResponse;

@FeignClient(name = "zone-service", url = "${services.zone.base-url}")
public interface ZoneServiceClient {
    
    /**
     * Calculate route between multiple waypoints
     * POST /api/v1/routing/route
     */
    @PostMapping("/api/v1/routing/route")
    RouteResponse calculateRoute(@RequestBody RouteRequest request);

    /**
     * Calculate actual route from raw history waypoints
     * POST /api/v1/routing/actual-route
     */
    @PostMapping("/api/v1/routing/actual-route")
    RouteResponse calculateActualRoute(@RequestBody RouteRequest request);
    
    /**
     * Find nearest road nodes to a location
     * GET /api/v1/road-nodes/nearest
     */
    @GetMapping("/api/v1/road-nodes/nearest")
    List<com.ds.session.session_service.application.client.zoneclient.response.RoadNodeResponse> findNearestNodes(
        @RequestParam("lat") Double lat,
        @RequestParam("lon") Double lon,
        @RequestParam(value = "radius", defaultValue = "100") Double radius
    );
}
