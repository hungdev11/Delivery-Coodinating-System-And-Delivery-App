package com.ds.user.business.v1.services;

import com.ds.user.app_context.repositories.UserAddressRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.application.client.AddressDetail;
import com.ds.user.application.client.AddressResponse;
import com.ds.user.application.client.ZoneClient;
import com.ds.user.common.entities.base.UserAddress;
import com.ds.user.common.entities.dto.UserAddressDto;
import com.ds.user.common.entities.dto.request.CreateUserAddressRequest;
import com.ds.user.common.entities.dto.request.UpdateUserAddressRequest;
import com.ds.user.common.exceptions.ResourceNotFoundException;
import com.ds.user.common.interfaces.IUserAddressService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserAddressService implements IUserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final ZoneClient zoneClient;
    private final WebClient zoneServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserAddressService(
            UserAddressRepository userAddressRepository,
            UserRepository userRepository,
            ZoneClient zoneClient,
            @Qualifier("zoneServiceWebClient") WebClient zoneServiceWebClient) {
        this.userAddressRepository = userAddressRepository;
        this.userRepository = userRepository;
        this.zoneClient = zoneClient;
        this.zoneServiceWebClient = zoneServiceWebClient;
    }

    @Override
    @Transactional
    public UserAddressDto createUserAddress(String userId, CreateUserAddressRequest request) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        // Determine destinationId: either use provided one, or create/get from zone-service
        String destinationId = request.getDestinationId();
        
        if (destinationId == null || destinationId.isBlank()) {
            // If destinationId is not provided, lat and lon must be provided
            if (request.getLat() == null || request.getLon() == null) {
                throw new IllegalArgumentException(
                    "Either destinationId must be provided, or both lat and lon must be provided");
            }
            
            // Call zone-service to get-or-create address
            destinationId = getOrCreateAddressInZoneService(
                request.getLat(), 
                request.getLon(),
                request.getName(),
                request.getAddressText());
        }

        // If setting as primary, unset other primary addresses
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            userAddressRepository.setAllNonPrimaryByUserId(userId);
        }

        UserAddress userAddress = UserAddress.builder()
                .userId(userId)
                .destinationId(destinationId)
                .note(request.getNote())
                .tag(request.getTag())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();

        UserAddress saved = userAddressRepository.save(userAddress);
        log.debug("Created user address {} for user {} with destinationId {}", saved.getId(), userId, destinationId);

        return toDto(saved);
    }

    /**
     * Call zone-service to get or create an address.
     * Returns the address ID from zone-service.
     */
    private String getOrCreateAddressInZoneService(BigDecimal lat, BigDecimal lon, String name, String addressText) {
        try {
            Map<String, Object> createAddressRequest = new HashMap<>();
            if (name != null && !name.isBlank()) {
                createAddressRequest.put("name", name);
            }
            if (addressText != null && !addressText.isBlank()) {
                createAddressRequest.put("addressText", addressText);
                // Use addressText as name if name is not provided
                if (name == null || name.isBlank()) {
                    createAddressRequest.put("name", addressText);
                }
            }
            createAddressRequest.put("lat", lat);
            createAddressRequest.put("lon", lon);

            log.debug("Calling zone-service to get-or-create address at ({}, {})", lat, lon);

            String responseBody = zoneServiceWebClient.post()
                    .uri("/api/v1/addresses/get-or-create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createAddressRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Empty response from zone-service when creating address");
            }

            // Parse response to get address ID
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode resultNode = responseJson.get("result");
            if (resultNode == null || !resultNode.has("id")) {
                log.error("Invalid response format from zone-service: {}", responseBody);
                throw new RuntimeException("Invalid response format from zone-service when creating address");
            }

            String addressId = resultNode.get("id").asText();
            log.debug("Address created/retrieved from zone-service: {}", addressId);
            return addressId;

        } catch (WebClientResponseException e) {
            log.error("Failed to create address in zone-service: HTTP {} - {}", 
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to create address in zone-service: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error calling zone-service to create address: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling zone-service to create address: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public UserAddressDto updateUserAddress(String userId, String addressId, UpdateUserAddressRequest request) {
        UserAddress userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User address not found: " + addressId));

        // Update fields if provided
        if (request.getDestinationId() != null) {
            userAddress.setDestinationId(request.getDestinationId());
        }
        if (request.getNote() != null) {
            userAddress.setNote(request.getNote());
        }
        if (request.getTag() != null) {
            userAddress.setTag(request.getTag());
        }
        if (request.getIsPrimary() != null) {
            if (request.getIsPrimary()) {
                // Set all other addresses as non-primary
                userAddressRepository.setAllNonPrimaryByUserId(userId);
            }
            userAddress.setIsPrimary(request.getIsPrimary());
        }

        UserAddress updated = userAddressRepository.save(userAddress);
        log.debug("Updated user address {} for user {}", updated.getId(), userId);

        return toDto(updated);
    }

    @Override
    @Transactional
    public void deleteUserAddress(String userId, String addressId) {
        UserAddress userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User address not found: " + addressId));

        userAddressRepository.delete(userAddress);
        log.debug("Deleted user address {} for user {}", addressId, userId);
    }

    @Override
    public UserAddressDto getUserAddress(String userId, String addressId) {
        UserAddress userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User address not found: " + addressId));

        return toDto(userAddress);
    }

    @Override
    public List<UserAddressDto> getUserAddresses(String userId) {
        List<UserAddress> addresses = userAddressRepository.findByUserId(userId);
        return addresses.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserAddressDto getPrimaryUserAddress(String userId) {
        UserAddress primaryAddress = userAddressRepository.findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No primary address found for user: " + userId));

        return toDto(primaryAddress);
    }

    @Override
    @Transactional
    public UserAddressDto setPrimaryAddress(String userId, String addressId) {
        UserAddress userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("User address not found: " + addressId));

        // Set all other addresses as non-primary
        userAddressRepository.setAllNonPrimaryByUserId(userId);

        // Set this address as primary
        userAddress.setIsPrimary(true);
        UserAddress updated = userAddressRepository.save(userAddress);
        log.debug("Set user address {} as primary for user {}", addressId, userId);

        return toDto(updated);
    }

    // Admin methods
    @Override
    @Transactional
    public UserAddressDto createUserAddressForUser(String targetUserId, CreateUserAddressRequest request) {
        return createUserAddress(targetUserId, request);
    }

    @Override
    @Transactional
    public UserAddressDto updateUserAddressForUser(String targetUserId, String addressId,
            UpdateUserAddressRequest request) {
        return updateUserAddress(targetUserId, addressId, request);
    }

    @Override
    @Transactional
    public void deleteUserAddressForUser(String targetUserId, String addressId) {
        deleteUserAddress(targetUserId, addressId);
    }

    @Override
    public UserAddressDto getUserAddressForUser(String targetUserId, String addressId) {
        return getUserAddress(targetUserId, addressId);
    }

    @Override
    public List<UserAddressDto> getUserAddressesForUser(String targetUserId) {
        return getUserAddresses(targetUserId);
    }

    @Override
    public UserAddressDto getUserAddressById(String addressId) {
        UserAddress userAddress = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("User address not found: " + addressId));
        return toDto(userAddress);
    }

    private UserAddressDto toDto(UserAddress userAddress) {
        UserAddressDto.UserAddressDtoBuilder builder = UserAddressDto.builder()
                .id(userAddress.getId())
                .userId(userAddress.getUserId())
                .destinationId(userAddress.getDestinationId())
                .note(userAddress.getNote())
                .tag(userAddress.getTag())
                .isPrimary(userAddress.getIsPrimary())
                .createdAt(userAddress.getCreatedAt())
                .updatedAt(userAddress.getUpdatedAt());

        // Fetch destination details from zone-service
        try {
            AddressResponse<AddressDetail> addressResponse = zoneClient.getAddress(userAddress.getDestinationId());
            if (addressResponse != null && addressResponse.getResult() != null) {
                AddressDetail addressDetail = addressResponse.getResult();
                UserAddressDto.DestinationDetails destinationDetails = UserAddressDto.DestinationDetails.builder()
                        .id(addressDetail.getId())
                        .name(addressDetail.getName())
                        .addressText(addressDetail.getAddressText())
                        .lat(addressDetail.getLat())
                        .lon(addressDetail.getLon())
                        .build();
                builder.destinationDetails(destinationDetails);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch destination details for destinationId {}: {}", 
                    userAddress.getDestinationId(), e.getMessage());
            // Continue without destination details
        }

        return builder.build();
    }
}
