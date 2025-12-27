package com.ds.parcel_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;
import com.ds.parcel_service.app_context.repositories.ParcelDestinationRepository;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.application.client.UserServiceClient;
import com.ds.parcel_service.application.client.ZoneClient;
import com.ds.parcel_service.business.v1.services.ParcelService;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.DestinationType;
import com.ds.parcel_service.common.enums.ParcelStatus;

/**
 * Complex business logic tests for ParcelService
 * Tests multiple queries, complex joins, and business rules
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService Complex Business Logic Tests")
class ParcelServiceComplexTest {

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
    private List<Parcel> mockParcels;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID().toString();
        receiverId = UUID.randomUUID().toString();
        senderAddressId = UUID.randomUUID().toString();
        receiverAddressId = UUID.randomUUID().toString();

        // Create multiple parcels for complex queries
        mockParcels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Parcel parcel = Parcel.builder()
                    .id(UUID.randomUUID())
                    .code("TEST-" + String.format("%03d", i))
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .senderAddressId(senderAddressId + "-" + i)
                    .receiverAddressId(receiverAddressId + "-" + i)
                    .deliveryType(i % 2 == 0 ? DeliveryType.NORMAL : DeliveryType.EXPRESS)
                    .status(ParcelStatus.IN_WAREHOUSE)
                    .weight(1.0 * i)
                    .value(new BigDecimal("100000").multiply(new BigDecimal(i)))
                    .priority(i % 2 == 0 ? 3 : 8) // NORMAL=3, EXPRESS=8
                    .build();
            parcel.calculatePriority();
            mockParcels.add(parcel);
        }
    }

    @Nested
    @DisplayName("Complex Query Tests - Multiple Parcels with Address IDs")
    class ComplexQueryTests {

        @Test
        @DisplayName("Should get parcels sent by customer with address IDs")
        void shouldGetParcelsSentByCustomer() {
            // Arrange
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);
            Page<Parcel> parcelPage = new PageImpl<>(mockParcels, pageable, mockParcels.size());

            when(parcelRepository.findBySenderId(eq(senderId), any(Pageable.class)))
                    .thenReturn(parcelPage);

            // Mock user service (toDto() calls getUserById for sender/receiver names)
            when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act
            PageResponse<com.ds.parcel_service.common.entities.dto.response.ParcelResponse> result =
                    parcelService.getParcelsSentByCustomer(senderId, page, size);

            // Assert
            assertNotNull(result);
            assertEquals(mockParcels.size(), result.content().size());
            
            // Verify all parcels have address IDs
            result.content().forEach(response -> {
                assertNotNull(response.getSenderAddressId());
                assertNotNull(response.getReceiverAddressId());
            });

            verify(parcelRepository, times(1)).findBySenderId(eq(senderId), any(Pageable.class));
            // Note: toDto() no longer calls parcelDestinationRepository after refactor
        }

        @Test
        @DisplayName("Should get parcels received by customer with address IDs")
        void shouldGetParcelsReceivedByCustomer() {
            // Arrange
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);
            Page<Parcel> parcelPage = new PageImpl<>(mockParcels, pageable, mockParcels.size());

            when(parcelRepository.findByReceiverId(eq(receiverId), any(Pageable.class)))
                    .thenReturn(parcelPage);

            // Mock user service (toDto() calls getUserById for sender/receiver names)
            when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act
            PageResponse<com.ds.parcel_service.common.entities.dto.response.ParcelResponse> result =
                    parcelService.getParcelsReceivedByCustomer(receiverId, page, size);

            // Assert
            assertNotNull(result);
            assertEquals(mockParcels.size(), result.content().size());
            
            // Verify all parcels have receiver address IDs
            result.content().forEach(response -> {
                assertNotNull(response.getReceiverAddressId());
            });

            verify(parcelRepository, times(1)).findByReceiverId(eq(receiverId), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter parcels by multiple criteria with address IDs")
        void shouldFilterParcelsByMultipleCriteria() {
            // Arrange
            ParcelFilterRequest filter = ParcelFilterRequest.builder()
                    .status(ParcelStatus.IN_WAREHOUSE.name()) // Filter uses String, not enum
                    .build();

            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);
            Page<Parcel> parcelPage = new PageImpl<>(mockParcels, pageable, mockParcels.size());

            when(parcelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                    .thenReturn(parcelPage);

            // Mock user service (toDto() calls getUserById for sender/receiver names)
            when(userServiceClient.getUserById(anyString())).thenReturn(null);

            // Act
            PageResponse<com.ds.parcel_service.common.entities.dto.response.ParcelResponse> result =
                    parcelService.getParcels(filter, page, size, null, null);

            // Assert
            assertNotNull(result);
            assertEquals(mockParcels.size(), result.content().size());
            
            // Verify all parcels match filter criteria and have address IDs
            result.content().forEach(response -> {
                assertEquals(ParcelStatus.IN_WAREHOUSE, response.getStatus());
                assertNotNull(response.getSenderAddressId());
                assertNotNull(response.getReceiverAddressId());
            });

            verify(parcelRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Priority-Based Business Logic Tests")
    class PriorityBasedTests {

        @Test
        @DisplayName("Should correctly assign priority based on DeliveryType for multiple parcels")
        void shouldAssignPriorityCorrectlyForMultipleParcels() {
            // Verify priority mapping for all delivery types
            DeliveryType[] types = DeliveryType.values();
            int[] expectedPriorities = {0, 3, 5, 8, 10};

            for (int i = 0; i < types.length; i++) {
                Parcel parcel = Parcel.builder()
                        .code("TEST-" + types[i].name())
                        .deliveryType(types[i])
                        .build();
                parcel.calculatePriority();

                assertEquals(expectedPriorities[i], parcel.getPriority(),
                    "Priority should match for " + types[i].name());
            }
        }

        @Test
        @DisplayName("Should update priority when DeliveryType changes")
        void shouldUpdatePriorityWhenDeliveryTypeChanges() {
            // Arrange
            Parcel parcel = Parcel.builder()
                    .code("TEST-001")
                    .deliveryType(DeliveryType.NORMAL)
                    .build();
            parcel.calculatePriority();

            // Act - Change to EXPRESS
            parcel.setDeliveryType(DeliveryType.EXPRESS);
            parcel.calculatePriority();

            // Assert
            assertEquals(8, parcel.getPriority()); // EXPRESS = 8
            assertNotEquals(3, parcel.getPriority()); // Should not be NORMAL priority
        }
    }
}
