package com.ds.gateway.application.controllers.v1;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.business.v1.services.ConversationEnrichmentService;
import com.ds.gateway.common.entities.dto.communicate.ConversationRequest;
import com.ds.gateway.common.entities.dto.communicate.EnrichedConversationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API Gateway proxy for Communication Service
 * Handles conversations, messages, proposals, and WebSocket endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunicationController {

    private final RestTemplate restTemplate;
    private final ConversationEnrichmentService conversationEnrichmentService;

    @Value("${services.communication.base-url}")
    private String communicationServiceUrl;

    // ============================================
    // Conversation Endpoints
    // ============================================

    /**
     * Get conversations for a user (BASIC - direct proxy)
     */
    @GetMapping("/conversations/user/{userId}")
    @AuthRequired
    public ResponseEntity<?> getMyConversations(@PathVariable String userId) {
        log.info("GET /api/v1/conversations/user/{} - Proxying to Communication Service", userId);
        String url = communicationServiceUrl + "/api/v1/conversations/user/" + userId;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Get ENRICHED conversations for Android client
     * Includes user info, parcel info, and session info
     * 
     * Usage: GET /api/v1/conversations?userId={userId}
     * Header: X-User-Id (extracted from JWT by API Gateway)
     */
    @GetMapping("/conversations")
    @AuthRequired
    public CompletableFuture<ResponseEntity<List<EnrichedConversationResponse>>> getEnrichedConversations(
            @RequestParam String userId) {
        log.info("📱 GET /api/v1/conversations?userId={} - Getting enriched conversations for Android", userId);
        
        return conversationEnrichmentService.getEnrichedConversations(userId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("❌ Failed to get enriched conversations: {}", ex.getMessage(), ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    /**
     * Find or create conversation between two users
     * This endpoint enriches the conversation with partner name and other details
     */
    @GetMapping("/conversations/find-by-users")
    @AuthRequired
    public ResponseEntity<?> getConversationByTwoUsers(
            @RequestParam("user1") String userId1,
            @RequestParam("user2") String userId2,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserIdFromHeader) {
        log.info("GET /api/v1/conversations/find-by-users?user1={}&user2={} - Enriching response", userId1, userId2);
        
        // Get basic conversation from Communication Service
        String url = communicationServiceUrl + "/api/v1/conversations/find-by-users?user1=" + userId1 + "&user2=" + userId2;
        try {
            Object conversationObj = restTemplate.getForObject(url, Object.class);
            
            if (conversationObj == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Convert to JsonNode for enrichment
            ObjectMapper mapper = new ObjectMapper();
            JsonNode convNode = mapper.valueToTree(conversationObj);
            
            // Determine current user and partner
            // Use header first, then try to determine from conversation data
            String currentUserId = currentUserIdFromHeader;
            String partnerId = null;
            
            if (currentUserId == null) {
                // Try to determine from conversation data (partnerId field)
                if (convNode.has("partnerId")) {
                    String convPartnerId = convNode.get("partnerId").asText();
                    // Determine which user is the current user
                    if (convPartnerId.equals(userId1)) {
                        currentUserId = userId2;
                        partnerId = userId1;
                    } else {
                        currentUserId = userId1;
                        partnerId = userId2;
                    }
                } else {
                    // Default: use userId1 as current user
                    currentUserId = userId1;
                    partnerId = userId2;
                }
            } else {
                // Current user is known, determine partner
                if (currentUserId.equals(userId1)) {
                    partnerId = userId2;
                } else {
                    partnerId = userId1;
                }
            }
            
            // Enrich the conversation
            EnrichedConversationResponse enriched = conversationEnrichmentService
                    .enrichSingleConversationSync(convNode, currentUserId, partnerId);
            
            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            log.error("Error enriching conversation: {}", e.getMessage(), e);
            // Fallback to direct proxy
            return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
        }
    }

    @PostMapping("/conversations/find-by-users")
    @AuthRequired
    public ResponseEntity<?> getConversationByTwoUsersUsePost(@RequestBody ConversationRequest request) {
        String userId1 = request.getUserId1();
        String userId2 = request.getUserId2();
        log.info("POST /api/v1/conversations/find-by-users?user1={}&user2={} - Proxying to Communication Service", userId1, userId2);
        String url = communicationServiceUrl + "/api/v1/conversations/find-by-users?user1=" + userId1 + "&user2=" + userId2;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Get messages for a conversation
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @AuthRequired
    public ResponseEntity<?> getMessages(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "sentAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("GET /api/v1/conversations/{}/messages - Proxying to Communication Service", conversationId);
        String url = communicationServiceUrl + "/api/v1/conversations/" + conversationId + "/messages" +
                "?userId=" + userId + "&page=" + page + "&size=" + size + "&sort=" + sort + "&direction=" + direction;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    // ============================================
    // Proposal Endpoints
    // ============================================

    /**
     * Create a new proposal
     */
    @PostMapping("/proposals")
    @AuthRequired
    public ResponseEntity<?> createProposal(@RequestBody Object request) {
        log.info("POST /api/v1/proposals - Proxying to Communication Service");
        String url = communicationServiceUrl + "/api/v1/proposals";
        return ResponseEntity.ok(restTemplate.postForObject(url, request, Object.class));
    }

    /**
     * Respond to a proposal
     */
    @PostMapping("/proposals/{proposalId}/respond")
    @AuthRequired
    public ResponseEntity<?> respondToProposal(
            @PathVariable String proposalId,
            @RequestParam String userId,
            @RequestBody Object request) {
        log.info("POST /api/v1/proposals/{}/respond - Proxying to Communication Service", proposalId);
        String url = communicationServiceUrl + "/api/v1/proposals/" + proposalId + "/respond?userId=" + userId;
        return ResponseEntity.ok(restTemplate.postForObject(url, request, Object.class));
    }

    /**
     * Get available proposal configs for roles
     */
    @GetMapping("/proposals/available-configs")
    @AuthRequired
    public ResponseEntity<?> getAvailableConfigs(@RequestParam java.util.List<String> roles) {
        log.info("GET /api/v1/proposals/available-configs - Proxying to Communication Service");
        String rolesParam = String.join(",", roles);
        String url = communicationServiceUrl + "/api/v1/proposals/available-configs?roles=" + rolesParam;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    // ============================================
    // Admin Proposal Config Endpoints
    // ============================================

    /**
     * Get all proposal configs (Admin only)
     */
    @GetMapping("/admin/proposals/configs")
    @AuthRequired
    public ResponseEntity<?> getAllProposalConfigs() {
        log.info("GET /api/v1/admin/proposals/configs - Proxying to Communication Service");
        String url = communicationServiceUrl + "/api/v1/admin/proposals/configs";
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Create a proposal config (Admin only)
     */
    @PostMapping("/admin/proposals/configs")
    @AuthRequired
    public ResponseEntity<?> createProposalConfig(@RequestBody Object request) {
        log.info("POST /api/v1/admin/proposals/configs - Proxying to Communication Service");
        String url = communicationServiceUrl + "/api/v1/admin/proposals/configs";
        return ResponseEntity.ok(restTemplate.postForObject(url, request, Object.class));
    }

    /**
     * Update a proposal config (Admin only)
     */
    @PutMapping("/admin/proposals/configs/{configId}")
    @AuthRequired
    public ResponseEntity<?> updateProposalConfig(
            @PathVariable String configId,
            @RequestBody Object request) {
        log.info("PUT /api/v1/admin/proposals/configs/{} - Proxying to Communication Service", configId);
        String url = communicationServiceUrl + "/api/v1/admin/proposals/configs/" + configId;
        restTemplate.put(url, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a proposal config (Admin only)
     */
    @DeleteMapping("/admin/proposals/configs/{configId}")
    @AuthRequired
    public ResponseEntity<?> deleteProposalConfig(@PathVariable String configId) {
        log.info("DELETE /api/v1/admin/proposals/configs/{} - Proxying to Communication Service", configId);
        String url = communicationServiceUrl + "/api/v1/admin/proposals/configs/" + configId;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }
}
