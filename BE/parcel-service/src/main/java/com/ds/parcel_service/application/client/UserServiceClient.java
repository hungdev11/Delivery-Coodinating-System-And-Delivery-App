package com.ds.parcel_service.application.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ds.parcel_service.common.entities.dto.common.BaseResponse;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
}
