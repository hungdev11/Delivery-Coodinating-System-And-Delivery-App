package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.communicate.EnrichedConversationResponse;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.interfaces.IUserServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service để tổng hợp thông tin từ nhiều service cho Conversations
 * Gọi tuần tự: Communication Service -> User Service -> Session/Parcel Service
 */
@Slf4j
@Service
public class ConversationEnrichmentService {

    private final IUserServiceClient userServiceClient;
    private final WebClient communicationServiceWebClient;
    private final WebClient sessionServiceWebClient;
    private final WebClient parcelServiceWebClient;

    public ConversationEnrichmentService(
            IUserServiceClient userServiceClient,
            @Qualifier("communicationServiceWebClient") WebClient communicationServiceWebClient,
            @Qualifier("sessionServiceWebClient") WebClient sessionServiceWebClient,
            @Qualifier("parcelServiceWebClient") WebClient parcelServiceWebClient) {
        this.userServiceClient = userServiceClient;
        this.communicationServiceWebClient = communicationServiceWebClient;
        this.sessionServiceWebClient = sessionServiceWebClient;
        this.parcelServiceWebClient = parcelServiceWebClient;
    }

    /**
     * Lấy danh sách conversations với thông tin đầy đủ cho Android client
     * 
     * @param currentUserId ID của user hiện tại (từ header/token)
     * @return List of enriched conversations
     */
    public CompletableFuture<List<EnrichedConversationResponse>> getEnrichedConversations(String currentUserId) {
        log.info("🔄 Enriching conversations for user: {}", currentUserId);
        
        // Step 1: Get basic conversations from Communication Service
        return communicationServiceWebClient.get()
                .uri("/api/v1/conversations/user/" + currentUserId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .toFuture()
                .thenCompose(conversationsJson -> {
                    List<EnrichedConversationResponse> enrichedList = new ArrayList<>();
                    
                    if (conversationsJson == null || !conversationsJson.isArray()) {
                        log.warn("⚠️ No conversations found or invalid response");
                        return CompletableFuture.completedFuture(enrichedList);
                    }
                    
                    // Step 2: Collect all partner IDs for batch fetching
                    List<String> partnerIds = new ArrayList<>();
                    List<JsonNode> conversationNodes = new ArrayList<>();
                    
                    for (JsonNode convNode : conversationsJson) {
                        if (convNode.has("partnerId")) {
                            String partnerId = convNode.get("partnerId").asText();
                            partnerIds.add(partnerId);
                            conversationNodes.add(convNode);
                        }
                    }
                    
                    // Step 3: Batch fetch all users in parallel (instead of sequential)
                    List<CompletableFuture<UserDto>> userFutures = new ArrayList<>();
                    for (String partnerId : partnerIds) {
                        userFutures.add(userServiceClient.getUserById(partnerId));
                    }
                    
                    // Wait for all user fetches to complete
                    return CompletableFuture.allOf(userFutures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                // Create map of partnerId -> UserDto
                                java.util.Map<String, UserDto> userMap = new java.util.HashMap<>();
                                for (int i = 0; i < partnerIds.size(); i++) {
                                    try {
                                        UserDto userDto = userFutures.get(i).join();
                                        if (userDto != null) {
                                            userMap.put(partnerIds.get(i), userDto);
                                        }
                                    } catch (Exception e) {
                                        log.warn("⚠️ Failed to fetch user {}: {}", partnerIds.get(i), e.getMessage());
                                    }
                                }
                                
                                // Step 4: Build enriched conversations using cached user data
                                for (JsonNode convNode : conversationNodes) {
                                    try {
                                        String partnerId = convNode.get("partnerId").asText();
                                        UserDto userDto = userMap.get(partnerId);
                                        EnrichedConversationResponse enriched = buildEnrichedConversation(
                                                convNode, currentUserId, userDto
                                        );
                                        enrichedList.add(enriched);
                                    } catch (Exception e) {
                                        log.error("❌ Failed to enrich conversation: {}", e.getMessage());
                                    }
                                }
                                
                                return enrichedList;
                            });
                })
                .exceptionally(ex -> {
                    log.error("❌ Failed to get conversations: {}", ex.getMessage(), ex);
                    return new ArrayList<>();
                });
    }

