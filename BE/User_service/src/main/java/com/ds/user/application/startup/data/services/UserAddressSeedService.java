package com.ds.user.application.startup.data.services;

import com.ds.user.app_context.repositories.UserAddressRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.UserAddress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for seeding primary addresses for users (shop and client)
 */
@Slf4j
@Service
public class UserAddressSeedService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final WebClient zoneServiceWebClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public UserAddressSeedService(
            UserRepository userRepository,
            UserAddressRepository userAddressRepository,
            @Qualifier("zoneServiceWebClient") WebClient zoneServiceWebClient) {
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.zoneServiceWebClient = zoneServiceWebClient;
    }

    /**
     * Seed primary address for a user
     * Creates address in zone-service, then creates UserAddress with isPrimary=true
     */
    public void seedPrimaryAddress(String userId, KeycloakInitConfig.AddressConfig addressConfig) {
        if (addressConfig == null || addressConfig.getLat() == null || addressConfig.getLon() == null) {
            log.warn("⚠️ Address config is missing or incomplete for user: {}. Skipping address seeding.", userId);
            return;
        }

        try {
            // Check if user exists
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("⚠️ User not found: {}. Skipping address seeding.", userId);
                return;
            }

            User user = userOpt.get();

            // Check if user already has a primary address
            Optional<UserAddress> existingPrimary = userAddressRepository.findByUserIdAndIsPrimaryTrue(userId);
            if (existingPrimary.isPresent()) {
                log.info("✓ User '{}' already has a primary address. Skipping address seeding.", user.getUsername());
                return;
            }

            // Create address in zone-service
            String addressName = addressConfig.getName() != null && !addressConfig.getName().isBlank()
                    ? addressConfig.getName()
                    : String.format("%s %s - Primary Address", user.getFirstName(), user.getLastName());
            
            Map<String, Object> createAddressRequest = new HashMap<>();
            createAddressRequest.put("name", addressName);
            createAddressRequest.put("lat", addressConfig.getLat());
            createAddressRequest.put("lon", addressConfig.getLon());
            if (addressConfig.getAddressText() != null && !addressConfig.getAddressText().isBlank()) {
                createAddressRequest.put("addressText", addressConfig.getAddressText());
            }

            log.info("🔗 Creating address in zone-service for user '{}' (id: {})", user.getUsername(), userId);
            log.info("   Address: {} at ({}, {})", addressName, addressConfig.getLat(), addressConfig.getLon());

            // Call zone-service to create or get address
            String responseBody = zoneServiceWebClient.post()
                    .uri("/api/v1/addresses/get-or-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createAddressRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                log.error("❌ Failed to create address in zone-service for user '{}': Empty response", user.getUsername());
                return;
            }

            // Parse response to get address ID
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode resultNode = responseJson.get("result");
            if (resultNode == null || !resultNode.has("id")) {
                log.error("❌ Failed to create address in zone-service for user '{}': Invalid response format", user.getUsername());
                log.error("   Response: {}", responseBody);
                return;
            }

            String destinationId = resultNode.get("id").asText();
            log.info("✓ Address created in zone-service: {}", destinationId);

            // Create UserAddress with isPrimary=true
            UserAddress userAddress = UserAddress.builder()
                    .userId(userId)
                    .destinationId(destinationId)
                    .note(addressConfig.getAddressText())
                    .tag("Primary")
                    .isPrimary(true)
                    .build();

            UserAddress saved = userAddressRepository.save(userAddress);
            log.info("✓ Primary address created for user '{}' (id: {}, addressId: {})", 
                    user.getUsername(), userId, saved.getId());

        } catch (WebClientResponseException e) {
            log.error("❌ Failed to create address in zone-service for user '{}': HTTP {} - {}", 
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ Failed to seed primary address for user '{}': {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Seed primary addresses for shop and client users
     */
    public void seedPrimaryAddressesForUsers(KeycloakInitConfig.RealmConfig realmConfig) {
        if (realmConfig == null || realmConfig.getUsers() == null) {
            return;
        }

        log.info("🌱 Seeding primary addresses for shop and client users...");

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (KeycloakInitConfig.UserConfig userConfig : realmConfig.getUsers()) {
            // Only seed addresses for SHOP and CLIENT users
            boolean isShop = userConfig.getRealmRoles() != null && 
                    userConfig.getRealmRoles().contains("SHOP");
            boolean isClient = userConfig.getRealmRoles() != null && 
                    userConfig.getRealmRoles().contains("CLIENT");

            if (!isShop && !isClient) {
                continue;
            }

            // Find user by username to get userId
            Optional<User> userOpt = userRepository.findByUsername(userConfig.getUsername());
            if (userOpt.isEmpty()) {
                log.warn("⚠️ User not found by username: {}. Skipping address seeding.", userConfig.getUsername());
                skipCount++;
                continue;
            }

            User user = userOpt.get();
            KeycloakInitConfig.AddressConfig addressConfig = userConfig.getAddress();

            if (addressConfig == null || addressConfig.getLat() == null || addressConfig.getLon() == null) {
                log.warn("⚠️ Address config is missing for user '{}'. Skipping address seeding.", userConfig.getUsername());
                skipCount++;
                continue;
            }

            try {
                seedPrimaryAddress(user.getId(), addressConfig);
                successCount++;
            } catch (Exception e) {
                log.error("❌ Failed to seed primary address for user '{}': {}", 
                        userConfig.getUsername(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("✓ Address seeding completed: {} successful, {} failed, {} skipped", 
                successCount, failCount, skipCount);
    }
}
