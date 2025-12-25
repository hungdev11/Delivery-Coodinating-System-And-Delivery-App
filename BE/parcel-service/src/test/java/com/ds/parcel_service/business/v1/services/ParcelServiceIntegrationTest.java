package com.ds.parcel_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.DestinationType;
import com.ds.parcel_service.common.enums.ParcelStatus;

/**
 * Integration-style tests for ParcelService
 * Simulates complex workflows involving multiple services and queries
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService Integration-Style Tests")
class ParcelServiceIntegrationTest {

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

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID().toString();
        receiverId = UUID.randomUUID().toString();
        senderAddressId = UUID.randomUUID().toString();
        receiverAddressId = UUID.randomUUID().toString();
        senderDestinationId = UUID.randomUUID().toString();
        receiverDestinationId = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("Should create parcel and fetch with all related data (simulating complete workflow)")
    void shouldCreateAndFetchParcelWithAllRelatedData() {
        // Arrange - Create request
        ParcelCreateRequest createRequest = ParcelCreateRequest.builder()
                .code("INTEGRATION-TEST-001")
                .senderId(senderId)
                .receiverId(receiverId)
                .senderAddressId(senderAddressId)
                .receiverAddressId(receiverAddressId)
                .deliveryType("EXPRESS")
                .weight(2.5)
                .value(new BigDecimal("500000"))
                .windowStart(LocalTime.of(9, 0))
                .windowEnd(LocalTime.of(17, 0))
                .build();

        // Mock UserAddress lookups
        UserServiceClient.UserAddressInfo senderAddress = new UserServiceClient.UserAddressInfo();
        senderAddress.setId(senderAddressId);
        senderAddress.setDestinationId(senderDestinationId);

        UserServiceClient.UserAddressInfo receiverAddress = new UserServiceClient.UserAddressInfo();
        receiverAddress.setId(receiverAddressId);
        receiverAddress.setDestinationId(receiverDestinationId);

        when(userServiceClient.getUserAddressById(senderAddressId)).thenReturn(senderAddress);
        when(userServiceClient.getUserAddressById(receiverAddressId)).thenReturn(receiverAddress);

        // Mock repository operations
        UUID parcelId = UUID.randomUUID();
        when(parcelRepository.existsByCode("INTEGRATION-TEST-001")).thenReturn(false);
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel p = invocation.getArgument(0);
            p.setId(parcelId);
            p.calculatePriority();
            return p;
        });
        when(parcelDestinationRepository.save(any(ParcelDestination.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parcelDestinationRepository.findAllByParcelAndIsCurrentTrue(any(Parcel.class))).thenReturn(List.of());

        // Act - Create parcel
        ParcelResponse createResponse = parcelService.createParcel(createRequest);

        // Assert creation
        assertNotNull(createResponse);
        assertEquals(parcelId.toString(), createResponse.getId());
        assertEquals(senderAddressId, createResponse.getSenderAddressId());
        assertEquals(receiverAddressId, createResponse.getReceiverAddressId());
        assertEquals(8, createResponse.getPriority()); // EXPRESS = 8

        // Arrange - Fetch parcel by ID
        Parcel savedParcel = Parcel.builder()
                .id(parcelId)
                .code("INTEGRATION-TEST-001")
                .senderId(senderId)
                .receiverId(receiverId)
                .senderAddressId(senderAddressId)
                .receiverAddressId(receiverAddressId)
                .deliveryType(DeliveryType.EXPRESS)
                .status(ParcelStatus.IN_WAREHOUSE)
                .weight(2.5)
                .value(new BigDecimal("500000"))
                .priority(8)
                .build();

        ParcelDestination destination = ParcelDestination.builder()
                .destinationId(receiverDestinationId)
                .destinationType(DestinationType.PRIMARY)
                .isCurrent(true)
                .parcel(savedParcel)
                .build();

        when(parcelRepository.findById(parcelId)).thenReturn(java.util.Optional.of(savedParcel));
        when(parcelDestinationRepository.findByParcelAndIsCurrentTrue(savedParcel))
                .thenReturn(java.util.Optional.of(destination));

        com.ds.parcel_service.application.client.DestinationResponse<com.ds.parcel_service.application.client.DesDetail> zoneResponse =
                new com.ds.parcel_service.application.client.DestinationResponse<>();
        com.ds.parcel_service.application.client.DesDetail detail = new com.ds.parcel_service.application.client.DesDetail();
        detail.setLat(new BigDecimal("10.8505"));
        detail.setLon(new BigDecimal("106.7718"));
        zoneResponse.setResult(detail);
        when(zoneClient.getDestination(receiverDestinationId)).thenReturn(zoneResponse);
        when(userServiceClient.getUserById(anyString())).thenReturn(null);

        // Act - Fetch parcel
        ParcelResponse fetchResponse = parcelService.getParcelById(parcelId);

        // Assert fetch
        assertNotNull(fetchResponse);
        assertEquals(parcelId.toString(), fetchResponse.getId());
        assertEquals("INTEGRATION-TEST-001", fetchResponse.getCode());
        assertEquals(senderAddressId, fetchResponse.getSenderAddressId());
        assertEquals(receiverAddressId, fetchResponse.getReceiverAddressId());
        assertEquals(8, fetchResponse.getPriority());
        assertNotNull(fetchResponse.getLat());
        assertNotNull(fetchResponse.getLon());

        // Verify all service interactions
        verify(userServiceClient, atLeastOnce()).getUserAddressById(senderAddressId); // Called during create
        verify(userServiceClient, atLeastOnce()).getUserAddressById(receiverAddressId);
        verify(parcelRepository, times(1)).save(any(Parcel.class));
        verify(parcelRepository, times(1)).findById(parcelId);
        verify(zoneClient, times(1)).getDestination(receiverDestinationId);
        verify(parcelDestinationRepository, times(2)).save(any(ParcelDestination.class)); // Sender + receiver destinations
    }
}
