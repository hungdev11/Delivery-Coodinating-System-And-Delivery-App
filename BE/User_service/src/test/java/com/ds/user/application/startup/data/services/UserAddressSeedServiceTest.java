package com.ds.user.application.startup.data.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.ds.user.app_context.repositories.UserAddressRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.UserAddress;

import reactor.core.publisher.Mono;

/**
 * Unit tests for UserAddressSeedService
 * Tests address seeding logic for shops and clients
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAddressSeedService Tests")
class UserAddressSeedServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private WebClient zoneServiceWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserAddressSeedService userAddressSeedService;

    private String userId;
    private String username;
    private User user;
    private KeycloakInitConfig.AddressConfig addressConfig;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        username = "testuser";
        user = User.builder()
                .id(userId)
                .username(username)
                .build();

        addressConfig = new KeycloakInitConfig.AddressConfig();
        addressConfig.setAddressText("123 Test Street, District 1, HCMC");
        addressConfig.setLat(10.84587);
        addressConfig.setLon(106.79548);
        addressConfig.setTag("Home");
        addressConfig.setIsPrimary(true);
    }

    @Nested
    @DisplayName("seedAddress Tests")
    class SeedAddressTests {

        @Test
        @DisplayName("Should seed address successfully with addressText as name")
        void shouldSeedAddressSuccessfully() {
            // Arrange
            String destinationId = "dest-123";
            String responseBody = String.format("{\"id\":\"%s\"}", destinationId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userAddressRepository.findByUserIdAndIsPrimaryTrue(userId)).thenReturn(Optional.empty());
            when(zoneServiceWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri("/api/v1/addresses/get-or-create")).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
            when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            userAddressSeedService.seedAddress(userId, addressConfig, true);

            // Assert
            ArgumentCaptor<Map<String, Object>> requestCaptor = ArgumentCaptor.forClass(Map.class);
            verify(requestBodySpec).bodyValue(requestCaptor.capture());
            
            Map<String, Object> request = requestCaptor.getValue();
            assertEquals(addressConfig.getAddressText(), request.get("name")); // addressText used as name
            assertEquals(addressConfig.getAddressText(), request.get("addressText"));
            assertEquals(addressConfig.getLat(), request.get("lat"));
            assertEquals(addressConfig.getLon(), request.get("lon"));

            ArgumentCaptor<UserAddress> addressCaptor = ArgumentCaptor.forClass(UserAddress.class);
            verify(userAddressRepository).save(addressCaptor.capture());
            
            UserAddress savedAddress = addressCaptor.getValue();
            assertEquals(userId, savedAddress.getUserId());
            assertEquals(destinationId, savedAddress.getDestinationId());
            assertEquals("Home", savedAddress.getTag());
            assertTrue(savedAddress.getIsPrimary());
        }

        @Test
        @DisplayName("Should skip if addressText is missing")
        void shouldSkipIfAddressTextMissing() {
            // Arrange
            addressConfig.setAddressText(null);

            // Act
            userAddressSeedService.seedAddress(userId, addressConfig, true);

            // Assert
            verify(zoneServiceWebClient, never()).post();
            verify(userAddressRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip if user not found")
        void shouldSkipIfUserNotFound() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act
            userAddressSeedService.seedAddress(userId, addressConfig, true);

            // Assert
            verify(zoneServiceWebClient, never()).post();
            verify(userAddressRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip if user already has primary address")
        void shouldSkipIfUserAlreadyHasPrimaryAddress() {
            // Arrange
            UserAddress existingPrimary = UserAddress.builder()
                    .userId(userId)
                    .isPrimary(true)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userAddressRepository.findByUserIdAndIsPrimaryTrue(userId))
                    .thenReturn(Optional.of(existingPrimary));

            // Act
            userAddressSeedService.seedAddress(userId, addressConfig, true);

            // Assert
            verify(zoneServiceWebClient, never()).post();
            verify(userAddressRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should unset other primary addresses when setting new primary")
        void shouldUnsetOtherPrimaryAddresses() {
            // Arrange
            String destinationId = "dest-123";
            String responseBody = String.format("{\"result\":{\"id\":\"%s\"}}", destinationId);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userAddressRepository.findByUserIdAndIsPrimaryTrue(userId)).thenReturn(Optional.empty());
            when(zoneServiceWebClient.post()).thenReturn(requestBodyUriSpec);
            lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
            when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(userAddressRepository).setAllNonPrimaryByUserId(userId);

            // Act
            userAddressSeedService.seedAddress(userId, addressConfig, true);

            // Assert
            verify(userAddressRepository).setAllNonPrimaryByUserId(userId);
        }
    }

    @Nested
    @DisplayName("seedPrimaryAddressesForUsers Tests")
    class SeedPrimaryAddressesForUsersTests {

        @Test
        @DisplayName("Should seed multiple addresses for client user")
        void shouldSeedMultipleAddressesForClient() {
            // Arrange
            KeycloakInitConfig.RealmConfig realmConfig = new KeycloakInitConfig.RealmConfig();
            KeycloakInitConfig.UserConfig userConfig = new KeycloakInitConfig.UserConfig();
            userConfig.setUsername(username);
            userConfig.setRealmRoles(List.of("CLIENT"));

            List<KeycloakInitConfig.AddressConfig> addresses = new ArrayList<>();
            
            KeycloakInitConfig.AddressConfig addr1 = new KeycloakInitConfig.AddressConfig();
            addr1.setAddressText("Address 1");
            addr1.setLat(10.0);
            addr1.setLon(106.0);
            addr1.setTag("Home");
            addr1.setIsPrimary(true);
            addresses.add(addr1);

            KeycloakInitConfig.AddressConfig addr2 = new KeycloakInitConfig.AddressConfig();
            addr2.setAddressText("Address 2");
            addr2.setLat(11.0);
            addr2.setLon(107.0);
            addr2.setTag("Company");
            addr2.setIsPrimary(false);
            addresses.add(addr2);

            userConfig.setAddresses(addresses);
            realmConfig.setUsers(List.of(userConfig));

            String destinationId1 = "dest-1";
            String destinationId2 = "dest-2";
            String responseBody1 = String.format("{\"result\":{\"id\":\"%s\"}}", destinationId1);
            String responseBody2 = String.format("{\"result\":{\"id\":\"%s\"}}", destinationId2);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userAddressRepository.findByUserId(userId)).thenReturn(List.of());
            when(userAddressRepository.findByUserIdAndIsPrimaryTrue(userId)).thenReturn(Optional.empty());
            when(zoneServiceWebClient.post()).thenReturn(requestBodyUriSpec);
            lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            lenient().when(responseSpec.bodyToMono(String.class))
                    .thenReturn(Mono.just(responseBody1))
                    .thenReturn(Mono.just(responseBody2));
            when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(userAddressRepository).setAllNonPrimaryByUserId(userId);

            // Act
            userAddressSeedService.seedPrimaryAddressesForUsers(realmConfig);

            // Assert
            verify(userAddressRepository, times(2)).save(any(UserAddress.class));
        }

        @Test
        @DisplayName("Should seed single address for shop user (backward compatibility)")
        void shouldSeedSingleAddressForShop() {
            // Arrange
            KeycloakInitConfig.RealmConfig realmConfig = new KeycloakInitConfig.RealmConfig();
            KeycloakInitConfig.UserConfig userConfig = new KeycloakInitConfig.UserConfig();
            userConfig.setUsername(username);
            userConfig.setRealmRoles(List.of("SHOP"));
            userConfig.setAddress(addressConfig);
            realmConfig.setUsers(List.of(userConfig));

            String destinationId = "dest-123";
            String responseBody = String.format("{\"result\":{\"id\":\"%s\"}}", destinationId);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userAddressRepository.findByUserId(userId)).thenReturn(List.of());
            when(userAddressRepository.findByUserIdAndIsPrimaryTrue(userId)).thenReturn(Optional.empty());
            when(zoneServiceWebClient.post()).thenReturn(requestBodyUriSpec);
            lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
            lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            lenient().when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseBody));
            when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(userAddressRepository).setAllNonPrimaryByUserId(userId);

            // Act
            userAddressSeedService.seedPrimaryAddressesForUsers(realmConfig);

            // Assert
            verify(userAddressRepository, times(1)).save(any(UserAddress.class));
        }

        @Test
        @DisplayName("Should skip if user already has addresses")
        void shouldSkipIfUserAlreadyHasAddresses() {
            // Arrange
            KeycloakInitConfig.RealmConfig realmConfig = new KeycloakInitConfig.RealmConfig();
            KeycloakInitConfig.UserConfig userConfig = new KeycloakInitConfig.UserConfig();
            userConfig.setUsername(username);
            userConfig.setRealmRoles(List.of("CLIENT"));
            userConfig.setAddresses(List.of(addressConfig));
            realmConfig.setUsers(List.of(userConfig));

            UserAddress existingAddress = UserAddress.builder()
                    .userId(userId)
                    .build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userAddressRepository.findByUserId(userId)).thenReturn(List.of(existingAddress));

            // Act
            userAddressSeedService.seedPrimaryAddressesForUsers(realmConfig);

            // Assert
            verify(zoneServiceWebClient, never()).post();
            verify(userAddressRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip non-shop and non-client users")
        void shouldSkipNonShopAndNonClientUsers() {
            // Arrange
            KeycloakInitConfig.RealmConfig realmConfig = new KeycloakInitConfig.RealmConfig();
            KeycloakInitConfig.UserConfig userConfig = new KeycloakInitConfig.UserConfig();
            userConfig.setUsername(username);
            userConfig.setRealmRoles(List.of("ADMIN"));
            realmConfig.setUsers(List.of(userConfig));

            // Act
            userAddressSeedService.seedPrimaryAddressesForUsers(realmConfig);

            // Assert
            verify(userRepository, never()).findByUsername(anyString());
            verify(zoneServiceWebClient, never()).post();
        }
    }
}
