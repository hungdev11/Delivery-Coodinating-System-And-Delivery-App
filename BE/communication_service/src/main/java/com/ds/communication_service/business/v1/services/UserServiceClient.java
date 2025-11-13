package com.ds.communication_service.business.v1.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.UserInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to interact with User Service
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://api-gateway:21500}")
    private String userServiceUrl;

    /**
     * Get user information by user ID
     * @param userId The user ID
     * @return UserInfoDto or null if not found
     */
    public UserInfoDto getUserById(String userId) {
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId;
            log.debug("Fetching user info from: {}", url);
            
            ResponseEntity<BaseResponse<UserInfoDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<BaseResponse<UserInfoDto>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                BaseResponse<UserInfoDto> body = response.getBody();
                if (body.isSuccess() && body.getData() != null) {
                    log.debug("Successfully fetched user info for userId: {}", userId);
                    return body.getData();
                }
            }
            
            log.warn("User not found or unsuccessful response for userId: {}", userId);
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching user info for userId: {}. Error: {}", userId, e.getMessage());
            return null;
        }
    }
}
