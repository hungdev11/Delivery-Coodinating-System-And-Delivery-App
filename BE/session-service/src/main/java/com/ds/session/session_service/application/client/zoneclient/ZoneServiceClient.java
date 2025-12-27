package com.ds.session.session_service.application.client.zoneclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ds.session.session_service.application.client.zoneclient.request.RouteRequest;
import com.ds.session.session_service.application.client.zoneclient.request.TableMatrixRequest;
import com.ds.session.session_service.application.client.zoneclient.response.RouteResponse;
import com.ds.session.session_service.application.client.zoneclient.response.TableMatrixResponse;

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
    
    /**
     * Get OSRM table matrix (distance/duration matrix) for VRP solving
     * POST /api/v1/routing/table-matrix
     * Note: Response is wrapped in BaseResponse, but Feign will auto-unwrap if configured
     */
    @PostMapping("/api/v1/routing/table-matrix")
    com.ds.session.session_service.application.client.zoneclient.response.BaseResponse<TableMatrixResponse> getTableMatrix(
        @RequestBody TableMatrixRequest request
    );
    
    /**
     * Solve VRP assignment problem
     * POST /api/v1/routing/vrp-assignment
     * Returns assignments of orders to shippers with workload balancing and constraints
     */
    @PostMapping("/api/v1/routing/vrp-assignment")
    com.ds.session.session_service.application.client.zoneclient.response.BaseResponse<com.ds.session.session_service.application.client.zoneclient.response.VRPAssignmentResponse> solveVRPAssignment(
        @RequestBody com.ds.session.session_service.application.client.zoneclient.request.VRPAssignmentRequest request
    );
}