    /**
     * Enrich một conversation với thông tin bổ sung (synchronous version for single conversation)
     */
    public EnrichedConversationResponse enrichSingleConversationSync(
            JsonNode convNode, String currentUserId, String partnerId) {
        try {
            // Fetch user synchronously for single conversation
            UserDto userDto = userServiceClient.getUserById(partnerId).get();
            return buildEnrichedConversation(convNode, currentUserId, userDto);
        } catch (Exception e) {
            log.error("Error enriching conversation synchronously: {}", e.getMessage(), e);
            // Return basic conversation without enrichment
            return EnrichedConversationResponse.builder()
                    .conversationId(convNode.has("conversationId") ? convNode.get("conversationId").asText() : "unknown")
                    .partnerId(partnerId)
                    .partnerName("Unknown User")
                    .build();
        }
    }

    /**
     * Enrich một conversation với thông tin bổ sung (async version)
     */
    private CompletableFuture<EnrichedConversationResponse> enrichSingleConversation(
            JsonNode convNode, String currentUserId) {
        
        try {
            // Parse basic conversation info
            String conversationId = convNode.get("conversationId").asText();
            String partnerId = convNode.get("partnerId").asText();
            String partnerName = convNode.has("partnerName") ? convNode.get("partnerName").asText() : null;
            String partnerUsername = convNode.has("partnerUsername") ? convNode.get("partnerUsername").asText() : null;
            String lastMessageTime = convNode.has("lastMessageTime") ? convNode.get("lastMessageTime").asText() : null;
            String lastMessageContent = convNode.has("lastMessageContent") ? convNode.get("lastMessageContent").asText() : null;
            Integer unreadCount = convNode.has("unreadCount") ? convNode.get("unreadCount").asInt() : 0;
            
            log.debug("📝 Enriching conversation: {} with partner: {}", conversationId, partnerId);
            
            // Step 2A: Get partner user info from User Service
            // ALWAYS fetch from User Service to get correct name (ignore fallback from Communication Service)
            return userServiceClient.getUserById(partnerId)
                    .thenCompose(userDto -> {
                        // Always use User Service data, not the fallback from Communication Service
                        String actualPartnerName = buildFullName(userDto);
                        String actualPartnerUsername = userDto != null ? userDto.getUsername() : partnerUsername;
                        
                        EnrichedConversationResponse.EnrichedConversationResponseBuilder builder = 
                                EnrichedConversationResponse.builder()
                                .conversationId(conversationId)
                                .partnerId(partnerId)
                                .partnerName(actualPartnerName) // Always use User Service name
                                .partnerUsername(actualPartnerUsername)
                                .partnerAvatar(null) // TODO: Add avatar when available
                                .lastMessageTime(lastMessageTime)
                                .lastMessageContent(lastMessageContent) // Pass through from Communication Service
                                .unreadCount(unreadCount != null ? unreadCount : 0); // Use unread count from Communication Service
                        
                        if (userDto != null) {
                            builder.partnerEmail(userDto.getEmail())
                                   .partnerPhone(userDto.getPhone())
                                   .partnerFirstName(userDto.getFirstName())
                                   .partnerLastName(userDto.getLastName());
                        }
                        
                        // Step 2B: Check if partner is shipper with active parcel for current user
                        return checkShipperParcelInfo(partnerId, currentUserId)
                                .thenApply(parcelInfo -> {
                                    if (parcelInfo != null) {
                                        builder.currentParcelId(parcelInfo.parcelId)
                                               .currentParcelCode(parcelInfo.parcelCode)
                                               .currentParcelStatus(parcelInfo.status);
                                    }
                                    return builder.build();
                                });
                    })
                    .exceptionally(ex -> {
                        log.warn("⚠️ Failed to fully enrich conversation {}: {}", conversationId, ex.getMessage());
                        // Return basic conversation without enrichment
                        return EnrichedConversationResponse.builder()
                                .conversationId(conversationId)
                                .partnerId(partnerId)
                                .partnerName(partnerName != null ? partnerName : "User")
                                .partnerUsername(partnerUsername)
                                .lastMessageTime(lastMessageTime)
                                .build();
                    });
                    
        } catch (Exception e) {
            log.error("❌ Error parsing conversation node: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    EnrichedConversationResponse.builder()
                            .conversationId("unknown")
                            .partnerId("unknown")
                            .partnerName("Error")
                            .build()
            );
        }
    }

