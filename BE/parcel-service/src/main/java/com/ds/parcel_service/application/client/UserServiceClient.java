package com.ds.parcel_service.application.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ds.parcel_service.common.entities.dto.common.BaseResponse;
import com.ds.parcel_service.common.entities.dto.common.PagedData;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client to call User Service REST API
 * Replaces UserSnapshot with direct service calls
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url:${user.service.url:http://user-service:21501}}")
    private String userServiceBaseUrl;

    /**
     * Get user information by user ID from User Service
     * @param userId The user ID (Keycloak ID)
     * @return UserInfo or null if not found
     */
    public UserInfo getUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .path("/api/v1/users/{id}")
                    .buildAndExpand(userId)
                    .toUriString();

            log.debug("[parcel-service] [UserServiceClient.getUserById] Calling User Service for user {}", userId);
            ParameterizedTypeReference<BaseResponse<UserInfo>> type = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<BaseResponse<UserInfo>> response = restTemplate.exchange(uri, HttpMethod.GET, null, type);
            BaseResponse<UserInfo> body = response.getBody();
            if (body != null && body.getResult() != null) {
                return body.getResult();
            }
            log.debug("[parcel-service] [UserServiceClient.getUserById] User {} not found in User Service", userId);
            return null;
        } catch (RestClientException e) {
            log.debug("[parcel-service] [UserServiceClient.getUserById] Failed to get user {} from User Service: {}", userId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[parcel-service] [UserServiceClient.getUserById] Unexpected error getting user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Bulk get users by list of user IDs using V2 API
     * Uses POST /api/v2/users with filter userId IN (userIds)
     * @param userIds List of user IDs
     * @return Map of userId to UserInfo
     */
    public Map<String, UserInfo> getUsersByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // Build V2 filter: userId IN (userIds)
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "condition");  // Must be lowercase per FilterItemV2 JsonSubTypes
            filter.put("field", "id");
            filter.put("operator", "IN");
            filter.put("value", userIds);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");  // Must be lowercase per FilterItemV2 JsonSubTypes
            filters.put("operator", "AND");
            filters.put("items", List.of(filter));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filters", filters);
            requestBody.put("page", 0);
            requestBody.put("size", userIds.size()); // Request all at once
            
            String uri = userServiceBaseUrl + "/api/v2/users";
            
            log.debug("[parcel-service] [UserServiceClient.getUsersByIds] Calling User Service V2 bulk API for {} users", userIds.size());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // User-service returns BaseResponse<PagedData<UserDto>>, deserialize as Map and map to UserInfo
            ParameterizedTypeReference<BaseResponse<PagedData<Map<String, Object>>>> type = new ParameterizedTypeReference<>() {};
            ResponseEntity<BaseResponse<PagedData<Map<String, Object>>>> response = restTemplate.exchange(
                uri, HttpMethod.POST, request, type);
            
            BaseResponse<PagedData<Map<String, Object>>> body = response.getBody();
            if (body != null && body.getResult() != null && body.getResult().getData() != null) {
                Map<String, UserInfo> result = new HashMap<>();
                for (Map<String, Object> userMap : body.getResult().getData()) {
                    UserInfo userInfo = new UserInfo();
                    userInfo.setId((String) userMap.get("id"));
                    userInfo.setFirstName((String) userMap.get("firstName"));
                    userInfo.setLastName((String) userMap.get("lastName"));
                    userInfo.setUsername((String) userMap.get("username"));
                    userInfo.setEmail((String) userMap.get("email"));
                    userInfo.setPhone((String) userMap.get("phone"));
                    userInfo.setAddress((String) userMap.get("address"));
                    if (userInfo.getId() != null) {
                        result.put(userInfo.getId(), userInfo);
                    }
                }
                log.debug("[parcel-service] [UserServiceClient.getUsersByIds] Retrieved {} users from User Service", result.size());
                return result;
            }
            
            log.debug("[parcel-service] [UserServiceClient.getUsersByIds] No users found in User Service");
            return new HashMap<>();
        } catch (RestClientException e) {
            log.warn("[parcel-service] [UserServiceClient.getUsersByIds] Failed to get users from User Service: {}", e.getMessage());
            return new HashMap<>();
        } catch (Exception e) {
            log.error("[parcel-service] [UserServiceClient.getUsersByIds] Unexpected error getting users: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Get UserAddress information by address ID from User Service
     * @param addressId The UserAddress ID
     * @return UserAddressInfo or null if not found
     */
    public UserAddressInfo getUserAddressById(String addressId) {
        if (addressId == null || addressId.isBlank()) {
            return null;
        }
        
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .path("/api/v1/users/addresses/{id}")
                    .buildAndExpand(addressId)
                    .toUriString();

            log.debug("[parcel-service] [UserServiceClient.getUserAddressById] Calling User Service for address {}", addressId);
            ParameterizedTypeReference<BaseResponse<UserAddressInfo>> type = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<BaseResponse<UserAddressInfo>> response = restTemplate.exchange(uri, HttpMethod.GET, null, type);
            BaseResponse<UserAddressInfo> body = response.getBody();
            if (body != null && body.getResult() != null) {
                return body.getResult();
            }
            log.debug("[parcel-service] [UserServiceClient.getUserAddressById] Address {} not found in User Service", addressId);
            return null;
        } catch (RestClientException e) {
            log.debug("[parcel-service] [UserServiceClient.getUserAddressById] Failed to get address {} from User Service: {}", addressId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[parcel-service] [UserServiceClient.getUserAddressById] Unexpected error getting address {}: {}", addressId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get all addresses for a user from User Service
     * @param userId The user ID
     * @return List of UserAddressInfo or empty list if not found
     */
    public List<UserAddressInfo> getUserAddressesByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return List.of();
        }
        
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .path("/api/v1/users/{userId}/addresses")
                    .buildAndExpand(userId)
                    .toUriString();

            log.debug("[parcel-service] [UserServiceClient.getUserAddressesByUserId] Calling User Service for addresses of user {}", userId);
            ParameterizedTypeReference<BaseResponse<List<UserAddressInfo>>> type = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<BaseResponse<List<UserAddressInfo>>> response = restTemplate.exchange(uri, HttpMethod.GET, null, type);
            BaseResponse<List<UserAddressInfo>> body = response.getBody();
            if (body != null && body.getResult() != null) {
                return body.getResult();
            }
            log.debug("[parcel-service] [UserServiceClient.getUserAddressesByUserId] No addresses found for user {} in User Service", userId);
            return List.of();
        } catch (RestClientException e) {
            log.debug("[parcel-service] [UserServiceClient.getUserAddressesByUserId] Failed to get addresses for user {} from User Service: {}", userId, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("[parcel-service] [UserServiceClient.getUserAddressesByUserId] Unexpected error getting addresses for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Get users by filter using V2 API
     * @param usernamePrefix Filter users by username prefix (e.g., "client", "shop")
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of UserInfo matching the filter
     */
    public List<UserInfo> getUsersByUsernamePrefix(String usernamePrefix, int page, int size) {
        if (usernamePrefix == null || usernamePrefix.isBlank()) {
            return List.of();
        }
        
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "condition");
            filter.put("field", "username");
            filter.put("operator", "STARTS_WITH");
            filter.put("value", usernamePrefix);
            filter.put("caseSensitive", false);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");
            filters.put("operator", "AND");
            filters.put("items", List.of(filter));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filters", filters);
            requestBody.put("page", page);
            requestBody.put("size", size);
            
            String uri = userServiceBaseUrl + "/api/v2/users";
            
            log.debug("[parcel-service] [UserServiceClient.getUsersByUsernamePrefix] Calling User Service V2 API for users with username prefix '{}'", usernamePrefix);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ParameterizedTypeReference<BaseResponse<PagedData<Map<String, Object>>>> type = 
                new ParameterizedTypeReference<>() {};
            ResponseEntity<BaseResponse<PagedData<Map<String, Object>>>> response = 
                restTemplate.exchange(uri, HttpMethod.POST, request, type);
            
            BaseResponse<PagedData<Map<String, Object>>> body = response.getBody();
            if (body != null && body.getResult() != null && body.getResult().getData() != null) {
                List<UserInfo> result = new ArrayList<>();
                for (Map<String, Object> userMap : body.getResult().getData()) {
                    UserInfo userInfo = new UserInfo();
                    userInfo.setId((String) userMap.get("id"));
                    userInfo.setFirstName((String) userMap.get("firstName"));
                    userInfo.setLastName((String) userMap.get("lastName"));
                    userInfo.setUsername((String) userMap.get("username"));
                    userInfo.setEmail((String) userMap.get("email"));
                    userInfo.setPhone((String) userMap.get("phone"));
                    userInfo.setAddress((String) userMap.get("address"));
                    result.add(userInfo);
                }
                log.debug("[parcel-service] [UserServiceClient.getUsersByUsernamePrefix] Retrieved {} users with username prefix '{}'", result.size(), usernamePrefix);
                return result;
            }
            
            log.debug("[parcel-service] [UserServiceClient.getUsersByUsernamePrefix] No users found with username prefix '{}'", usernamePrefix);
            return List.of();
        } catch (RestClientException e) {
            log.warn("[parcel-service] [UserServiceClient.getUsersByUsernamePrefix] Failed to get users with username prefix '{}' from User Service: {}", usernamePrefix, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("[parcel-service] [UserServiceClient.getUsersByUsernamePrefix] Unexpected error getting users with username prefix '{}': {}", usernamePrefix, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * DTO for user information from User Service
     */
    @Data
    public static class UserInfo {
        private String id;
        private String firstName;
        private String lastName;
        private String username;
        private String email;
        private String phone;
        private String address;
        
        /**
         * Get full name (firstName + lastName)
         */
        public String getFullName() {
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            } else if (firstName != null) {
                return firstName;
            } else if (lastName != null) {
                return lastName;
            } else if (username != null) {
                return username;
            }
            return "User " + (id != null ? id.substring(0, Math.min(4, id.length())) : "Unknown");
        }
    }

    /**
     * DTO for UserAddress information from User Service
     */
    @Data
    public static class UserAddressInfo {
        private String id;
        private String userId;
        private String destinationId; // Reference to zone_service addresses table
        private String note;
        private String tag;
        private Boolean isPrimary;
    }
}
