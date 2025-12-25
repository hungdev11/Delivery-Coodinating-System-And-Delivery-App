package com.ds.parcel_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;
import com.ds.parcel_service.app_context.repositories.ParcelDestinationRepository;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.application.client.UserServiceClient;
import com.ds.parcel_service.application.client.ZoneClient;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.DestinationType;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.exceptions.ResourceNotFound;

/**
 * Error handling and edge case tests for ParcelService
 * Tests: API failures, null values, exceptions, network errors
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService Error Handling Tests")
class ParcelServiceErrorHandlingTest {

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private ParcelDestinationRepository parcelDestinationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ZoneClient zoneClient;

    @Mock
    private com.ds.parcel_service.application.services.AssignmentService assignmentService;

    @Mock
    private com.ds.parcel_service.application.client.SessionServiceClient sessionServiceClient;

    @Mock
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ParcelService parcelService;

    private UUID parcelId;
    private String senderId;
    private String receiverId;
    private String senderAddressId;
    private String receiverAddressId;
    private String senderDestinationId;
    private String receiverDestinationId;
    private ParcelCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        parcelId = UUID.randomUUID();
        senderId = UUID.randomUUID().toString();
        receiverId = UUID.randomUUID().toString();
        senderAddressId = UUID.randomUUID().toString();
        receiverAddressId = UUID.randomUUID().toString();
        senderDestinationId = UUID.randomUUID().toString();
        receiverDestinationId = UUID.randomUUID().toString();

        createRequest = ParcelCreateRequest.builder()
                .code("ERROR-TEST-001")
                .senderId(senderId)
                .receiverId(receiverId)
                .senderAddressId(senderAddressId)
                .receiverAddressId(receiverAddressId)
                .deliveryType("NORMAL")
                .weight(1.5)
                .value(new BigDecimal("100000"))
                .windowStart(LocalTime.of(8, 0))
                .windowEnd(LocalTime.of(18, 0))
                .build();
    }

    @Nested
    @DisplayName("API Failure Tests - UserServiceClient")
    class UserServiceClientFailureTests {

        @Test
        @DisplayName("Should handle RestClientException when getUserAddressById fails")
        void shouldHandleRestClientExceptionWhenGetUserAddressByIdFails() {
            // Arrange
            when(userServiceClient.getUserAddressById(senderAddressId))
                    .thenThrow(new RestClientException("Connection timeout"));

            // Act & Assert
            Exception exception = assertThrows(Exception.class, () -> {
                parcelService.createParcel(createRequest);
            });

            // Should propagate the exception (or wrap it)
            assertTrue(exception instanceof RestClientException || 
                      exception.getCause() instanceof RestClientException ||
                      exception.getMessage().contains("timeout") ||
                      exception.getMessage().contains("address"));

            verify(userServiceClient, times(1)).getUserAddressById(senderAddressId);
            verify(parcelRepository, never()).save(any(Parcel.class));
        }

        @Test
        @DisplayName("Should handle null response from getUserAddressById gracefully")
        void shouldHandleNullResponseFromGetUserAddressById() {
            // Arrange
            when(userServiceClient.getUserAddressById(senderAddressId)).thenReturn(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                parcelService.createParcel(createRequest);
            });

            assertEquals("Sender address not found: " + senderAddressId, exception.getMessage());
            verify(parcelRepository, never()).save(any(Parcel.class));
        }

        @Test
        @DisplayName("Should handle getUserById failure in toDto gracefully (should not throw)")
        void shouldHandleGetUserByIdFailureInToDto() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .id(parcelId)
                    .code("ERROR-TEST-001")
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .senderAddressId(senderAddressId)
                    .receiverAddressId(receiverAddressId)
                    .deliveryType(DeliveryType.NORMAL)
                    .status(ParcelStatus.IN_WAREHOUSE)
                    .weight(1.5)
                    .value(new BigDecimal("100000"))
                    .priority(3)
                    .build();

            ParcelDestination destination = ParcelDestination.builder()
                    .destinationId(receiverDestinationId)
                    .destinationType(DestinationType.PRIMARY)
                    .isCurrent(true)
                    .parcel(parcel)
                    .build();

            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.of(parcel));
            when(parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel))
                    .thenReturn(java.util.Optional.of(destination));

            com.ds.parcel_service.application.client.DestinationResponse<com.ds.parcel_service.application.client.DesDetail> zoneResponse =
                    new com.ds.parcel_service.application.client.DestinationResponse<>();
            com.ds.parcel_service.application.client.DesDetail detail = new com.ds.parcel_service.application.client.DesDetail();
            detail.setLat(new BigDecimal("10.8505"));
            detail.setLon(new BigDecimal("106.7718"));
            zoneResponse.setResult(detail);
            when(zoneClient.getDestination(receiverDestinationId)).thenReturn(zoneResponse);

            // Mock getUserById to throw exception (should be caught in toDto)
            lenient().when(userServiceClient.getUserById(senderId))
                    .thenThrow(new RestClientException("User service unavailable"));
            lenient().when(userServiceClient.getUserById(receiverId))
                    .thenThrow(new RestClientException("User service unavailable"));

            // Act - Should not throw exception, should return response without user names
            ParcelResponse response = parcelService.getParcelById(parcelId);

            // Assert
            assertNotNull(response);
            assertEquals(parcelId.toString(), response.getId());
            // User names should be null when API fails (graceful degradation)
            assertNull(response.getSenderName());
            assertNull(response.getReceiverName());

            verify(userServiceClient, atLeastOnce()).getUserById(anyString());
        }
    }

    @Nested
    @DisplayName("API Failure Tests - ZoneClient")
    class ZoneClientFailureTests {

        @Test
        @DisplayName("Should handle exception when getDestination fails in getParcelById")
        void shouldHandleExceptionWhenGetDestinationFails() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .id(parcelId)
                    .code("ERROR-TEST-001")
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .senderAddressId(senderAddressId)
                    .receiverAddressId(receiverAddressId)
                    .deliveryType(DeliveryType.NORMAL)
                    .status(ParcelStatus.IN_WAREHOUSE)
                    .weight(1.5)
                    .value(new BigDecimal("100000"))
                    .priority(3)
                    .build();

            ParcelDestination destination = ParcelDestination.builder()
                    .destinationId(receiverDestinationId)
                    .destinationType(DestinationType.PRIMARY)
                    .isCurrent(true)
                    .parcel(parcel)
                    .build();

            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.of(parcel));
            when(parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel))
                    .thenReturn(java.util.Optional.of(destination));
            when(zoneClient.getDestination(receiverDestinationId))
                    .thenThrow(new RestClientException("Zone service unavailable"));
            // getUserById is called in toDto, but exception happens before that
            lenient().when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act & Assert - Should throw exception or handle gracefully
            assertThrows(Exception.class, () -> {
                parcelService.getParcelById(parcelId);
            });

            verify(zoneClient, times(1)).getDestination(receiverDestinationId);
        }

        @Test
        @DisplayName("Should handle null response from getDestination")
        void shouldHandleNullResponseFromGetDestination() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .id(parcelId)
                    .code("ERROR-TEST-001")
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .senderAddressId(senderAddressId)
                    .receiverAddressId(receiverAddressId)
                    .deliveryType(DeliveryType.NORMAL)
                    .status(ParcelStatus.IN_WAREHOUSE)
                    .weight(1.5)
                    .value(new BigDecimal("100000"))
                    .priority(3)
                    .build();

            ParcelDestination destination = ParcelDestination.builder()
                    .destinationId(receiverDestinationId)
                    .destinationType(DestinationType.PRIMARY)
                    .isCurrent(true)
                    .parcel(parcel)
                    .build();

            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.of(parcel));
            when(parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel))
                    .thenReturn(java.util.Optional.of(destination));
            when(zoneClient.getDestination(receiverDestinationId)).thenReturn(null);
            when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act & Assert - Should throw NullPointerException or handle gracefully
            assertThrows(Exception.class, () -> {
                parcelService.getParcelById(parcelId);
            });

            verify(zoneClient, times(1)).getDestination(receiverDestinationId);
        }
    }

    @Nested
    @DisplayName("Null Safety Tests")
    class NullSafetyTests {

        @Test
        @DisplayName("Should handle null destination in getParcelById")
        void shouldHandleNullDestinationInGetParcelById() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .id(parcelId)
                    .code("ERROR-TEST-001")
                    .status(ParcelStatus.IN_WAREHOUSE)
                    .build();

            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.of(parcel));
            when(parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel))
                    .thenReturn(java.util.Optional.empty()); // No destination found

            // Act & Assert
            ResourceNotFound exception = assertThrows(ResourceNotFound.class, () -> {
                parcelService.getParcelById(parcelId);
            });

            assertTrue(exception.getMessage().contains("Not found any current destination"));
            verify(parcelDestinationRepository, times(1)).findByParcelAndIsCurrentTrue(parcel);
        }

        @Test
        @DisplayName("Should handle null senderId in toDto gracefully")
        void shouldHandleNullSenderIdInToDto() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .id(parcelId)
                    .code("ERROR-TEST-001")
                    .senderId(null) // Null sender ID
                    .receiverId(receiverId)
                    .deliveryType(DeliveryType.NORMAL)
                    .status(ParcelStatus.IN_WAREHOUSE)
                    .build();

            ParcelDestination destination = ParcelDestination.builder()
                    .destinationId(receiverDestinationId)
                    .destinationType(DestinationType.PRIMARY)
                    .isCurrent(true)
                    .parcel(parcel)
                    .build();

            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.of(parcel));
            when(parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel))
                    .thenReturn(java.util.Optional.of(destination));

            com.ds.parcel_service.application.client.DestinationResponse<com.ds.parcel_service.application.client.DesDetail> zoneResponse =
                    new com.ds.parcel_service.application.client.DestinationResponse<>();
            com.ds.parcel_service.application.client.DesDetail detail = new com.ds.parcel_service.application.client.DesDetail();
            detail.setLat(new BigDecimal("10.8505"));
            detail.setLon(new BigDecimal("106.7718"));
            zoneResponse.setResult(detail);
            when(zoneClient.getDestination(receiverDestinationId)).thenReturn(zoneResponse);
            when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act - Should not throw exception
            ParcelResponse response = parcelService.getParcelById(parcelId);

            // Assert
            assertNotNull(response);
            assertEquals(parcelId.toString(), response.getId());
            assertNull(response.getSenderName()); // Should be null when senderId is null
        }
    }

    @Nested
    @DisplayName("Exception Propagation Tests")
    class ExceptionPropagationTests {

        @Test
        @DisplayName("Should propagate IllegalArgumentException when address ID is blank")
        void shouldPropagateIllegalArgumentExceptionWhenAddressIdIsBlank() {
            // Arrange
            createRequest.setSenderAddressId(""); // Blank address ID

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                parcelService.createParcel(createRequest);
            });

            assertTrue(exception.getMessage().contains("Sender address ID is required"));
            verify(parcelRepository, never()).save(any(Parcel.class));
        }

        @Test
        @DisplayName("Should propagate ResourceNotFound when parcel not found")
        void shouldPropagateResourceNotFoundWhenParcelNotFound() {
            // Arrange
            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.empty());

            // Act & Assert
            ResourceNotFound exception = assertThrows(ResourceNotFound.class, () -> {
                parcelService.getParcelById(parcelId);
            });

            assertTrue(exception.getMessage().contains("Parcel not found"));
        }
    }
}
