package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import com.ds.gateway.common.interfaces.IZoneServiceClient;
import com.ds.gateway.common.interfaces.IParcelServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for delivery session operations with nested queries
 * This service orchestrates calls to multiple services (session, zone, parcel)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverySessionService {

    private final ISessionServiceClient sessionServiceClient;
    private final IZoneServiceClient zoneServiceClient;
    private final IParcelServiceClient parcelServiceClient;

    /**
     * API 1: Get delivery_session and all delivery_assignments in that session
     * This is a nesting query that combines session and assignment data
     */
    public ResponseEntity<?> getSessionWithAssignments(java.util.UUID sessionId) {
        log.info("Fetching session {} with all assignments", sessionId);
        
        // Call session service to get session with assignments
        ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
        
        if (sessionResponse.getStatusCode().is2xxSuccessful() && sessionResponse.getBody() != null) {
            log.info("Successfully retrieved session {} with assignments", sessionId);
            return sessionResponse;
        } else {
            log.error("Failed to retrieve session {}", sessionId);
            return ResponseEntity.status(sessionResponse.getStatusCode()).body(sessionResponse.getBody());
        }
    }

    /**
     * API 2: Get demo-route by data from API 1
     * This must be done in api gateway, service layer
     * It takes session data and calculates a demo route for all assignments
     */
    public ResponseEntity<?> getDemoRouteForSession(java.util.UUID sessionId) {
        log.info("Calculating demo route for session {}", sessionId);
        
        try {
            // Step 1: Get session with assignments (API 1)
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            
            if (!sessionResponse.getStatusCode().is2xxSuccessful() || sessionResponse.getBody() == null) {
                log.error("Failed to retrieve session {} for route calculation", sessionId);
                return ResponseEntity.status(sessionResponse.getStatusCode()).body(sessionResponse.getBody());
            }

            // Step 2: Extract assignment data from session response
            Map<String, Object> sessionData = (Map<String, Object>) sessionResponse.getBody();
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) sessionData.get("assignments");
            
            if (assignments == null || assignments.isEmpty()) {
                log.warn("No assignments found for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "No assignments found for this session"));
            }

            // Step 3: Get parcel details for each assignment to get destination coordinates
            List<CompletableFuture<Map<String, Object>>> parcelFutures = new ArrayList<>();
            List<Map<String, Object>> waypoints = new ArrayList<>();
            
            for (Map<String, Object> assignment : assignments) {
                String parcelId = (String) assignment.get("parcelId");
                if (parcelId != null) {
                    CompletableFuture<Map<String, Object>> parcelFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            ResponseEntity<?> parcelResponse = parcelServiceClient.getParcelById(java.util.UUID.fromString(parcelId));
                            if (parcelResponse.getStatusCode().is2xxSuccessful() && parcelResponse.getBody() != null) {
                                return (Map<String, Object>) parcelResponse.getBody();
                            }
                        } catch (Exception e) {
                            log.error("Error fetching parcel {}: {}", parcelId, e.getMessage());
                        }
                        return null;
                    });
                    parcelFutures.add(parcelFuture);
                }
            }

            // Wait for all parcel requests to complete
            CompletableFuture.allOf(parcelFutures.toArray(new CompletableFuture[0])).join();
            
            // Step 4: Build waypoints from parcel destinations
            for (int i = 0; i < parcelFutures.size(); i++) {
                Map<String, Object> parcelData = parcelFutures.get(i).get();
                if (parcelData != null) {
                    // Extract destination coordinates from parcel data
                    // Assuming parcel has sendTo field with lat/lon or destination info
                    Map<String, Object> destination = extractDestinationFromParcel(parcelData);
                    if (destination != null) {
                        Map<String, Object> waypoint = new HashMap<>();
                        waypoint.put("lat", destination.get("lat"));
                        waypoint.put("lon", destination.get("lon"));
                        waypoint.put("parcelId", assignments.get(i).get("parcelId"));
                        waypoints.add(waypoint);
                    }
                }
            }

            if (waypoints.isEmpty()) {
                log.warn("No valid waypoints found for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "No valid waypoints found for route calculation"));
            }

            // Step 5: Call zone service to calculate demo route
            Map<String, Object> routeRequest = Map.of(
                "waypoints", waypoints
            );

            CompletableFuture<Object> routeFuture = zoneServiceClient.calculateDemoRoute(routeRequest);
            Object routeResponse = routeFuture.get();

            log.info("Successfully calculated demo route for session {} with {} waypoints", sessionId, waypoints.size());
            return ResponseEntity.ok(routeResponse);

        } catch (Exception e) {
            log.error("Error calculating demo route for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to calculate demo route: " + e.getMessage()));
        }
    }

    /**
     * API 3: Set delivery_assignments status and parcel status
     * This is a nesting query that updates both assignment and parcel status
     */
    public ResponseEntity<?> updateAssignmentAndParcelStatus(
            java.util.UUID sessionId,
            java.util.UUID assignmentId,
            Object statusUpdateRequest) {
        log.info("Updating assignment {} and parcel status for session {}", assignmentId, sessionId);
        
        try {
            // Step 1: Get assignment details to find parcel ID
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            
            if (!sessionResponse.getStatusCode().is2xxSuccessful() || sessionResponse.getBody() == null) {
                log.error("Failed to retrieve session {} for status update", sessionId);
                return ResponseEntity.status(sessionResponse.getStatusCode()).body(sessionResponse.getBody());
            }

            Map<String, Object> sessionData = (Map<String, Object>) sessionResponse.getBody();
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) sessionData.get("assignments");
            
            Map<String, Object> targetAssignment = null;
            for (Map<String, Object> assignment : assignments) {
                if (assignmentId.toString().equals(assignment.get("id").toString())) {
                    targetAssignment = assignment;
                    break;
                }
            }

            if (targetAssignment == null) {
                log.error("Assignment {} not found in session {}", assignmentId, sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "Assignment not found in this session"));
            }

            String parcelId = (String) targetAssignment.get("parcelId");
            if (parcelId == null) {
                log.error("Parcel ID not found in assignment {}", assignmentId);
                return ResponseEntity.badRequest().body(Map.of("error", "Parcel ID not found in assignment"));
            }

            // Step 2: Extract status from request
            Map<String, Object> requestMap = (Map<String, Object>) statusUpdateRequest;
            String assignmentStatusStr = (String) requestMap.get("assignmentStatus");
            String parcelEventStr = (String) requestMap.get("parcelEvent");
            String failReason = (String) requestMap.get("failReason");
            Object routeInfoObj = requestMap.get("routeInfo");

            // Step 3: Build update request for session service
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("assignmentStatus", assignmentStatusStr);
            updateRequest.put("parcelEvent", parcelEventStr);
            if (failReason != null) {
                updateRequest.put("failReason", failReason);
            }
            if (routeInfoObj != null) {
                updateRequest.put("routeInfo", routeInfoObj);
            }

            // Step 4: Update assignment status and parcel status (via session service)
            // This endpoint handles both assignment and parcel status updates
            ResponseEntity<?> updateResponse = sessionServiceClient.updateAssignmentStatus(sessionId, assignmentId, updateRequest);
            
            if (!updateResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to update assignment {} status", assignmentId);
                return ResponseEntity.status(updateResponse.getStatusCode()).body(updateResponse.getBody());
            }

            log.info("Successfully updated assignment {} and parcel {} status", assignmentId, parcelId);
            return updateResponse;

        } catch (Exception e) {
            log.error("Error updating assignment and parcel status: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update status: " + e.getMessage()));
        }
    }

    /**
     * Helper method to extract destination coordinates from parcel data
     */
    private Map<String, Object> extractDestinationFromParcel(Map<String, Object> parcelData) {
        // This is a placeholder - adjust based on actual parcel data structure
        // Assuming parcel has sendTo field with lat/lon or destination info
        try {
            Object sendTo = parcelData.get("sendTo");
            if (sendTo instanceof Map) {
                Map<String, Object> destination = (Map<String, Object>) sendTo;
                if (destination.containsKey("lat") && destination.containsKey("lon")) {
                    return destination;
                }
            }
            
            // Alternative: check for destination field
            Object destination = parcelData.get("destination");
            if (destination instanceof Map) {
                Map<String, Object> dest = (Map<String, Object>) destination;
                if (dest.containsKey("lat") && dest.containsKey("lon")) {
                    return dest;
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting destination from parcel data: {}", e.getMessage());
        }
        return null;
    }
}
