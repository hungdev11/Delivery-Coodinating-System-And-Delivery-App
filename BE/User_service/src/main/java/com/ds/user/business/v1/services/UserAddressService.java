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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAddressService implements IUserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final ZoneClient zoneClient;

    @Override
    @Transactional
    public UserAddressDto createUserAddress(String userId, CreateUserAddressRequest request) {
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        // If setting as primary, unset other primary addresses
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            userAddressRepository.setAllNonPrimaryByUserId(userId);
        }

        UserAddress userAddress = UserAddress.builder()
                .userId(userId)
                .destinationId(request.getDestinationId())
                .note(request.getNote())
                .tag(request.getTag())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .build();

        UserAddress saved = userAddressRepository.save(userAddress);
        log.debug("Created user address {} for user {}", saved.getId(), userId);

        return toDto(saved);
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
