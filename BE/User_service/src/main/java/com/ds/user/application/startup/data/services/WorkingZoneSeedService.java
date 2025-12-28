package com.ds.user.application.startup.data.services;

import com.ds.user.app_context.repositories.DeliveryManRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.app_context.repositories.WorkingZoneRepository;
import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.entities.base.DeliveryMan;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.WorkingZone;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.MediaType;

import java.util.*;

/**
 * Service to seed working zones for delivery men
 * Zones are distributed across shippers (1-3 zones per shipper)
 */
@Slf4j
@Service
public class WorkingZoneSeedService {

    @Autowired
    private WorkingZoneRepository workingZoneRepository;

    @Autowired
    private DeliveryManRepository deliveryManRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    @Qualifier("zoneServiceWebClient")
    private WebClient zoneServiceWebClient;
    
    @Value("${services.zone.base-url:http://zone-service:21503}")
    private String zoneServiceBaseUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Zone codes mapping from zone names
     * Based on the zones provided by user
     */
    private static final Map<String, String> ZONE_NAME_TO_CODE = Map.of(
        "Tăng Nhơn Phú", "TNP",
        "Phước Long", "PL",
        "Thủ Đức", "TĐ",
        "Hiệp Bình", "HB",
        "Linh Xuân", "LX",
        "Tam Bình", "TB",
        "Long Bình", "LB"
    );

