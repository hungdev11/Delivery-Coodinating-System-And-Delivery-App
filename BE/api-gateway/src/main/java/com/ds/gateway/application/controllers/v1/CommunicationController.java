package com.ds.gateway.application.controllers.v1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.ds.gateway.annotations.AuthRequired;

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

    @Value("${services.communication.base-url}")
    private String communicationServiceUrl;

    // ============================================
    // Conversation Endpoints
    // ============================================

    /**
     * Get conversations for a user
     */
    @GetMapping("/conversations/user/{userId}")
    @AuthRequired
    public ResponseEntity<?> getMyConversations(@PathVariable String userId) {
        log.info("GET /api/v1/conversations/user/{} - Proxying to Communication Service", userId);
        String url = communicationServiceUrl + "/api/v1/conversations/user/" + userId;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Find or create conversation between two users
     */
    @GetMapping("/conversations/find-by-users")
    @AuthRequired
    public ResponseEntity<?> getConversationByTwoUsers(
            @RequestParam("user1") String userId1,
            @RequestParam("user2") String userId2) {
        log.info("GET /api/v1/conversations/find-by-users?user1={}&user2={} - Proxying to Communication Service", userId1, userId2);
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
