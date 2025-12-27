package com.ds.session.session_service.application.client.userclient;

import com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to get delivery man information from User Service API
 * Uses bulk query API instead of snapshot table
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    @Qualifier("userServiceWebClient")
    private final WebClient userServiceWebClient;

    /**
     * Get delivery man information by user ID from User Service API
     * Uses V2 query API with filter to query by userId
     * @param userId The user ID
     * @return DeliveryManResponse or null if not found
     */
    public DeliveryManResponse getDeliveryManByUserId(String userId) {
        try {
            // Use V2 query API with filter userId = userId
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "condition");  // Must be lowercase per FilterItemV2 JsonSubTypes
            filter.put("field", "userId");
            filter.put("operator", "EQUALS");
            filter.put("value", userId);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");  // Must be lowercase per FilterItemV2 JsonSubTypes
            filters.put("operator", "AND");
            filters.put("items", List.of(filter));
            
            requestBody.put("filters", filters);
            requestBody.put("page", 0);
            requestBody.put("size", 1);
            
            DeliveryManQueryResponse response = userServiceWebClient.post()
                .uri("/api/v2/users/shippers")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(DeliveryManQueryResponse.class)
                .block();

            if (response != null && response.getResult() != null 
                    && response.getResult().getData() != null 
                    && !response.getResult().getData().isEmpty()) {
                return mapToDeliveryManResponse(response.getResult().getData().get(0));
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching delivery man info from User Service for userId: {}. Error: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Bulk get delivery men information by user IDs from User Service API
     * Uses V2 query API with filter userId IN (userIds)
     * @param userIds List of user IDs
     * @return Map of userId to DeliveryManResponse
     */
    public Map<String, DeliveryManResponse> getDeliveryMenByUserIds(List<String> userIds) {
        try {
            if (userIds == null || userIds.isEmpty()) {
                return new HashMap<>();
            }
            
            // Use V2 query API with filter userId IN (userIds)
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "condition");  // Must be lowercase per FilterItemV2 JsonSubTypes
            filter.put("field", "userId");
            filter.put("operator", "IN");
            filter.put("value", userIds);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");  // Must be lowercase per FilterItemV2 JsonSubTypes
            filters.put("operator", "AND");
            filters.put("items", List.of(filter));
            
            requestBody.put("filters", filters);
            requestBody.put("page", 0);
            requestBody.put("size", userIds.size()); // Request all at once
            
            DeliveryManQueryResponse response = userServiceWebClient.post()
                .uri("/api/v2/users/shippers")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(DeliveryManQueryResponse.class)
                .block();

            Map<String, DeliveryManResponse> result = new HashMap<>();
            if (response != null && response.getResult() != null 
                    && response.getResult().getData() != null) {
                for (DeliveryManDto dm : response.getResult().getData()) {
                    result.put(dm.getUserId(), mapToDeliveryManResponse(dm));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Error bulk fetching delivery men info from User Service. Error: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Map DeliveryManDto from user-service to DeliveryManResponse
     */
    private DeliveryManResponse mapToDeliveryManResponse(DeliveryManDto dto) {
        return DeliveryManResponse.builder()
            .id(dto.getId())
            .userId(dto.getUserId())
            .username(dto.getUsername())
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .email(dto.getEmail())
            .phone(dto.getPhone())
            .vehicleType(dto.getVehicleType())
            .capacityKg(dto.getCapacityKg())
            .build();
    }

    /**
     * Query response wrapper - matches BaseResponse<PagedData<DeliveryManDto>> structure
     */
    private static class DeliveryManQueryResponse {
        private DeliveryManPagedData result;

        public DeliveryManPagedData getResult() {
            return result;
        }

        public void setResult(DeliveryManPagedData result) {
            this.result = result;
        }
    }

    /**
     * PagedData wrapper - matches PagedData<DeliveryManDto> structure
     */
    private static class DeliveryManPagedData {
        private List<DeliveryManDto> data;

        public List<DeliveryManDto> getData() {
            return data;
        }

        public void setData(List<DeliveryManDto> data) {
            this.data = data;
        }
        
        // PagedData has other fields (page, etc.), but we only need data for now
    }

    /**
     * DeliveryManDto from user-service (simplified for mapping)
     */
    private static class DeliveryManDto {
        private java.util.UUID id;
        private String userId;
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String vehicleType;
        private Double capacityKg;

        public java.util.UUID getId() { return id; }
        public void setId(java.util.UUID id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getVehicleType() { return vehicleType; }
        public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
        public Double getCapacityKg() { return capacityKg; }
        public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }
    }
}
