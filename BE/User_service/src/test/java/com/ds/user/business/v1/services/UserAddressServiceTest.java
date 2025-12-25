package com.ds.user.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ds.user.app_context.repositories.UserAddressRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.application.client.ZoneClient;
import com.ds.user.common.entities.base.UserAddress;
import com.ds.user.common.entities.dto.UserAddressDto;
import com.ds.user.common.entities.dto.request.CreateUserAddressRequest;
import com.ds.user.common.exceptions.ResourceNotFoundException;

/**
 * Unit tests for UserAddressService
 * Tests CRUD operations and business logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAddressService Tests")
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ZoneClient zoneClient;

    @InjectMocks
    private UserAddressService userAddressService;

    private String userId;
    private String addressId;
    private String destinationId;
    private UserAddress userAddress;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        addressId = "address-123";
        destinationId = "dest-123";

        userAddress = UserAddress.builder()
                .id(addressId)
                .userId(userId)
                .destinationId(destinationId)
                .note("Home address")
                .tag("HOME")
                .isPrimary(true)
                .build();
    }

    @Nested
    @DisplayName("getUserAddressById Tests")
    class GetUserAddressByIdTests {

        @Test
        @DisplayName("Should get user address by ID successfully")
        void shouldGetUserAddressById() {
            // Arrange
            when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(userAddress));
            when(zoneClient.getAddress(destinationId)).thenReturn(null); // Mock zone client response

            // Act
            UserAddressDto result = userAddressService.getUserAddressById(addressId);

            // Assert
            assertNotNull(result);
            assertEquals(addressId, result.getId());
            assertEquals(userId, result.getUserId());
            assertEquals(destinationId, result.getDestinationId());
            assertEquals("Home address", result.getNote());
            assertTrue(result.getIsPrimary());

            verify(userAddressRepository, times(1)).findById(addressId);
        }

        @Test
        @DisplayName("Should throw exception when address not found")
        void shouldThrowExceptionWhenAddressNotFound() {
            // Arrange
            when(userAddressRepository.findById(addressId)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                userAddressService.getUserAddressById(addressId);
            });

            assertEquals("User address not found: " + addressId, exception.getMessage());
            verify(userAddressRepository, times(1)).findById(addressId);
        }
    }

    @Nested
    @DisplayName("createUserAddress Tests")
    class CreateUserAddressTests {

        @Test
        @DisplayName("Should create user address successfully")
        void shouldCreateUserAddress() {
            // Arrange
            CreateUserAddressRequest request = CreateUserAddressRequest.builder()
                    .destinationId(destinationId)
                    .note("Work address")
                    .tag("WORK")
                    .isPrimary(true)
                    .build();

            when(userRepository.existsById(userId)).thenReturn(true);
            when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
                UserAddress addr = invocation.getArgument(0);
                addr.setId(addressId);
                return addr;
            });
            // Mock setAllNonPrimaryByUserId (called when isPrimary is true)
            doNothing().when(userAddressRepository).setAllNonPrimaryByUserId(userId);
            lenient().when(zoneClient.getAddress(destinationId)).thenReturn(null);

            // Act
            UserAddressDto result = userAddressService.createUserAddress(userId, request);

            // Assert
            assertNotNull(result);
            assertEquals(addressId, result.getId());
            assertEquals(userId, result.getUserId());
            assertEquals(destinationId, result.getDestinationId());
            assertEquals("Work address", result.getNote());
            assertTrue(result.getIsPrimary());

            verify(userRepository, times(1)).existsById(userId);
            verify(userAddressRepository, times(1)).save(any(UserAddress.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            CreateUserAddressRequest request = CreateUserAddressRequest.builder()
                    .destinationId(destinationId)
                    .build();

            when(userRepository.existsById(userId)).thenReturn(false);

            // Act & Assert
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
                userAddressService.createUserAddress(userId, request);
            });

            assertEquals("User not found: " + userId, exception.getMessage());
            verify(userAddressRepository, never()).save(any(UserAddress.class));
        }
    }
}