    /**
     * Seed working zones for all delivery men
     * Only seeds if database is empty (no working zones exist)
     * Each shipper gets 1-3 zones distributed evenly
     */
    @Transactional
    public void seedWorkingZonesForAllDeliveryMen(KeycloakInitConfig.RealmConfig realmConfig) {
        // Check if database already has working zones - if yes, skip seeding
        long existingWorkingZoneCount = workingZoneRepository.count();
        if (existingWorkingZoneCount > 0) {
            log.info("✓ Working zone seeding skipped: Database already has {} working zone(s). Skipping seed.", existingWorkingZoneCount);
            return;
        }

        log.info("🌱 Starting working zone seeding for delivery men...");

        // Get all delivery men with SHIPPER role
        List<DeliveryMan> allDeliveryMen = getAllShipperDeliveryMen(realmConfig);
        
        if (allDeliveryMen.isEmpty()) {
            log.warn("⚠️ No delivery men found. Skipping working zone seeding.");
            return;
        }

        // Fetch zone IDs from zone-service by code
        Map<String, String> zoneCodeToIdMap = fetchZoneIdsFromZoneService(ZONE_NAME_TO_CODE.values());
        
        if (zoneCodeToIdMap.isEmpty()) {
            log.warn("⚠️ No zones found from zone-service. Skipping working zone seeding.");
            return;
        }
        
        // Convert to list of zone IDs for distribution
        List<String> zoneIds = new ArrayList<>(zoneCodeToIdMap.values());
        
        // Distribute zones across delivery men (1-3 zones per shipper)
        // Strategy: Round-robin distribution to ensure each zone has at least some shippers
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        int zoneCount = 0;

        // Track which zones have been assigned to ensure fair distribution
        Map<String, Integer> zoneAssignmentCount = new HashMap<>();
        for (String zoneId : zoneIds) {
            zoneAssignmentCount.put(zoneId, 0);
        }

        for (DeliveryMan deliveryMan : allDeliveryMen) {
            try {
                // Check if delivery man already has working zones
                List<WorkingZone> existingZones = workingZoneRepository
                    .findByDeliveryManIdOrderByOrderAsc(deliveryMan.getId());
                
                if (!existingZones.isEmpty()) {
                    log.debug("✓ Delivery man '{}' already has {} working zone(s). Skipping.", 
                        deliveryMan.getUser().getUsername(), existingZones.size());
                    skipCount++;
                    continue;
                }

                // Track zones already assigned to this delivery man (to avoid duplicates for same shipper)
                Set<String> assignedZoneIds = new HashSet<>();
                
                // Assign 2-3 zones per delivery man (changed from 1-3 to ensure 2-3 as expected)
                int numberOfZones = new Random().nextInt(2) + 2; // 2-3 zones (always at least 2)
                int order = 1;
                
                // Create a shuffled list of available zones for this shipper
                // With many-to-many relationship, same zone can be assigned to multiple shippers
                List<String> availableZones = new ArrayList<>(zoneIds);
                Collections.shuffle(availableZones); // Randomize for fair distribution
                
                for (String zoneId : availableZones) {
                    // Stop if we've assigned enough zones for this shipper
                    if (assignedZoneIds.size() >= numberOfZones) {
                        break;
                    }
                    
                    // Skip if zone already assigned to this delivery man (avoid duplicate for same shipper)
                    if (assignedZoneIds.contains(zoneId)) {
                        continue;
                    }
                    
                    // Check if zone already exists for this delivery man in database (double check)
                    Optional<WorkingZone> existingZone = workingZoneRepository
                        .findByDeliveryManIdAndZoneId(deliveryMan.getId(), zoneId);
                    if (existingZone.isPresent()) {
                        log.debug("Zone {} already exists for delivery man '{}'. Skipping.", 
                            zoneId, deliveryMan.getUser().getUsername());
                        continue;
                    }
                    
                    // Create working zone (many-to-many: zone n - n shipper through this table)
                    // Multiple shippers can have the same zone, so no need to check if zone is already used by other shippers
                    WorkingZone workingZone = WorkingZone.builder()
                        .deliveryMan(deliveryMan)
                        .zoneId(zoneId)
                        .order(order) // Priority order (1 = highest priority)
                        .build();
                    
                    workingZoneRepository.save(workingZone);
                    assignedZoneIds.add(zoneId);
                    zoneAssignmentCount.put(zoneId, zoneAssignmentCount.get(zoneId) + 1);
                    zoneCount++;
                    order++;
                    
                    log.debug("✓ Created working zone {} (order {}) for delivery man '{}'", 
                        zoneId, order - 1, deliveryMan.getUser().getUsername());
                }
                
                if (assignedZoneIds.size() < numberOfZones) {
                    log.warn("⚠️ Only assigned {} out of {} requested zones for delivery man '{}' (not enough unique zones available in system)", 
                        assignedZoneIds.size(), numberOfZones, deliveryMan.getUser().getUsername());
                } else {
                    log.info("✓ Assigned {} zones to delivery man '{}'", assignedZoneIds.size(), deliveryMan.getUser().getUsername());
                }

                successCount++;
                log.info("✓ Seeded {} working zone(s) for delivery man '{}'", 
                    assignedZoneIds.size(), deliveryMan.getUser().getUsername());
                
            } catch (Exception e) {
                log.error("❌ Failed to seed working zones for delivery man '{}': {}", 
                    deliveryMan.getUser().getUsername(), e.getMessage(), e);
                failCount++;
            }
        }

        // Log distribution statistics
        log.info("🌱 Working zone seeding completed - Success: {}, Failed: {}, Skipped: {}, Total zones assigned: {}", 
            successCount, failCount, skipCount, zoneCount);
        log.info("📊 Zone distribution: {}", zoneAssignmentCount);
    }

