package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.session.EnrichedAssignmentResponse;
import com.ds.gateway.common.entities.dto.session.EnrichedSessionResponse;
import com.ds.gateway.common.interfaces.IParcelServiceClient;
import com.ds.gateway.common.interfaces.ISessionServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service để tổng hợp thông tin session với đầy đủ thông tin assignment (parcel info + proofs)
 * Gọi: Session Service -> Parcel Service (cho mỗi parcel) -> Delivery Proofs Service (cho mỗi assignment)
 */
@Slf4j
@Service
public class SessionEnrichmentService {

    private final ISessionServiceClient sessionServiceClient;
    private final IParcelServiceClient parcelServiceClient;
    private final ObjectMapper objectMapper;

    public SessionEnrichmentService(
            ISessionServiceClient sessionServiceClient,
            IParcelServiceClient parcelServiceClient,
            ObjectMapper objectMapper) {
        this.sessionServiceClient = sessionServiceClient;
        this.parcelServiceClient = parcelServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Lấy session với đầy đủ thông tin: assignments có parcel info và proofs
     */
    public CompletableFuture<EnrichedSessionResponse> getEnrichedSession(UUID sessionId) {
        log.debug("[api-gateway] [SessionEnrichmentService.getEnrichedSession] Enriching session: {}", sessionId);
        
        // Step 1: Get basic session from session-service
        try {
            ResponseEntity<?> sessionResponse = sessionServiceClient.getSessionById(sessionId);
            
            if (sessionResponse.getBody() == null) {
                log.warn("[api-gateway] [SessionEnrichmentService.getEnrichedSession] Session {} not found", sessionId);
                return CompletableFuture.completedFuture(null);
            }
            
            // Parse session response
            JsonNode sessionJson = objectMapper.valueToTree(sessionResponse.getBody());
            JsonNode sessionData = extractData(sessionJson);
            
            if (sessionData == null) {
                log.warn("[api-gateway] [SessionEnrichmentService.getEnrichedSession] Invalid session response for {}", sessionId);
                return CompletableFuture.completedFuture(null);
            }
            
            // Step 2: Extract assignments
            List<JsonNode> assignments = new ArrayList<>();
            if (sessionData.has("assignments") && sessionData.get("assignments").isArray()) {
                for (JsonNode assignment : sessionData.get("assignments")) {
                    assignments.add(assignment);
                }
            }
            
            // Step 3: Collect parcel IDs and assignment IDs
            List<String> parcelIds = assignments.stream()
                    .map(a -> a.has("parcelId") ? a.get("parcelId").asText() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<UUID> assignmentIds = assignments.stream()
                    .map(a -> {
                        if (a.has("id")) {
                            try {
                                return UUID.fromString(a.get("id").asText());
                            } catch (Exception e) {
                                return null;
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Step 4: Fetch parcels and proofs in parallel
            CompletableFuture<Map<String, JsonNode>> parcelsFuture = fetchParcels(parcelIds);
            CompletableFuture<Map<UUID, List<EnrichedAssignmentResponse.DeliveryProofResponse>>> proofsFuture = fetchProofs(assignmentIds);
            
            // Step 5: Combine results
            return parcelsFuture.thenCombine(proofsFuture, (parcelMap, proofsMap) -> {
                // Build enriched assignments
                List<EnrichedAssignmentResponse> enrichedAssignments = assignments.stream()
                        .map(assignment -> enrichAssignment(assignment, parcelMap, proofsMap))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                
                // Build enriched session response
                return buildEnrichedSessionResponse(sessionData, enrichedAssignments);
            });
            
        } catch (Exception e) {
            log.error("[api-gateway] [SessionEnrichmentService.getEnrichedSession] Error enriching session {}", sessionId, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    private CompletableFuture<Map<String, JsonNode>> fetchParcels(List<String> parcelIds) {
        if (parcelIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        
        log.debug("[api-gateway] [SessionEnrichmentService.fetchParcels] Fetching {} parcels", parcelIds.size());
        
        // Fetch parcels in parallel
        List<CompletableFuture<Map.Entry<String, JsonNode>>> futures = parcelIds.stream()
                .map(parcelId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        ResponseEntity<?> response = parcelServiceClient.getParcelById(UUID.fromString(parcelId));
                        if (response.getBody() != null) {
                            JsonNode parcelJson = objectMapper.valueToTree(response.getBody());
                            JsonNode parcelData = extractData(parcelJson);
                            return parcelData != null ? Map.entry(parcelId, parcelData) : null;
                        }
                    } catch (Exception e) {
                        log.debug("[api-gateway] [SessionEnrichmentService.fetchParcels] Failed to fetch parcel {}: {}", parcelId, e.getMessage());
                    }
                    return null;
                }))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private CompletableFuture<Map<UUID, List<EnrichedAssignmentResponse.DeliveryProofResponse>>> fetchProofs(List<UUID> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }
        
        log.debug("[api-gateway] [SessionEnrichmentService.fetchProofs] Fetching proofs for {} assignments", assignmentIds.size());
        
        // Fetch proofs in parallel
        List<CompletableFuture<Map.Entry<UUID, List<EnrichedAssignmentResponse.DeliveryProofResponse>>>> futures = assignmentIds.stream()
                .map(assignmentId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        ResponseEntity<?> response = sessionServiceClient.getProofsByAssignment(assignmentId);
                        if (response.getBody() != null) {
                            JsonNode proofsJson = objectMapper.valueToTree(response.getBody());
                            JsonNode proofsData = extractData(proofsJson);
                            List<EnrichedAssignmentResponse.DeliveryProofResponse> proofs = parseProofs(proofsData);
                            return Map.entry(assignmentId, proofs);
                        }
                    } catch (Exception e) {
                        log.debug("[api-gateway] [SessionEnrichmentService.fetchProofs] Failed to fetch proofs for assignment {}: {}", assignmentId, e.getMessage());
                    }
                    return Map.entry(assignmentId, Collections.<EnrichedAssignmentResponse.DeliveryProofResponse>emptyList());
                }))
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private EnrichedAssignmentResponse enrichAssignment(
            JsonNode assignment,
            Map<String, JsonNode> parcelMap,
            Map<UUID, List<EnrichedAssignmentResponse.DeliveryProofResponse>> proofsMap) {
        try {
            String parcelId = assignment.has("parcelId") ? assignment.get("parcelId").asText() : null;
            UUID assignmentId = assignment.has("id") ? UUID.fromString(assignment.get("id").asText()) : null;
            
            JsonNode parcel = parcelMap.get(parcelId);
            List<EnrichedAssignmentResponse.DeliveryProofResponse> proofs = assignmentId != null ? 
                    proofsMap.getOrDefault(assignmentId, Collections.emptyList()) : Collections.emptyList();
            
            return EnrichedAssignmentResponse.builder()
                    .id(assignmentId)
                    .assignmentId(assignmentId != null ? assignmentId.toString() : null)
                    .sessionId(assignment.has("sessionId") ? assignment.get("sessionId").asText() : null)
                    .parcelId(parcelId)
                    .status(assignment.has("status") ? assignment.get("status").asText() : null)
                    .failReason(assignment.has("failReason") ? assignment.get("failReason").asText() : null)
                    .scanedAt(parseDateTime(assignment, "scanedAt"))
                    .updatedAt(parseDateTime(assignment, "updatedAt"))
                    .completedAt(parseDateTime(assignment, "completedAt"))
                    // Parcel info
                    .parcelCode(parcel != null && parcel.has("code") ? parcel.get("code").asText() : null)
                    .deliveryType(parcel != null && parcel.has("deliveryType") ? parcel.get("deliveryType").asText() : null)
                    .receiverName(parcel != null && parcel.has("receiverName") ? parcel.get("receiverName").asText() : null)
                    .receiverId(parcel != null && parcel.has("receiverId") ? parcel.get("receiverId").asText() : null)
                    .receiverPhone(parcel != null && parcel.has("receiverPhoneNumber") ? parcel.get("receiverPhoneNumber").asText() : null)
                    .deliveryLocation(parcel != null && parcel.has("targetDestination") ? parcel.get("targetDestination").asText() : null)
                    .value(parcel != null && parcel.has("value") && !parcel.get("value").isNull() ? 
                            new BigDecimal(parcel.get("value").asText()) : null)
                    .weight(parcel != null && parcel.has("weight") ? parcel.get("weight").asDouble() : null)
                    .lat(parcel != null && parcel.has("lat") && !parcel.get("lat").isNull() ? 
                            new BigDecimal(parcel.get("lat").asText()) : null)
                    .lon(parcel != null && parcel.has("lon") && !parcel.get("lon").isNull() ? 
                            new BigDecimal(parcel.get("lon").asText()) : null)
                    // Proofs
                    .proofs(proofs)
                    .build();
        } catch (Exception e) {
            log.error("[api-gateway] [SessionEnrichmentService.enrichAssignment] Error enriching assignment", e);
            return null;
        }
    }

    private EnrichedSessionResponse buildEnrichedSessionResponse(JsonNode sessionData, List<EnrichedAssignmentResponse> assignments) {
        try {
            JsonNode deliveryManNode = sessionData.has("deliveryMan") ? sessionData.get("deliveryMan") : null;
            
            EnrichedSessionResponse.DeliveryManInfo deliveryMan = null;
            if (deliveryManNode != null && !deliveryManNode.isNull()) {
                deliveryMan = EnrichedSessionResponse.DeliveryManInfo.builder()
                        .name(deliveryManNode.has("name") ? deliveryManNode.get("name").asText() : null)
                        .vehicleType(deliveryManNode.has("vehicleType") ? deliveryManNode.get("vehicleType").asText() : null)
                        .capacityKg(deliveryManNode.has("capacityKg") && !deliveryManNode.get("capacityKg").isNull() ? 
                                deliveryManNode.get("capacityKg").asDouble() : null)
                        .phone(deliveryManNode.has("phone") ? deliveryManNode.get("phone").asText() : null)
                        .email(deliveryManNode.has("email") ? deliveryManNode.get("email").asText() : null)
                        .build();
            }
            
            return EnrichedSessionResponse.builder()
                    .id(sessionData.has("id") ? UUID.fromString(sessionData.get("id").asText()) : null)
                    .deliveryManId(sessionData.has("deliveryManId") ? sessionData.get("deliveryManId").asText() : null)
                    .status(sessionData.has("status") ? sessionData.get("status").asText() : null)
                    .startTime(parseDateTime(sessionData, "startTime"))
                    .endTime(parseDateTime(sessionData, "endTime"))
                    .totalTasks(sessionData.has("totalTasks") ? sessionData.get("totalTasks").asInt() : null)
                    .completedTasks(sessionData.has("completedTasks") ? sessionData.get("completedTasks").asInt() : null)
                    .failedTasks(sessionData.has("failedTasks") ? sessionData.get("failedTasks").asInt() : null)
                    .deliveryMan(deliveryMan)
                    .assignments(assignments)
                    .build();
        } catch (Exception e) {
            log.error("[api-gateway] [SessionEnrichmentService.buildEnrichedSessionResponse] Error building enriched session", e);
            return null;
        }
    }

    private List<EnrichedAssignmentResponse.DeliveryProofResponse> parseProofs(JsonNode proofsData) {
        List<EnrichedAssignmentResponse.DeliveryProofResponse> proofs = new ArrayList<>();
        if (proofsData != null && proofsData.isArray()) {
            for (JsonNode proof : proofsData) {
                try {
                    proofs.add(EnrichedAssignmentResponse.DeliveryProofResponse.builder()
                            .id(proof.has("id") ? UUID.fromString(proof.get("id").asText()) : null)
                            .type(proof.has("type") ? proof.get("type").asText() : null)
                            .mediaUrl(proof.has("mediaUrl") ? proof.get("mediaUrl").asText() : null)
                            .confirmedBy(proof.has("confirmedBy") ? proof.get("confirmedBy").asText() : null)
                            .createdAt(parseDateTime(proof, "createdAt"))
                            .build());
                } catch (Exception e) {
                    log.debug("[api-gateway] [SessionEnrichmentService.parseProofs] Failed to parse proof", e);
                }
            }
        }
        return proofs;
    }

    private LocalDateTime parseDateTime(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        try {
            String dateStr = node.get(fieldName).asText();
            return LocalDateTime.parse(dateStr.replace("Z", "").replace("+00:00", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode extractData(JsonNode response) {
        if (response == null) return null;
        // Handle BaseResponse wrapper
        if (response.has("data")) {
            return response.get("data");
        }
        // If no wrapper, return as-is
        return response;
    }
}
