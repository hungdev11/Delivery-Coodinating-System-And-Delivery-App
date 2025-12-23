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
     * Enriches assignments with parcel information
     */
    public ResponseEntity<?> getSessionWithAssignments(java.util.UUID sessionId) {
        log.debug("[api-gateway] [DeliverySessionService.getSessionWithAssignments] Fetching session {} with all assignments", sessionId);
        
        try {
            // Call session service to get session with assignments
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            
            if (!sessionResponse.getStatusCode().is2xxSuccessful() || sessionResponse.getBody() == null) {
                log.error("[api-gateway] [DeliverySessionService.getSessionWithAssignments] Failed to retrieve session {}", sessionId);
                return ResponseEntity.status(sessionResponse.getStatusCode()).body(sessionResponse.getBody());
            }

            // Unwrap BaseResponse if needed
            Map<String, Object> responseBody = (Map<String, Object>) sessionResponse.getBody();
            Map<String, Object> sessionData = responseBody;
            
            // Check if it's wrapped in BaseResponse
            if (responseBody.containsKey("result")) {
                sessionData = (Map<String, Object>) responseBody.get("result");
            }
            
            // Get assignments
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) sessionData.get("assignments");
            if (assignments == null || assignments.isEmpty()) {
                log.debug("[api-gateway] [DeliverySessionService.getSessionWithAssignments] No assignments found for session {}", sessionId);
                return sessionResponse; // Return as-is if no assignments
            }

            // Enrich each assignment with parcel information
            List<CompletableFuture<Map<String, Object>>> parcelFutures = new ArrayList<>();
            for (Map<String, Object> assignment : assignments) {
                String parcelId = (String) assignment.get("parcelId");
                if (parcelId != null) {
                    CompletableFuture<Map<String, Object>> parcelFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            ResponseEntity<?> parcelResponse = parcelServiceClient.getParcelById(java.util.UUID.fromString(parcelId));
                            if (parcelResponse.getStatusCode().is2xxSuccessful() && parcelResponse.getBody() != null) {
                                Map<String, Object> parcelResponseBody = (Map<String, Object>) parcelResponse.getBody();
                                // Unwrap BaseResponse if needed
                                if (parcelResponseBody.containsKey("result")) {
                                    return (Map<String, Object>) parcelResponseBody.get("result");
                                }
                                return parcelResponseBody;
                            }
                        } catch (Exception e) {
                            log.error("[api-gateway] [DeliverySessionService.getSessionWithAssignments] Error fetching parcel {}: {}", parcelId, e.getMessage());
                        }
                        return null;
                    });
                    parcelFutures.add(parcelFuture);
                } else {
                    parcelFutures.add(CompletableFuture.completedFuture(null));
                }
            }

            // Wait for all parcel requests to complete
            CompletableFuture.allOf(parcelFutures.toArray(new CompletableFuture[0])).join();
            
            // Enrich assignments with parcel data
            for (int i = 0; i < assignments.size() && i < parcelFutures.size(); i++) {
                Map<String, Object> parcelData = parcelFutures.get(i).get();
                if (parcelData != null) {
                    // Add parcel info to assignment
                    Map<String, Object> parcelInfo = new HashMap<>();
                    parcelInfo.put("id", parcelData.get("id"));
                    parcelInfo.put("code", parcelData.get("code"));
                    parcelInfo.put("targetDestination", parcelData.get("targetDestination"));
                    parcelInfo.put("value", parcelData.get("value"));
                    parcelInfo.put("weight", parcelData.get("weight"));
                    parcelInfo.put("deliveryType", parcelData.get("deliveryType"));
                    parcelInfo.put("receiverName", parcelData.get("receiverName"));
                    parcelInfo.put("receiverPhoneNumber", parcelData.get("receiverPhoneNumber"));
                    parcelInfo.put("lat", parcelData.get("lat"));
                    parcelInfo.put("lon", parcelData.get("lon"));
                    assignments.get(i).put("parcelInfo", parcelInfo);
                }
            }

            // Update sessionData with enriched assignments
            sessionData.put("assignments", assignments);
            
            // Re-wrap in BaseResponse format if original was wrapped
            Map<String, Object> finalResponse = new HashMap<>();
            if (responseBody.containsKey("result")) {
                finalResponse.put("success", responseBody.get("success"));
                finalResponse.put("message", responseBody.get("message"));
                finalResponse.put("result", sessionData);
            } else {
                finalResponse = sessionData;
            }

            log.debug("[api-gateway] [DeliverySessionService.getSessionWithAssignments] Successfully enriched session {} with parcel info for {} assignments", sessionId, assignments.size());
            return ResponseEntity.ok(finalResponse);
            
        } catch (Exception e) {
            log.error("[api-gateway] [DeliverySessionService.getSessionWithAssignments] Error enriching session {} with parcel info", sessionId, e);
            // Fallback to basic response
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            return sessionResponse;
        }
    }

    /**
     * API 2b: Proxy to session-service actual route endpoint.
     */
    public ResponseEntity<?> getActualRouteForSession(java.util.UUID sessionId) {
        log.debug("[api-gateway] [DeliverySessionService.getActualRouteForSession] Proxying actual route for session {}", sessionId);
        return sessionServiceClient.getActualRouteForSession(sessionId);
    }

    /**
     * API 2: Get demo-route by data from API 1
     * This must be done in api gateway, service layer
     * It takes session data and calculates a demo route for all assignments
     * 
     * @param vehicle Vehicle type: "bicycle" (default) or "car"
     * @param routingType Routing type: "full" (default), "rating-only", "blocking-only", "base"
     */
    public ResponseEntity<?> getDemoRouteForSession(java.util.UUID sessionId, Double overrideStartLat, Double overrideStartLon, String vehicle, String routingType) {
        log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Calculating demo route for session {} (override start: {}, {}, vehicle: {}, routingType: {})", sessionId, overrideStartLat, overrideStartLon, vehicle, routingType);
        
        try {
            // Step 1: Get session with assignments (API 1)
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            
            if (!sessionResponse.getStatusCode().is2xxSuccessful() || sessionResponse.getBody() == null) {
                log.error("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Failed to retrieve session {} for route calculation", sessionId);
                return ResponseEntity.status(sessionResponse.getStatusCode()).body(sessionResponse.getBody());
            }

            // Step 2: Extract assignment data from session response (unwrap BaseResponse if needed)
            Map<String, Object> responseBody = (Map<String, Object>) sessionResponse.getBody();
            Map<String, Object> sessionData = responseBody;
            
            // Unwrap BaseResponse if needed
            if (responseBody.containsKey("result")) {
                sessionData = (Map<String, Object>) responseBody.get("result");
            }
            
            List<Map<String, Object>> assignments = (List<Map<String, Object>>) sessionData.get("assignments");
            
            if (assignments == null || assignments.isEmpty()) {
                log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] No assignments found for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "No assignments found for this session"));
            }

            // Step 3: Get parcel details for each assignment to get destination coordinates
            List<CompletableFuture<Map<String, Object>>> parcelFutures = new ArrayList<>();
            
            for (Map<String, Object> assignment : assignments) {
                String parcelId = (String) assignment.get("parcelId");
                if (parcelId != null) {
                    CompletableFuture<Map<String, Object>> parcelFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            ResponseEntity<?> parcelResponse = parcelServiceClient.getParcelById(java.util.UUID.fromString(parcelId));
                            if (parcelResponse.getStatusCode().is2xxSuccessful() && parcelResponse.getBody() != null) {
                                Map<String, Object> parcelResponseBody = (Map<String, Object>) parcelResponse.getBody();
                                // Unwrap BaseResponse if needed
                                if (parcelResponseBody.containsKey("result")) {
                                    return (Map<String, Object>) parcelResponseBody.get("result");
                                }
                                return parcelResponseBody;
                            }
                        } catch (Exception e) {
                            log.error("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Error fetching parcel {}", parcelId, e);
                        }
                        return null;
                    });
                    parcelFutures.add(parcelFuture);
                }
            }

            // Wait for all parcel requests to complete
            CompletableFuture.allOf(parcelFutures.toArray(new CompletableFuture[0])).join();
            
            // Step 4: Build waypoints from parcel destinations and group by priority
            Map<Integer, List<Map<String, Object>>> priorityGroupsMap = new HashMap<>();
            Map<String, Object> startPoint = null;
            List<Map<String, Object>> allWaypoints = new ArrayList<>();
            
            for (int i = 0; i < parcelFutures.size(); i++) {
                try {
                    Map<String, Object> parcelData = parcelFutures.get(i).get();
                    if (parcelData != null) {
                        // Extract lat/lon directly from parcel data
                        Object latObj = parcelData.get("lat");
                        Object lonObj = parcelData.get("lon");
                        
                        if (latObj != null && lonObj != null) {
                            Double lat = null;
                            Double lon = null;
                            
                            // Handle different number types (BigDecimal, Double, etc.)
                            if (latObj instanceof Number) {
                                lat = ((Number) latObj).doubleValue();
                            }
                            if (lonObj instanceof Number) {
                                lon = ((Number) lonObj).doubleValue();
                            }
                            
                            if (lat != null && lon != null) {
                                Map<String, Object> waypoint = new HashMap<>();
                                waypoint.put("lat", lat);
                                waypoint.put("lon", lon);
                                waypoint.put("parcelId", assignments.get(i).get("parcelId"));
                                allWaypoints.add(waypoint);
                                log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Added waypoint {}: lat={}, lon={}, parcelId={}", i, lat, lon, assignments.get(i).get("parcelId"));
                            } else {
                                log.warn("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Invalid lat/lon for parcel {}: latObj={}, lonObj={}", assignments.get(i).get("parcelId"), latObj, lonObj);
                            }
                        } else {
                            log.warn("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Missing lat/lon for parcel {}", assignments.get(i).get("parcelId"));
                        }
                    } else {
                        log.warn("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Parcel data is null for assignment {}", assignments.get(i).get("parcelId"));
                    }
                } catch (Exception e) {
                    log.error("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Error processing parcel {}: {}", assignments.get(i).get("parcelId"), e.getMessage(), e);
                }
            }
            
            log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Collected {} waypoints from {} parcels", allWaypoints.size(), parcelFutures.size());

            if (allWaypoints.isEmpty()) {
                log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] No valid waypoints found for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "No valid waypoints found for route calculation"));
            }

            // Derive start point:
            // 1) Prefer session start/current location if present
            // 2) Fallback to driver's first parcel location but keep it in destinations
            Double startLat = overrideStartLat != null ? overrideStartLat : toDouble(sessionData.get("startLat"));
            Double startLon = overrideStartLon != null ? overrideStartLon : toDouble(sessionData.get("startLon"));
            if (startLat == null || startLon == null) {
                startLat = overrideStartLat != null ? overrideStartLat : toDouble(sessionData.get("currentLat"));
                startLon = overrideStartLon != null ? overrideStartLon : toDouble(sessionData.get("currentLon"));
            }
            if (startLat == null || startLon == null) {
                startLat = overrideStartLat != null ? overrideStartLat : toDouble(sessionData.get("lat"));
                startLon = overrideStartLon != null ? overrideStartLon : toDouble(sessionData.get("lon"));
            }

            if (startLat != null && startLon != null) {
                startPoint = new HashMap<>();
                startPoint.put("lat", startLat);
                startPoint.put("lon", startLon);
                startPoint.put("parcelId", "START");
            } else {
                // Fallback: reuse first waypoint but do NOT remove it from destinations
                Map<String, Object> firstWaypoint = new HashMap<>(allWaypoints.get(0));
                startPoint = new HashMap<>(firstWaypoint);
                startPoint.put("parcelId", "START");
            }
            
            // Group ALL waypoints by priority based on deliveryType (including the first one)
            for (int i = 0; i < allWaypoints.size(); i++) {
                Map<String, Object> waypoint = allWaypoints.get(i);
                Map<String, Object> parcelData = parcelFutures.get(i).get();
                
                // Map deliveryType to priority: EXPRESS=1, FAST=2, NORMAL=3, ECONOMY=4
                String deliveryType = parcelData != null ? (String) parcelData.get("deliveryType") : null;
                int priority = mapDeliveryTypeToPriority(deliveryType);
                
                priorityGroupsMap.computeIfAbsent(priority, k -> new ArrayList<>()).add(waypoint);
            }

            if (priorityGroupsMap.isEmpty()) {
                log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] No destination waypoints found for session {}", sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "No destination waypoints found for route calculation"));
            }

            // Step 5: Build priority groups list
            List<Map<String, Object>> priorityGroups = new ArrayList<>();
            for (Map.Entry<Integer, List<Map<String, Object>>> entry : priorityGroupsMap.entrySet()) {
                Map<String, Object> group = new HashMap<>();
                group.put("priority", entry.getKey());
                group.put("waypoints", entry.getValue());
                priorityGroups.add(group);
            }

            // Step 6: Map vehicle and routingType to zone-service format
            // Normalize vehicle: "bicycle" -> "motorbike" (zone-service uses "motorbike")
            String normalizedVehicle = "bicycle".equalsIgnoreCase(vehicle) ? "motorbike" : "car";
            
            // Map routingType to mode based on vehicle
            String mode;
            if ("bicycle".equalsIgnoreCase(vehicle)) {
                switch (routingType.toLowerCase()) {
                    case "rating-only":
                        mode = "v2-rating-only";
                        break;
                    case "blocking-only":
                        mode = "v2-blocking-only";
                        break;
                    case "base":
                        mode = "v2-base";
                        break;
                    case "full":
                    default:
                        mode = "v2-full";
                        break;
                }
            } else { // car
                switch (routingType.toLowerCase()) {
                    case "rating-only":
                        mode = "v2-car-rating-only";
                        break;
                    case "blocking-only":
                        mode = "v2-car-blocking-only";
                        break;
                    case "base":
                        mode = "v2-car-base";
                        break;
                    case "full":
                    default:
                        mode = "v2-car-full";
                        break;
                }
            }

            // Step 7: Call zone service to calculate demo route with correct format
            Map<String, Object> routeRequest = new HashMap<>();
            routeRequest.put("startPoint", startPoint);
            routeRequest.put("priorityGroups", priorityGroups);
            routeRequest.put("vehicle", normalizedVehicle);
            routeRequest.put("mode", mode);

            log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Sending request to zone-service with startPoint: {}, {} priority groups (total {} waypoints), vehicle: {}, mode: {}", startPoint, priorityGroups.size(), allWaypoints.size(), normalizedVehicle, mode);
            
            try {
                CompletableFuture<Object> routeFuture = zoneServiceClient.calculateDemoRoute(routeRequest);
                // Add timeout to prevent hanging (30 seconds)
                Object routeResponse = routeFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);

                log.debug("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Successfully calculated demo route for session {} with {} total waypoints", sessionId, allWaypoints.size());

                // Normalize response to BaseResponse format for Android client
                if (routeResponse instanceof Map<?, ?> map) {
                    if (map.containsKey("result") || map.containsKey("message")) {
                        return ResponseEntity.ok(routeResponse);
                    }
                    return ResponseEntity.ok(Map.of("result", map));
                }

                return ResponseEntity.ok(Map.of("result", routeResponse));
            } catch (java.util.concurrent.TimeoutException e) {
                log.error("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Timeout waiting for zone-service response for session {}", sessionId, e);
                return ResponseEntity.status(504).body(Map.of("error", "Zone service timeout: " + e.getMessage()));
            }

        } catch (Exception e) {
            log.error("[api-gateway] [DeliverySessionService.getDemoRouteForSession] Error calculating demo route for session {}", sessionId, e);
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
        log.debug("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Updating assignment {} and parcel status for session {}", assignmentId, sessionId);
        
        try {
            // Step 1: Get assignment details to find parcel ID
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            
            if (!sessionResponse.getStatusCode().is2xxSuccessful() || sessionResponse.getBody() == null) {
                log.error("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Failed to retrieve session {} for status update", sessionId);
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
                log.error("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Assignment {} not found in session {}", assignmentId, sessionId);
                return ResponseEntity.badRequest().body(Map.of("error", "Assignment not found in this session"));
            }

            String parcelId = (String) targetAssignment.get("parcelId");
            if (parcelId == null) {
                log.error("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Parcel ID not found in assignment {}", assignmentId);
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
                log.error("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Failed to update assignment {} status", assignmentId);
                return ResponseEntity.status(updateResponse.getStatusCode()).body(updateResponse.getBody());
            }

            log.debug("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Successfully updated assignment {} and parcel {} status", assignmentId, parcelId);
            return updateResponse;

        } catch (Exception e) {
            log.error("[api-gateway] [DeliverySessionService.updateAssignmentAndParcelStatus] Error updating assignment and parcel status", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update status: " + e.getMessage()));
        }
    }

    /**
     * Map deliveryType to priority number for demo route
     * Priority: 0 = urgent, 1 = express, 2 = fast, 3 = normal, 4 = economy
     */
    private int mapDeliveryTypeToPriority(String deliveryType) {
        if (deliveryType == null) {
            return 3; // Default to NORMAL
        }
        String upperType = deliveryType.toUpperCase();
        switch (upperType) {
            case "EXPRESS":
                return 1;
            case "FAST":
                return 2;
            case "NORMAL":
                return 3;
            case "ECONOMY":
                return 4;
            default:
                return 3; // Default to NORMAL
        }
    }

    /**
     * Helper method to extract destination coordinates from parcel data
     * ParcelResponse has lat and lon directly as BigDecimal
     */
    private Map<String, Object> extractDestinationFromParcel(Map<String, Object> parcelData) {
        try {
            // ParcelResponse has lat and lon directly
            Object lat = parcelData.get("lat");
            Object lon = parcelData.get("lon");
            
            if (lat != null && lon != null) {
                Map<String, Object> destination = new HashMap<>();
                destination.put("lat", lat);
                destination.put("lon", lon);
                return destination;
            }
            
            // Fallback: check for sendTo field (legacy format)
            Object sendTo = parcelData.get("sendTo");
            if (sendTo instanceof Map) {
                Map<String, Object> destination = (Map<String, Object>) sendTo;
                if (destination.containsKey("lat") && destination.containsKey("lon")) {
                    return destination;
                }
            }
            
            // Fallback: check for destination field
            Object destination = parcelData.get("destination");
            if (destination instanceof Map) {
                Map<String, Object> dest = (Map<String, Object>) destination;
                if (dest.containsKey("lat") && dest.containsKey("lon")) {
                    return dest;
                }
            }
        } catch (Exception e) {
            log.debug("[api-gateway] [DeliverySessionService.extractDestinationFromParcel] Error extracting destination from parcel data: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Safely convert object to Double if possible.
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
