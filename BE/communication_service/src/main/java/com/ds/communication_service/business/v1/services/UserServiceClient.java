package com.ds.communication_service.business.v1.services;

import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.UserSnapshot;
import com.ds.communication_service.app_context.repositories.UserSnapshotRepository;
import com.ds.communication_service.common.dto.UserInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to get user information from snapshot table
 * Uses local snapshot instead of calling User Service API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final UserSnapshotRepository userSnapshotRepository;

    /**
     * Get user information by user ID from snapshot table
     * @param userId The user ID
     * @return UserInfoDto or null if not found
     */
    public UserInfoDto getUserById(String userId) {
        try {
            return userSnapshotRepository.findByUserId(userId)
                .map(this::mapToUserInfoDto)
                .orElse(null);
        } catch (Exception e) {
            log.error("Error fetching user info from snapshot for userId: {}. Error: {}", userId, e.getMessage());
            return null;
        }
    }

    private UserInfoDto mapToUserInfoDto(UserSnapshot snapshot) {
        return UserInfoDto.builder()
            .id(snapshot.getUserId())
            .username(snapshot.getUsername())
            .firstName(snapshot.getFirstName())
            .lastName(snapshot.getLastName())
            .email(snapshot.getEmail())
            .build();
    }
}
