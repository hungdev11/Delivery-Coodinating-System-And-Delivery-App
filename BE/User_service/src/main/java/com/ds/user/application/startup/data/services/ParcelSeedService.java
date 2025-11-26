package com.ds.user.application.startup.data.services;

import com.ds.user.app_context.repositories.UserAddressRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.UserAddress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for seeding parcels (orders) for testing
 * Randomly selects shops and clients, uses their primary addresses to create
 * parcels
 */
@Slf4j
@Service
public class ParcelSeedService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final WebClient zoneServiceWebClient;
    private final WebClient parcelServiceWebClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${parcel.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${parcel.seed.count:20}")
    private int seedCount;

    public ParcelSeedService(
            UserRepository userRepository,
            UserAddressRepository userAddressRepository,
            @Qualifier("zoneServiceWebClient") WebClient zoneServiceWebClient,
            @Qualifier("parcelServiceWebClient") WebClient parcelServiceWebClient) {
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.zoneServiceWebClient = zoneServiceWebClient;
        this.parcelServiceWebClient = parcelServiceWebClient;
    }

    /**
     * Seed parcels for shops and clients (startup - uses config)
     * Randomly selects shops and clients, uses their primary addresses
     * Used during application startup - no authentication needed
     */
    public void seedParcels() {
        if (!seedEnabled) {
            log.debug("Parcel seeding is DISABLED (parcel.seed.enabled=false)");
            return;
        }
        seedParcels(seedCount, null);
    }

    /**
     * Seed parcels for shops and clients
     * Randomly selects shops and clients, uses their primary addresses
     * 
     * @param count         number of parcels to create
     * @param authorization optional Authorization header token (for API calls)
     * @return result with success/fail counts
     */
    public ParcelSeedService.SeedParcelsResult seedParcels(int count, String authorization) {
        return seedParcelsWithSelection(count, null, null, authorization);
    }

    /**
     * Seed parcels with optional shop/client selection
     * 
     * @param count         number of parcels to create
     * @param shopId        optional shop ID (if null, randomly selects)
     * @param clientId      optional client ID (if null, randomly selects)
     * @param authorization optional Authorization header token (for API calls)
     * @return result with success/fail counts
     */
    public ParcelSeedService.SeedParcelsResult seedParcelsWithSelection(int count, String shopId, String clientId,
            String authorization) {
        log.debug("Starting parcel seeding...");
        log.debug("   Target count: {} parcels", count);
        if (shopId != null) {
            log.debug("   Using shop: {}", shopId);
        }
        if (clientId != null) {
            log.debug("   Using client: {}", clientId);
        }

        try {
            // Get all shops and clients with their primary addresses
            List<User> shops = userRepository.findAll().stream()
                    .filter(user -> {
                        // Check if user has SHOP role by querying Keycloak would be complex
                        // For now, we'll use a simpler approach: check username pattern or all users
                        // In production, you might want to fetch roles from Keycloak
                        return user.getUsername() != null && user.getUsername().startsWith("shop");
                    })
                    .collect(Collectors.toList());

            List<User> clients = userRepository.findAll().stream()
                    .filter(user -> {
                        return user.getUsername() != null && user.getUsername().startsWith("client");
                    })
                    .collect(Collectors.toList());

            if (shops.isEmpty() || clients.isEmpty()) {
                log.debug("[user-service] [ParcelSeedService.seedParcels] Cannot seed parcels: No shops or clients found");
                log.debug("[user-service] [ParcelSeedService.seedParcels]    Shops: {}, Clients: {}", shops.size(), clients.size());
                return new SeedParcelsResult(0, count, count);
            }

            log.debug("   Found {} shops and {} clients", shops.size(), clients.size());

            // Get primary addresses for shops and clients
            Map<String, String> shopAddresses = new HashMap<>();
            Map<String, String> clientAddresses = new HashMap<>();

            for (User shop : shops) {
                Optional<UserAddress> primaryAddress = userAddressRepository.findByUserIdAndIsPrimaryTrue(shop.getId());
                if (primaryAddress.isPresent()) {
                    shopAddresses.put(shop.getId(), primaryAddress.get().getDestinationId());
                }
            }

            for (User client : clients) {
                Optional<UserAddress> primaryAddress = userAddressRepository
                        .findByUserIdAndIsPrimaryTrue(client.getId());
                if (primaryAddress.isPresent()) {
                    clientAddresses.put(client.getId(), primaryAddress.get().getDestinationId());
                }
            }

            if (shopAddresses.isEmpty() || clientAddresses.isEmpty()) {
                log.debug("[user-service] [ParcelSeedService.seedParcels] Cannot seed parcels: Shops or clients missing primary addresses");
                log.debug("[user-service] [ParcelSeedService.seedParcels]    Shops with addresses: {}, Clients with addresses: {}",
                        shopAddresses.size(), clientAddresses.size());
                return new SeedParcelsResult(0, count, count);
            }

            log.debug("   Shops with primary addresses: {}", shopAddresses.size());
            log.debug("   Clients with primary addresses: {}", clientAddresses.size());

            // Get address details for shop and client addresses
            Map<String, AddressInfo> addressInfoMap = new HashMap<>();

            Set<String> allDestinationIds = new HashSet<>();
            allDestinationIds.addAll(shopAddresses.values());
            allDestinationIds.addAll(clientAddresses.values());

            for (String destinationId : allDestinationIds) {
                try {
                    AddressInfo info = getAddressInfo(destinationId);
                    if (info != null) {
                        addressInfoMap.put(destinationId, info);
                    }
                } catch (Exception e) {
                    log.debug("[user-service] [ParcelSeedService.seedParcels] Failed to get address info for {}: {}", destinationId, e.getMessage());
                }
            }

            log.debug("   Retrieved address details for {} addresses", addressInfoMap.size());

            // Validate specific shop/client if provided
            if (shopId != null && !shopAddresses.containsKey(shopId)) {
                log.error("[user-service] [ParcelSeedService.seedParcels] Shop ID {} not found or missing primary address", shopId);
                return new SeedParcelsResult(0, 0, count);
            }
            if (clientId != null && !clientAddresses.containsKey(clientId)) {
                log.error("[user-service] [ParcelSeedService.seedParcels] Client ID {} not found or missing primary address", clientId);
                return new SeedParcelsResult(0, 0, count);
            }

            // Create parcels
            Random random = new Random();
            List<String> shopIds = new ArrayList<>(shopAddresses.keySet());
            List<String> clientIds = new ArrayList<>(clientAddresses.keySet());

            String[] deliveryTypes = { "NORMAL", "EXPRESS", "FAST", "URGENT", "ECONOMY" };
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < count; i++) {
                Map<String, Object> parcelRequest = null;
                String code = null;
                try {
                    // Select shop and client (use provided or randomly select)
                    String selectedShopId = shopId != null ? shopId : shopIds.get(random.nextInt(shopIds.size()));
                    String selectedClientId = clientId != null ? clientId
                            : clientIds.get(random.nextInt(clientIds.size()));

                    String senderDestinationId = shopAddresses.get(selectedShopId);
                    String receiverDestinationId = clientAddresses.get(selectedClientId);

                    if (senderDestinationId == null || receiverDestinationId == null) {
                        log.debug("[user-service] [ParcelSeedService.seedParcels] Skipping parcel {}: Missing address", i + 1);
                        failCount++;
                        continue;
                    }

                    AddressInfo senderAddress = addressInfoMap.get(senderDestinationId);
                    AddressInfo receiverAddress = addressInfoMap.get(receiverDestinationId);

                    if (senderAddress == null || receiverAddress == null) {
                        log.debug("[user-service] [ParcelSeedService.seedParcels] Skipping parcel {}: Missing address info", i + 1);
                        failCount++;
                        continue;
                    }

                    // Generate parcel code (use timestamp to avoid duplicates)
                    code = "PARCEL-" + System.currentTimeMillis() + "-" + String.format("%03d", i + 1);

                    // Random delivery type and weight/value
                    String deliveryType = deliveryTypes[random.nextInt(deliveryTypes.length)];
                    double weight = 0.5 + random.nextDouble() * 9.5; // 0.5 - 10 kg
                    BigDecimal value = BigDecimal.valueOf(10000 + random.nextInt(990000)); // 10k - 1M VND

                    // Create parcel
                    parcelRequest = new HashMap<>();
                    parcelRequest.put("code", code);
                    parcelRequest.put("senderId", selectedShopId);
                    parcelRequest.put("receiverId", selectedClientId);
                    parcelRequest.put("deliveryType", deliveryType);
                    parcelRequest.put("receiveFrom", senderAddress.name);
                    parcelRequest.put("sendTo", receiverAddress.name);
                    parcelRequest.put("weight", weight);
                    parcelRequest.put("value", value);
                    parcelRequest.put("senderDestinationId", senderDestinationId);
                    parcelRequest.put("receiverDestinationId", receiverDestinationId);

                    // Build request with optional authorization header
                    var requestSpec = parcelServiceWebClient.post()
                            .uri("/api/v1/parcels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(parcelRequest);

                    // Add authorization header if provided (from web UI request)
                    if (authorization != null && !authorization.isBlank()) {
                        requestSpec = requestSpec.header("Authorization", authorization);
                    }

                    String responseBody = requestSpec
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

                    if (responseBody != null && !responseBody.isBlank()) {
                        JsonNode responseJson = objectMapper.readTree(responseBody);
                        // Check if response has "success" field (wrapped response) or "id" field
                        // (direct ParcelResponse)
                        boolean isSuccess = (responseJson.has("success") && responseJson.get("success").asBoolean())
                                || responseJson.has("id"); // ParcelResponse has "id" field if successful

                        if (isSuccess) {
                            successCount++;
                            if ((i + 1) % 5 == 0) {
                                log.debug("   Created {}/{} parcels...", i + 1, count);
                            }
                        } else {
                            failCount++;
                            log.debug("[user-service] [ParcelSeedService.seedParcels] Failed to create parcel {}: {}", code, responseBody);
                        }
                    } else {
                        failCount++;
                        log.debug("[user-service] [ParcelSeedService.seedParcels] Failed to create parcel {}: Empty response", code);
                    }

                } catch (WebClientResponseException e) {
                    failCount++;
                    String errorBody = e.getResponseBodyAsString();
                    log.error("[user-service] [ParcelSeedService.seedParcels] Failed to create parcel {}: HTTP {} - Status: {}, Body: {}",
                            code != null ? code : (i + 1), e.getStatusCode(), e.getStatusCode().value(), errorBody);
                    if ((i + 1) == 1 && parcelRequest != null) {
                        // Log detailed error for first failure to help debug
                        log.error("[user-service] [ParcelSeedService.seedParcels]    First failure details - Request: {}", parcelRequest);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("[user-service] [ParcelSeedService.seedParcels] Failed to create parcel {}", code != null ? code : (i + 1), e);
                    if ((i + 1) == 1 && parcelRequest != null) {
                        log.error("[user-service] [ParcelSeedService.seedParcels]    First failure exception - Request: {}", parcelRequest, e);
                    }
                }
            }

            log.debug("[user-service] [ParcelSeedService.seedParcels] Parcel seeding completed: {} successful, {} failed", successCount, failCount);
            return new SeedParcelsResult(successCount, failCount, count);

        } catch (Exception e) {
            log.error("[user-service] [ParcelSeedService.seedParcels] Error during parcel seeding", e);
            return new SeedParcelsResult(0, count, count);
        }
    }

    /**
     * Result holder for seed operation
     */
    public static class SeedParcelsResult {
        public final int successCount;
        public final int failCount;
        public final int total;

        public SeedParcelsResult(int successCount, int failCount, int total) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.total = total;
        }
    }

    /**
     * Get address information from zone-service directly
     */
    private AddressInfo getAddressInfo(String destinationId) {
        try {
            String responseBody = zoneServiceWebClient.get()
                    .uri("/api/v1/addresses/{id}", destinationId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                return null;
            }

            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode resultNode = responseJson.get("result");
            if (resultNode == null) {
                return null;
            }

            String name = resultNode.has("name") ? resultNode.get("name").asText() : "Unknown Address";
            String addressText = resultNode.has("addressText") ? resultNode.get("addressText").asText() : null;

            return new AddressInfo(name, addressText);
        } catch (Exception e) {
            log.warn("Failed to get address info for {}: {}", destinationId, e.getMessage());
            return null;
        }
    }

    /**
     * Address information holder
     */
    private static class AddressInfo {
        String name;

        AddressInfo(String name, String addressText) {
            this.name = name;
            // addressText is available but not currently used in parcel creation
        }
    }
}
