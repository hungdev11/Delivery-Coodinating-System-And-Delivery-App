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

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;
import com.ds.parcel_service.app_context.repositories.ParcelDestinationRepository;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.application.client.UserServiceClient;
import com.ds.parcel_service.application.client.ZoneClient;
import com.ds.parcel_service.business.v1.services.ParcelService;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.DestinationType;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.exceptions.ResourceNotFound;

/**
 * Comprehensive tests for ParcelService
 * Tests CRUD operations with new address ID model
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService Tests")
class ParcelServiceTest {

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

    private String senderId;
    private String receiverId;
    private String senderAddressId;
    private String receiverAddressId;
    private String senderDestinationId;
    private String receiverDestinationId;
    private UUID parcelId;
    private ParcelCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID().toString();
        receiverId = UUID.randomUUID().toString();
        senderAddressId = UUID.randomUUID().toString();
        receiverAddressId = UUID.randomUUID().toString();
        senderDestinationId = UUID.randomUUID().toString();
        receiverDestinationId = UUID.randomUUID().toString();
        parcelId = UUID.randomUUID();

        createRequest = ParcelCreateRequest.builder()
                .code("TEST-001")
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
    @DisplayName("Create Parcel Tests")
    class CreateParcelTests {

        @Test
        @DisplayName("Should create parcel with address IDs successfully")
        void shouldCreateParcelWithAddressIds() {
            // Arrange
            UserServiceClient.UserAddressInfo senderAddress = new UserServiceClient.UserAddressInfo();
            senderAddress.setId(senderAddressId);
            senderAddress.setDestinationId(senderDestinationId);

            UserServiceClient.UserAddressInfo receiverAddress = new UserServiceClient.UserAddressInfo();
            receiverAddress.setId(receiverAddressId);
            receiverAddress.setDestinationId(receiverDestinationId);

            when(userServiceClient.getUserAddressById(senderAddressId)).thenReturn(senderAddress);
            when(userServiceClient.getUserAddressById(receiverAddressId)).thenReturn(receiverAddress);
            when(parcelRepository.existsByCode("TEST-001")).thenReturn(false);
            when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
                Parcel p = invocation.getArgument(0);
                p.setId(parcelId);
                p.calculatePriority(); // Manually trigger priority calculation
                return p;
            });
            when(parcelDestinationRepository.save(any(ParcelDestination.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(parcelDestinationRepository.findAllByParcelAndIsCurrentTrue(any(Parcel.class))).thenReturn(java.util.Collections.emptyList());

            // Act
            ParcelResponse response = parcelService.createParcel(createRequest);

            // Assert
            assertNotNull(response);
            assertEquals("TEST-001", response.getCode());
            assertEquals(senderId, response.getSenderId());
            assertEquals(receiverId, response.getReceiverId());
            assertEquals(senderAddressId, response.getSenderAddressId());
            assertEquals(receiverAddressId, response.getReceiverAddressId());
            assertEquals(DeliveryType.NORMAL, response.getDeliveryType());
            assertEquals(3, response.getPriority()); // NORMAL priority = 3

            verify(userServiceClient, times(1)).getUserAddressById(senderAddressId);
            verify(userServiceClient, times(1)).getUserAddressById(receiverAddressId);
            verify(parcelRepository, times(1)).save(any(Parcel.class));
            verify(parcelDestinationRepository, times(2)).save(any(ParcelDestination.class));
        }

        @Test
        @DisplayName("Should throw exception when sender address not found")
        void shouldThrowExceptionWhenSenderAddressNotFound() {
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
        @DisplayName("Should throw exception when receiver address not found")
        void shouldThrowExceptionWhenReceiverAddressNotFound() {
            // Arrange
            UserServiceClient.UserAddressInfo senderAddress = new UserServiceClient.UserAddressInfo();
            senderAddress.setId(senderAddressId);
            senderAddress.setDestinationId(senderDestinationId);

            when(userServiceClient.getUserAddressById(senderAddressId)).thenReturn(senderAddress);
            when(userServiceClient.getUserAddressById(receiverAddressId)).thenReturn(null);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                parcelService.createParcel(createRequest);
            });

            assertEquals("Receiver address not found: " + receiverAddressId, exception.getMessage());
            verify(parcelRepository, never()).save(any(Parcel.class));
        }

        @Test
        @DisplayName("Should calculate priority from DeliveryType automatically")
        void shouldCalculatePriorityFromDeliveryType() {
            // Test all DeliveryTypes
            DeliveryType[] types = DeliveryType.values();
            int[] expectedPriorities = {0, 3, 5, 8, 10}; // ECONOMY, NORMAL, FAST, EXPRESS, URGENT

            UserServiceClient.UserAddressInfo senderAddress = new UserServiceClient.UserAddressInfo();
            senderAddress.setId(senderAddressId);
            senderAddress.setDestinationId(senderDestinationId);

            UserServiceClient.UserAddressInfo receiverAddress = new UserServiceClient.UserAddressInfo();
            receiverAddress.setId(receiverAddressId);
            receiverAddress.setDestinationId(receiverDestinationId);

            when(userServiceClient.getUserAddressById(senderAddressId)).thenReturn(senderAddress);
            when(userServiceClient.getUserAddressById(receiverAddressId)).thenReturn(receiverAddress);
            when(parcelRepository.existsByCode(anyString())).thenReturn(false);
            when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
                Parcel p = invocation.getArgument(0);
                p.setId(parcelId);
                p.calculatePriority(); // Manually trigger priority calculation
                return p;
            });
            when(parcelDestinationRepository.save(any(ParcelDestination.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(parcelDestinationRepository.findAllByParcelAndIsCurrentTrue(any(Parcel.class))).thenReturn(java.util.Collections.emptyList());

            for (int i = 0; i < types.length; i++) {
                createRequest.setDeliveryType(types[i].name());
                createRequest.setCode("TEST-00" + (i + 1)); // Unique code for each iteration

                ParcelResponse response = parcelService.createParcel(createRequest);

                assertEquals(expectedPriorities[i], response.getPriority(), 
                    "Priority should match for " + types[i].name());
            }
        }
    }

    @Nested
    @DisplayName("Get Parcel Tests")
    class GetParcelTests {

        @Test
        @DisplayName("Should get parcel by ID successfully")
        void shouldGetParcelById() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .id(parcelId)
                    .code("TEST-001")
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
            when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act
            ParcelResponse response = parcelService.getParcelById(parcelId);

            // Assert
            assertNotNull(response);
            assertEquals(parcelId.toString(), response.getId());
            assertEquals("TEST-001", response.getCode());
            assertEquals(senderAddressId, response.getSenderAddressId());
            assertEquals(receiverAddressId, response.getReceiverAddressId());
        }

        @Test
        @DisplayName("Should throw exception when parcel not found")
        void shouldThrowExceptionWhenParcelNotFound() {
            // Arrange
            when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFound.class, () -> {
                parcelService.getParcelById(parcelId);
            });
        }
    }

    @Nested
    @DisplayName("Priority Calculation Tests")
    class PriorityCalculationTests {

        @Test
        @DisplayName("Should set priority on entity pre-persist")
        void shouldSetPriorityOnPrePersist() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .code("TEST-001")
                    .deliveryType(DeliveryType.URGENT)
                    .build();

            // Act - Trigger @PrePersist (simulated by calling calculatePriority manually)
            parcel.calculatePriority();

            // Assert
            assertEquals(10, parcel.getPriority()); // URGENT = 10
        }

        @Test
        @DisplayName("Should update priority on entity pre-update when delivery type changes")
        void shouldUpdatePriorityOnPreUpdate() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .code("TEST-001")
                    .deliveryType(DeliveryType.NORMAL)
                    .priority(3)
                    .build();

            // Act - Change delivery type and trigger @PreUpdate
            parcel.setDeliveryType(DeliveryType.EXPRESS);
            parcel.calculatePriority();

            // Assert
            assertEquals(8, parcel.getPriority()); // EXPRESS = 8
        }
    }
}