    /**
     * Get all delivery men with SHIPPER role from config
     */
    private List<DeliveryMan> getAllShipperDeliveryMen(KeycloakInitConfig.RealmConfig realmConfig) {
        List<DeliveryMan> deliveryMen = new ArrayList<>();
        
        for (KeycloakInitConfig.UserConfig userConfig : realmConfig.getUsers()) {
            // Only process SHIPPER users with deliveryMan config
            if (userConfig.getDeliveryMan() == null) {
                continue;
            }

            boolean isShipper = userConfig.getRealmRoles() != null && 
                    userConfig.getRealmRoles().contains("SHIPPER");
            
            if (!isShipper) {
                continue;
            }

            try {
                // Find user by username
                Optional<User> userOpt = userRepository.findByUsername(userConfig.getUsername());
                if (userOpt.isEmpty()) {
                    log.debug("⚠️ User not found: {}. Skipping.", userConfig.getUsername());
                    continue;
                }

                User user = userOpt.get();
                
                // Find delivery man by userId
                Optional<DeliveryMan> deliveryManOpt = deliveryManRepository.findByUserId(user.getId());
                if (deliveryManOpt.isEmpty()) {
                    log.debug("⚠️ Delivery man not found for user: {}. Skipping.", userConfig.getUsername());
                    continue;
                }

                deliveryMen.add(deliveryManOpt.get());
            } catch (Exception e) {
                log.warn("⚠️ Error finding delivery man for user '{}': {}", 
                    userConfig.getUsername(), e.getMessage());
            }
        }

        return deliveryMen;
    }

    /**
     * Fetch zone IDs from zone-service by zone codes
     * @param zoneCodes List of zone codes (e.g., "TNP", "PL", etc.)
     * @return Map of zone code to zone ID
     */
    private Map<String, String> fetchZoneIdsFromZoneService(Collection<String> zoneCodes) {
        Map<String, String> zoneCodeToIdMap = new HashMap<>();
        
        if (zoneServiceWebClient == null && zoneCodes.isEmpty()) {
            log.warn("⚠️ Zone service WebClient not available. Cannot fetch zone IDs.");
            return zoneCodeToIdMap;
        }
        
        for (String zoneCode : zoneCodes) {
            try {
                String zoneId = fetchZoneIdByCode(zoneCode);
                if (zoneId != null && !zoneId.isEmpty()) {
                    zoneCodeToIdMap.put(zoneCode, zoneId);
                    log.debug("✓ Fetched zone ID {} for code {}", zoneId, zoneCode);
                } else {
                    log.warn("⚠️ Zone not found for code: {}", zoneCode);
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch zone ID for code {}: {}", zoneCode, e.getMessage());
            }
        }
        
        log.info("✓ Fetched {} zone IDs from zone-service", zoneCodeToIdMap.size());
        return zoneCodeToIdMap;
    }

    /**
     * Fetch zone ID from zone-service by zone code
     * Uses POST /api/v1/zones with filter for code
     */
    private String fetchZoneIdByCode(String zoneCode) {
        try {
            WebClient webClient = zoneServiceWebClient;
            if (webClient == null) {
                // Fallback: try to create WebClient directly
                webClient = WebClient.builder()
                    .baseUrl(zoneServiceBaseUrl)
                    .build();
            }
            
            // Build filter request for zone by code
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "condition");
            filter.put("field", "code");
            filter.put("operator", "EQUALS");
            filter.put("value", zoneCode);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");
            filters.put("operator", "AND");
            filters.put("items", List.of(filter));
            
            requestBody.put("filters", filters);
            requestBody.put("page", 0);
            requestBody.put("size", 1);
            
            String responseBody = webClient.post()
                .uri("/api/v1/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode responseJson = objectMapper.readTree(responseBody);
                JsonNode resultNode = responseJson.get("result");
                if (resultNode != null) {
                    JsonNode dataNode = resultNode.get("data");
                    if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                        JsonNode zoneNode = dataNode.get(0);
                        if (zoneNode.has("zone_id")) {
                            return zoneNode.get("zone_id").asText();
                        }
                    }
                }
                
                // Log full response for debugging if zone not found
                log.debug("Zone code {} response: {}", zoneCode, responseBody);
            }
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.warn("⚠️ Zone service returned error for code {}: {} - {}", 
                zoneCode, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch zone ID for code {}: {}", zoneCode, e.getMessage());
            log.debug("Full error for zone code {}: ", zoneCode, e);
        }
        
        return null;
    }
}
