package com.ds.gateway.application.controllers.v1;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.security.UserContext;
import com.ds.gateway.business.v1.services.ConversationEnrichmentService;
import com.ds.gateway.common.entities.dto.communicate.ConversationRequest;
import com.ds.gateway.common.entities.dto.communicate.EnrichedConversationResponse;
import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.infrastructure.http.ProxyHttpClient;
import com.ds.gateway.infrastructure.logging.ProxyLogContext;
import com.ds.gateway.infrastructure.logging.ProxyRequestLogger;
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

    private static final String COMMUNICATION_SERVICE = "communication-service";

    private final ProxyHttpClient proxyHttpClient;
    private final ProxyRequestLogger proxyRequestLogger;
    private final ConversationEnrichmentService conversationEnrichmentService;
    private final ObjectMapper objectMapper;

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
        log.debug("[api-gateway] [CommunicationController.getMyConversations] GET /api/v1/conversations/user/{} - Proxying to Communication Service", userId);
        return proxyCommunication(HttpMethod.GET, "/api/v1/conversations/user/" + userId, null);
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
    public CompletableFuture<ResponseEntity<BaseResponse<List<EnrichedConversationResponse>>>> getEnrichedConversations(
            @RequestParam String userId) {
        log.debug("[api-gateway] [CommunicationController.getEnrichedConversations] GET /api/v1/conversations?userId={} - Getting enriched conversations for Android", userId);
        
        return conversationEnrichmentService.getEnrichedConversations(userId)
                .thenApply(conversations -> ResponseEntity.ok(BaseResponse.success(conversations)))
                .exceptionally(ex -> {
                    log.error("[api-gateway] [CommunicationController.getEnrichedConversations] Failed to get enriched conversations", ex);
                    return ResponseEntity.internalServerError().body(BaseResponse.error("Không thể tải danh sách hội thoại"));
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
            @RequestParam("user2") String userId2) {
        log.debug("[api-gateway] [CommunicationController.getConversationByTwoUsers] GET /api/v1/conversations/find-by-users?user1={}&user2={} - Enriching response", userId1, userId2);
        
        // Get current user from UserContext (JWT token)
        String currentUserId = UserContext.getCurrentUser()
            .map(user -> user.getUserId())
            .orElse(null);
        
        // Get basic conversation from Communication Service
        String path = "/api/v1/conversations/find-by-users?user1=" + userId1 + "&user2=" + userId2;
        try {
            ResponseEntity<Object> baseResponse = proxyCommunication(HttpMethod.GET, path, null);
            if (!baseResponse.getStatusCode().is2xxSuccessful()) {
                return baseResponse;
            }
            Object conversationObj = baseResponse.getBody();
            if (conversationObj == null) {
                return ResponseEntity.notFound().build();
            }
            JsonNode convNode = objectMapper.valueToTree(conversationObj);
            if (convNode.has("result")) {
                convNode = convNode.get("result");
            }
            if (convNode == null || convNode.isNull()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine current user and partner
            // Use UserContext first, then try to determine from conversation data
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
            
            return ResponseEntity.ok(BaseResponse.success(enriched));
        } catch (Exception e) {
            log.error("[api-gateway] [CommunicationController.getConversationByTwoUsers] Error enriching conversation", e);
            return proxyCommunication(HttpMethod.GET, path, null);
        }
    }

    @PostMapping("/conversations/find-by-users")
    @AuthRequired
    public ResponseEntity<?> getConversationByTwoUsersUsePost(@RequestBody ConversationRequest request) {
        String userId1 = request.getUserId1();
        String userId2 = request.getUserId2();
        log.debug("[api-gateway] [CommunicationController.getConversationByTwoUsersUsePost] POST /api/v1/conversations/find-by-users?user1={}&user2={} - Proxying to Communication Service", userId1, userId2);
        return proxyCommunication(HttpMethod.GET, "/api/v1/conversations/find-by-users?user1=" + userId1 + "&user2=" + userId2, null);
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
        log.debug("[api-gateway] [CommunicationController.getMessages] GET /api/v1/conversations/{}/messages - Proxying to Communication Service", conversationId);
        String path = "/api/v1/conversations/" + conversationId + "/messages" +
                "?userId=" + userId + "&page=" + page + "&size=" + size + "&sort=" + sort + "&direction=" + direction;
        return proxyCommunication(HttpMethod.GET, path, null);
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
        log.debug("[api-gateway] [CommunicationController.createProposal] POST /api/v1/proposals - Proxying to Communication Service");
        return proxyCommunication(HttpMethod.POST, "/api/v1/proposals", request);
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
        log.debug("[api-gateway] [CommunicationController.respondToProposal] POST /api/v1/proposals/{}/respond - Proxying to Communication Service", proposalId);
        return proxyCommunication(HttpMethod.POST, "/api/v1/proposals/" + proposalId + "/respond?userId=" + userId, request);
    }

    /**
     * Get available proposal configs for roles
     */
    @GetMapping("/proposals/available-configs")
    @AuthRequired
    public ResponseEntity<?> getAvailableConfigs(@RequestParam java.util.List<String> roles) {
        log.debug("[api-gateway] [CommunicationController.getAvailableConfigs] GET /api/v1/proposals/available-configs - Proxying to Communication Service");
        String rolesParam = String.join(",", roles);
        return proxyCommunication(HttpMethod.GET, "/api/v1/proposals/available-configs?roles=" + rolesParam, null);
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
        log.debug("[api-gateway] [CommunicationController.getAllProposalConfigs] GET /api/v1/admin/proposals/configs - Proxying to Communication Service");
        return proxyCommunication(HttpMethod.GET, "/api/v1/admin/proposals/configs", null);
    }

    /**
     * Create a proposal config (Admin only)
     */
    @PostMapping("/admin/proposals/configs")
    @AuthRequired
    public ResponseEntity<?> createProposalConfig(@RequestBody Object request) {
        log.debug("[api-gateway] [CommunicationController.createProposalConfig] POST /api/v1/admin/proposals/configs - Proxying to Communication Service");
        return proxyCommunication(HttpMethod.POST, "/api/v1/admin/proposals/configs", request);
    }

    /**
     * Update a proposal config (Admin only)
     */
    @PutMapping("/admin/proposals/configs/{configId}")
    @AuthRequired
    public ResponseEntity<?> updateProposalConfig(
            @PathVariable String configId,
            @RequestBody Object request) {
        log.debug("[api-gateway] [CommunicationController.updateProposalConfig] PUT /api/v1/admin/proposals/configs/{} - Proxying to Communication Service", configId);
        return proxyCommunication(HttpMethod.PUT, "/api/v1/admin/proposals/configs/" + configId, request);
    }

    /**
     * Delete a proposal config (Admin only)
     */
    @DeleteMapping("/admin/proposals/configs/{configId}")
    @AuthRequired
    public ResponseEntity<?> deleteProposalConfig(@PathVariable String configId) {
        log.debug("[api-gateway] [CommunicationController.deleteProposalConfig] DELETE /api/v1/admin/proposals/configs/{} - Proxying to Communication Service", configId);
        return proxyCommunication(HttpMethod.DELETE, "/api/v1/admin/proposals/configs/" + configId, null);
    }

    // ============================================
    // Message Endpoints (Direct Proxy)
    // ============================================

    /**
     * Send a new message
     */
    @PostMapping("/messages")
    @AuthRequired
    public ResponseEntity<?> sendMessage(@RequestBody JsonNode requestBody) {
        log.debug("[api-gateway] [CommunicationController.sendMessage] POST /api/v1/messages - Proxying to Communication Service");
        return proxyCommunication(HttpMethod.POST, "/api/v1/messages", requestBody);
    }

    /**
     * Update message status (e.g., READ)
     */
    @PutMapping("/messages/{messageId}/status")
    @AuthRequired
    public ResponseEntity<?> updateMessageStatus(@PathVariable String messageId, @RequestBody JsonNode requestBody) {
        log.debug("[api-gateway] [CommunicationController.updateMessageStatus] PUT /api/v1/messages/{}/status - Proxying to Communication Service", messageId);
        return proxyCommunication(HttpMethod.PUT, "/api/v1/messages/" + messageId + "/status", requestBody);
    }

    private ResponseEntity<Object> proxyCommunication(HttpMethod method, String path, Object body) {
        String url = communicationServiceUrl + path;
        ProxyLogContext context = proxyRequestLogger.start(method, COMMUNICATION_SERVICE, url, body);
        try {
            ResponseEntity<Object> response = proxyHttpClient.exchange(method, url, body, Object.class);
            proxyRequestLogger.success(context, response.getStatusCodeValue());
            return response;
        } catch (ResourceAccessException e) {
            proxyRequestLogger.failure(context, 502, e.getMessage(), e);
            return ResponseEntity.status(502).body("{\"error\":\"Bad Gateway: Communication Service unavailable\"}");
        } catch (HttpStatusCodeException e) {
            proxyRequestLogger.failure(context, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            proxyRequestLogger.failure(context, 500, e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }
}
