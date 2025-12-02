package com.ds.communication_service.business.v1.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ds.communication_service.common.dto.UserInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to get user information from User Service REST API
 * Now calls external service directly instead of using local snapshot
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url:${user.service.url:http://user-service:21501}}")
    private String userServiceBaseUrl;

    /**
     * Get user information by user ID from User Service
     * @param userId The user ID (Keycloak ID)
     * @return UserInfoDto or null if not found
     */
    public UserInfoDto getUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        
        try {
            String uri = UriComponentsBuilder.fromHttpUrl(userServiceBaseUrl)
                    .path("/api/v1/users/{id}")
                    .buildAndExpand(userId)
                    .toUriString();

            log.debug("[communication-service] [UserServiceClient.getUserById] Calling User Service for user {}", userId);
            // User service returns BaseResponse<UserDto>, use Map to deserialize
            ParameterizedTypeReference<Map<String, Object>> type = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null, type);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) body.get("result");
                if (userData != null) {
                    return mapToUserInfoDto(userData);
                }
            }
            log.debug("[communication-service] [UserServiceClient.getUserById] User {} not found in User Service", userId);
            return null;
        } catch (RestClientException e) {
            log.debug("[communication-service] [UserServiceClient.getUserById] Failed to get user {} from User Service: {}", userId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[communication-service] [UserServiceClient.getUserById] Unexpected error getting user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Map UserDto from User service (as Map) to UserInfoDto
     */
    @SuppressWarnings("unchecked")
    private UserInfoDto mapToUserInfoDto(Map<String, Object> userData) {
        if (userData == null) {
            return null;
        }
        return UserInfoDto.builder()
            .id(getString(userData, "id"))
            .username(getString(userData, "username"))
            .firstName(getString(userData, "firstName"))
            .lastName(getString(userData, "lastName"))
            .email(getString(userData, "email"))
            .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
