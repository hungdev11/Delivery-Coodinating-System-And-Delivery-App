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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * Seed an address for a user
     * Creates address in zone-service, then creates UserAddress
     */
    public void seedAddress(String userId, KeycloakInitConfig.AddressConfig addressConfig, String tag, boolean isPrimary) {
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

            // Check if this specific address already exists (by tag and isPrimary)
            if (isPrimary) {
                Optional<UserAddress> existingPrimary = userAddressRepository.findByUserIdAndIsPrimaryTrue(userId);
                if (existingPrimary.isPresent()) {
                    log.info("✓ User '{}' already has a primary address. Skipping primary address seeding.", user.getUsername());
                    return;
                }
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

            // Create UserAddress
            UserAddress userAddress = UserAddress.builder()
                    .userId(userId)
                    .destinationId(destinationId)
                    .note(addressConfig.getAddressText())
                    .tag(tag != null ? tag : "Primary")
                    .isPrimary(isPrimary)
                    .build();

            UserAddress saved = userAddressRepository.save(userAddress);
            log.info("✓ Address created for user '{}' (id: {}, addressId: {}, tag: {}, isPrimary: {})", 
                    user.getUsername(), userId, saved.getId(), tag, isPrimary);

        } catch (WebClientResponseException e) {
            log.error("❌ Failed to create address in zone-service for user '{}': HTTP {} - {}", 
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("❌ Failed to seed address for user '{}': {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Seed primary address for a user (convenience method)
     */
    public void seedPrimaryAddress(String userId, KeycloakInitConfig.AddressConfig addressConfig) {
        seedAddress(userId, addressConfig, "Primary", true);
    }

    /**
     * Seed addresses for shop and client users
     * Supports both single 'address' and multiple 'addresses'
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
            
            // Handle multiple addresses (for clients)
            List<KeycloakInitConfig.AddressConfig> addressesToSeed = new ArrayList<>();
            
            // Add single address if present
            KeycloakInitConfig.AddressConfig singleAddress = userConfig.getAddress();
            if (singleAddress != null && singleAddress.getLat() != null && singleAddress.getLon() != null) {
                addressesToSeed.add(singleAddress);
            }
            
            // Add multiple addresses if present
            List<KeycloakInitConfig.AddressConfig> multipleAddresses = userConfig.getAddresses();
            if (multipleAddresses != null && !multipleAddresses.isEmpty()) {
                addressesToSeed.addAll(multipleAddresses);
            }
            
            if (addressesToSeed.isEmpty()) {
                log.warn("⚠️ No address config found for user '{}'. Skipping address seeding.", userConfig.getUsername());
                skipCount++;
                continue;
            }

            // Seed all addresses
            for (int i = 0; i < addressesToSeed.size(); i++) {
                KeycloakInitConfig.AddressConfig addressConfig = addressesToSeed.get(i);
                try {
                    // Use tag and isPrimary from config if provided, otherwise infer
                    String tag = addressConfig.getTag() != null ? addressConfig.getTag() :
                                (addressConfig.getName() != null && addressConfig.getName().contains("Home")) ? "Home" :
                                (addressConfig.getName() != null && addressConfig.getName().contains("Company")) ? "Company" :
                                (addressConfig.getName() != null && addressConfig.getName().contains("Other")) ? "Other" :
                                (i == 0) ? "Primary" : "Other";
                    
                    boolean isPrimary = addressConfig.getIsPrimary() != null ? addressConfig.getIsPrimary() :
                                       (i == 0 && singleAddress != null) || 
                                       (multipleAddresses != null && i == 0 && singleAddress == null);
                    
                    seedAddress(user.getId(), addressConfig, tag, isPrimary);
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ Failed to seed address {} for user '{}': {}", 
                            i + 1, userConfig.getUsername(), e.getMessage(), e);
                    failCount++;
                }
            }
        }

        log.info("✓ Address seeding completed: {} successful, {} failed, {} skipped", 
                successCount, failCount, skipCount);
    }
}
