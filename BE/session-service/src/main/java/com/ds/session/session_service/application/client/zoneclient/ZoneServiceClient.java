package com.ds.session.session_service.application.client.zoneclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