    /**
     * Check if partner (shipper) has active parcel for current user (customer)
     * OR if current user (shipper) has active parcel for partner (customer)
     */
    private CompletableFuture<ParcelInfo> checkShipperParcelInfo(String partnerId, String currentUserId) {
        // TODO: Implement logic to check:
        // 1. If partner is shipper -> check if they have active delivery for currentUser
        // 2. If currentUser is shipper -> check if they have active delivery for partner
        // This requires querying Session Service for active assignments
        
        log.debug("🔍 Checking parcel info between {} and {}", partnerId, currentUserId);
        
        // For now, return null (no active parcel)
        // Will implement in next iteration
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Build enriched conversation response from conversation node and user DTO
     * This is used after batch fetching users to avoid N+1 queries
     */
    private EnrichedConversationResponse buildEnrichedConversation(
            JsonNode convNode, String currentUserId, UserDto userDto) {
        
        try {
            // Parse basic conversation info
            String conversationId = convNode.get("conversationId").asText();
            String partnerId = convNode.get("partnerId").asText();
            String partnerUsername = convNode.has("partnerUsername") ? convNode.get("partnerUsername").asText() : null;
            String lastMessageTime = convNode.has("lastMessageTime") ? convNode.get("lastMessageTime").asText() : null;
            String lastMessageContent = convNode.has("lastMessageContent") ? convNode.get("lastMessageContent").asText() : null;
            Integer unreadCount = convNode.has("unreadCount") ? convNode.get("unreadCount").asInt() : 0;
            
            // Always use User Service data, not the fallback from Communication Service
            String actualPartnerName = buildFullName(userDto);
            String actualPartnerUsername = userDto != null ? userDto.getUsername() : partnerUsername;
            
            EnrichedConversationResponse.EnrichedConversationResponseBuilder builder = 
                    EnrichedConversationResponse.builder()
                    .conversationId(conversationId)
                    .partnerId(partnerId)
                    .partnerName(actualPartnerName) // Always use User Service name
                    .partnerUsername(actualPartnerUsername)
                    .partnerAvatar(null) // TODO: Add avatar when available
                    .lastMessageTime(lastMessageTime)
                    .lastMessageContent(lastMessageContent) // Pass through from Communication Service
                    .unreadCount(unreadCount != null ? unreadCount : 0); // Use unread count from Communication Service
            
            if (userDto != null) {
                builder.partnerEmail(userDto.getEmail())
                       .partnerPhone(userDto.getPhone())
                       .partnerFirstName(userDto.getFirstName())
                       .partnerLastName(userDto.getLastName());
            }
            
            // Note: checkShipperParcelInfo is async and expensive, skip for now in batch mode
            // TODO: Implement batch parcel info check if needed
            
            return builder.build();
                    
        } catch (Exception e) {
            log.error("❌ Error building enriched conversation: {}", e.getMessage());
            return EnrichedConversationResponse.builder()
                    .conversationId("unknown")
                    .partnerId("unknown")
                    .partnerName("Error")
                    .build();
        }
    }

    private String buildFullName(UserDto userDto) {
        if (userDto == null) return "Unknown User";
        
        String firstName = userDto.getFirstName() != null ? userDto.getFirstName() : "";
        String lastName = userDto.getLastName() != null ? userDto.getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        
        return fullName.isEmpty() ? userDto.getUsername() : fullName;
    }

    /**
     * Helper class để lưu thông tin parcel
     */
    private static class ParcelInfo {
        String parcelId;
        String parcelCode;
        String status;
    }
}
