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
     * Seed a single address for a user
     * Creates address in zone-service, then creates UserAddress with specified tag and isPrimary
     */
    public void seedAddress(String userId, KeycloakInitConfig.AddressConfig addressConfig, boolean isPrimary) {
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

            // If setting as primary, check if user already has a primary address
            if (isPrimary) {
                Optional<UserAddress> existingPrimary = userAddressRepository.findByUserIdAndIsPrimaryTrue(userId);
                if (existingPrimary.isPresent()) {
                    log.info("✓ User '{}' already has a primary address. Skipping this address.", user.getUsername());
                    return;
                }
            }

            // Create address in zone-service
            // Use addressText as name/title for the address
            String addressText = addressConfig.getAddressText();
            if (addressText == null || addressText.isBlank()) {
                log.warn("⚠️ Address text is missing for user: {}. Skipping address seeding.", userId);
                return;
            }
            
            Map<String, Object> createAddressRequest = new HashMap<>();
            createAddressRequest.put("name", addressText); // Use addressText as name
            createAddressRequest.put("addressText", addressText);
            createAddressRequest.put("lat", addressConfig.getLat());
            createAddressRequest.put("lon", addressConfig.getLon());

            log.debug("🔗 Creating address in zone-service for user '{}' (id: {})", user.getUsername(), userId);
            log.debug("   Address: {} at ({}, {})", addressText, addressConfig.getLat(), addressConfig.getLon());

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
            log.debug("✓ Address created in zone-service: {}", destinationId);

            // Determine tag
            String tag = addressConfig.getTag();
            if (tag == null || tag.isBlank()) {
                // Auto-detect tag from name
                String nameLower = addressName.toLowerCase();
                if (nameLower.contains("primary") || nameLower.contains("home")) {
                    tag = "Home";
                } else if (nameLower.contains("office") || nameLower.contains("work") || nameLower.contains("company")) {
                    tag = "Company";
                } else if (nameLower.contains("secondary")) {
                    tag = "Other";
                } else {
                    tag = "Home"; // Default
                }
            }

            // If setting as primary, unset other primary addresses
            if (isPrimary) {
                userAddressRepository.setAllNonPrimaryByUserId(userId);
            }

            // Create UserAddress
            UserAddress userAddress = UserAddress.builder()
                    .userId(userId)
                    .destinationId(destinationId)
                    .note(addressConfig.getAddressText())
                    .tag(tag)
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
     * Seed primary address for a user (backward compatibility)
     * Creates address in zone-service, then creates UserAddress with isPrimary=true
     */
    public void seedPrimaryAddress(String userId, KeycloakInitConfig.AddressConfig addressConfig) {
        seedAddress(userId, addressConfig, true);
    }

    /**
     * Seed addresses for shop and client users
     * Supports both single address (backward compatibility) and multiple addresses
     */
    public void seedPrimaryAddressesForUsers(KeycloakInitConfig.RealmConfig realmConfig) {
        if (realmConfig == null || realmConfig.getUsers() == null) {
            return;
        }

        log.info("🌱 Seeding addresses for shop and client users...");

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        int addressCount = 0;

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
            
            // Check if user already has addresses
            List<UserAddress> existingAddresses = userAddressRepository.findByUserId(user.getId());
            if (!existingAddresses.isEmpty()) {
                log.info("✓ User '{}' already has {} address(es). Skipping address seeding.", 
                        user.getUsername(), existingAddresses.size());
                skipCount++;
                continue;
            }

            // Support multiple addresses (new format)
            List<KeycloakInitConfig.AddressConfig> addressesToSeed = userConfig.getAddresses();
            if (addressesToSeed != null && !addressesToSeed.isEmpty()) {
                try {
                    boolean hasPrimary = false;
                    for (int i = 0; i < addressesToSeed.size(); i++) {
                        KeycloakInitConfig.AddressConfig addressConfig = addressesToSeed.get(i);
                        
                        if (addressConfig == null || addressConfig.getLat() == null || addressConfig.getLon() == null) {
                            log.warn("⚠️ Address config {} is incomplete for user '{}'. Skipping.", 
                                    i + 1, userConfig.getUsername());
                            continue;
                        }

                        // First address is primary if not specified
                        boolean isPrimary = addressConfig.getIsPrimary() != null 
                                ? addressConfig.getIsPrimary() 
                                : (i == 0 && !hasPrimary);
                        
                        if (isPrimary) {
                            hasPrimary = true;
                        }

                        seedAddress(user.getId(), addressConfig, isPrimary);
                        addressCount++;
                    }
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ Failed to seed addresses for user '{}': {}", 
                            userConfig.getUsername(), e.getMessage(), e);
                    failCount++;
                }
            } 
            // Backward compatibility: single address
            else if (userConfig.getAddress() != null) {
                KeycloakInitConfig.AddressConfig addressConfig = userConfig.getAddress();
                
                if (addressConfig.getLat() == null || addressConfig.getLon() == null) {
                    log.warn("⚠️ Address config is missing for user '{}'. Skipping address seeding.", userConfig.getUsername());
                    skipCount++;
                    continue;
                }

                try {
                    seedPrimaryAddress(user.getId(), addressConfig);
                    addressCount++;
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ Failed to seed primary address for user '{}': {}", 
                            userConfig.getUsername(), e.getMessage(), e);
                    failCount++;
                }
            } else {
                log.warn("⚠️ No address config found for user '{}'. Skipping address seeding.", userConfig.getUsername());
                skipCount++;
            }
        }

        log.info("✓ Address seeding completed: {} users successful, {} failed, {} skipped, {} addresses created", 
                successCount, failCount, skipCount, addressCount);
    }
}
