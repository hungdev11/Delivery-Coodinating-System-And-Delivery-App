package com.ds.session.session_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliveryAssignmentParcel;
import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentParcelRepository;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.response.BaseResponse;
import com.ds.session.session_service.application.client.zoneclient.response.VRPAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.request.AutoAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.request.ManualAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.response.AutoAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.ManualAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.SessionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAssignmentService Tests")
class AdminAssignmentServiceTest {

    @Mock
    private DeliveryAssignmentRepository assignmentRepository;

    @Mock
    private DeliveryAssignmentParcelRepository assignmentParcelRepository;

    @Mock
    private ParcelServiceClient parcelServiceClient;

    @Mock
    private ZoneServiceClient zoneServiceClient;

    @Mock
    private DeliverySessionRepository sessionRepository;

    @InjectMocks
    private AdminAssignmentService adminAssignmentService;

    private ParcelResponse createMockParcel(String id, String deliveryAddressId, double lat, double lon, String deliveryType) {
        return ParcelResponse.builder()
            .id(id)
            .receiverAddressId(deliveryAddressId)
            .lat(BigDecimal.valueOf(lat))
            .lon(BigDecimal.valueOf(lon))
            .deliveryType(deliveryType)
            .status("PENDING")
            .build();
    }

    private DeliverySession createMockSession(UUID sessionId, String deliveryManId, SessionStatus status) {
        DeliverySession session = DeliverySession.builder()
            .id(sessionId)
            .deliveryManId(deliveryManId)
            .status(status)
            .build();
        return session;
    }

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(assignmentRepository, assignmentParcelRepository, parcelServiceClient, zoneServiceClient, sessionRepository);
    }

    @Nested
    @DisplayName("Manual Assignment Tests")
    class ManualAssignmentTests {

        @Test
        @DisplayName("Should create manual assignment successfully")
        void shouldCreateManualAssignmentSuccessfully() {
            // Given
            String shipperId = "shipper-1";
            String parcelId1 = "parcel-1";
            String parcelId2 = "parcel-2";
            String deliveryAddressId = "address-1";
            UUID sessionId = UUID.randomUUID();

            DeliverySession session = createMockSession(sessionId, shipperId, SessionStatus.CREATED);
            when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));

            ManualAssignmentRequest request = ManualAssignmentRequest.builder()
                .sessionId(sessionId.toString())
                .shipperId(shipperId)
                .parcelIds(Arrays.asList(parcelId1, parcelId2))
                .build();

            ParcelResponse parcel1 = createMockParcel(parcelId1, deliveryAddressId, 10.8505, 106.7718, "NORMAL");
            ParcelResponse parcel2 = createMockParcel(parcelId2, deliveryAddressId, 10.8550, 106.7800, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId1)).thenReturn(parcel1);
            when(parcelServiceClient.fetchParcelResponse(parcelId2)).thenReturn(parcel2);
            when(assignmentParcelRepository.existsByParcelId(anyString())).thenReturn(false);

            DeliveryAssignment savedAssignment = DeliveryAssignment.builder()
                .id(UUID.randomUUID())
                .shipperId(shipperId)
                .deliveryAddressId(deliveryAddressId)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .parcels(new ArrayList<>())
                .build();

            when(assignmentRepository.save(any(DeliveryAssignment.class))).thenReturn(savedAssignment);

            // When
            ManualAssignmentResponse response = adminAssignmentService.createManualAssignment(request);

            // Then
            assertNotNull(response);
            assertEquals(shipperId, response.getShipperId());
            assertEquals(deliveryAddressId, response.getDeliveryAddressId());
            assertEquals(AssignmentStatus.PENDING, response.getStatus());
            assertEquals(2, response.getParcelIds().size());
            assertTrue(response.getParcelIds().contains(parcelId1));
            assertTrue(response.getParcelIds().contains(parcelId2));

            verify(assignmentRepository, times(2)).save(any(DeliveryAssignment.class));
        }

        @Test
        @DisplayName("Should throw exception when parcels have different delivery addresses")
        void shouldThrowExceptionWhenParcelsHaveDifferentDeliveryAddresses() {
            // Given
            String shipperId = "shipper-1";
            String parcelId1 = "parcel-1";
            String parcelId2 = "parcel-2";
            String address1 = "address-1";
            String address2 = "address-2";
            UUID sessionId = UUID.randomUUID();

            DeliverySession session = createMockSession(sessionId, shipperId, SessionStatus.CREATED);
            when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));

            ManualAssignmentRequest request = ManualAssignmentRequest.builder()
                .sessionId(sessionId.toString())
                .shipperId(shipperId)
                .parcelIds(Arrays.asList(parcelId1, parcelId2))
                .build();

            ParcelResponse parcel1 = createMockParcel(parcelId1, address1, 10.8505, 106.7718, "NORMAL");
            ParcelResponse parcel2 = createMockParcel(parcelId2, address2, 10.8550, 106.7800, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId1)).thenReturn(parcel1);
            when(parcelServiceClient.fetchParcelResponse(parcelId2)).thenReturn(parcel2);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                adminAssignmentService.createManualAssignment(request);
            });

            assertTrue(exception.getMessage().contains("same delivery address"));
            verify(assignmentRepository, never()).save(any(DeliveryAssignment.class));
        }

        @Test
        @DisplayName("Should throw exception when parcel is already assigned")
        void shouldThrowExceptionWhenParcelAlreadyAssigned() {
            // Given
            String shipperId = "shipper-1";
            String parcelId = "parcel-1";
            String deliveryAddressId = "address-1";
            UUID sessionId = UUID.randomUUID();

            DeliverySession session = createMockSession(sessionId, shipperId, SessionStatus.CREATED);
            when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));

            ManualAssignmentRequest request = ManualAssignmentRequest.builder()
                .sessionId(sessionId.toString())
                .shipperId(shipperId)
                .parcelIds(Arrays.asList(parcelId))
                .build();

            ParcelResponse parcel = createMockParcel(parcelId, deliveryAddressId, 10.8505, 106.7718, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId)).thenReturn(parcel);
            when(assignmentParcelRepository.existsByParcelId(parcelId)).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                adminAssignmentService.createManualAssignment(request);
            });

            assertTrue(exception.getMessage().contains("already assigned"));
            verify(assignmentRepository, never()).save(any(DeliveryAssignment.class));
        }
    }

    @Nested
    @DisplayName("Auto Assignment Tests")
    class AutoAssignmentTests {

        @Test
        @DisplayName("Should create auto assignments successfully")
        void shouldCreateAutoAssignmentsSuccessfully() {
            // Given
            String shipperId1 = "shipper-1";
            String shipperId2 = "shipper-2";
            String parcelId1 = "parcel-1";
            String parcelId2 = "parcel-2";
            String parcelId3 = "parcel-3";
            String deliveryAddressId = "address-1";

            UUID sessionId1 = UUID.randomUUID();
            UUID sessionId2 = UUID.randomUUID();
            
            DeliverySession session1 = createMockSession(sessionId1, shipperId1, SessionStatus.CREATED);
            DeliverySession session2 = createMockSession(sessionId2, shipperId2, SessionStatus.CREATED);
            
            when(sessionRepository.findById(sessionId1)).thenReturn(java.util.Optional.of(session1));
            when(sessionRepository.findById(sessionId2)).thenReturn(java.util.Optional.of(session2));
            
            Map<String, String> shipperSessionMap = new HashMap<>();
            shipperSessionMap.put(shipperId1, sessionId1.toString());
            shipperSessionMap.put(shipperId2, sessionId2.toString());

            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .shipperSessionMap(shipperSessionMap)
                .shipperIds(Arrays.asList(shipperId1, shipperId2))
                .parcelIds(Arrays.asList(parcelId1, parcelId2, parcelId3))
                .vehicle("motorbike")
                .mode("v2-full")
                .build();

            ParcelResponse parcel1 = createMockParcel(parcelId1, deliveryAddressId, 10.8505, 106.7718, "NORMAL");
            ParcelResponse parcel2 = createMockParcel(parcelId2, deliveryAddressId, 10.8550, 106.7800, "NORMAL");
            ParcelResponse parcel3 = createMockParcel(parcelId3, deliveryAddressId, 10.8623, 106.8032, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId1)).thenReturn(parcel1);
            when(parcelServiceClient.fetchParcelResponse(parcelId2)).thenReturn(parcel2);
            when(parcelServiceClient.fetchParcelResponse(parcelId3)).thenReturn(parcel3);
            when(assignmentParcelRepository.existsByParcelId(anyString())).thenReturn(false);

            // Mock VRP response
            VRPAssignmentResponse.VRPTaskDto task1 = VRPAssignmentResponse.VRPTaskDto.builder()
                .orderId(parcelId1)
                .sequenceIndex(0)
                .estimatedArrivalTime("2024-01-01T08:30:00Z")
                .travelTimeFromPreviousStop(600)
                .build();

            VRPAssignmentResponse.VRPTaskDto task2 = VRPAssignmentResponse.VRPTaskDto.builder()
                .orderId(parcelId2)
                .sequenceIndex(1)
                .estimatedArrivalTime("2024-01-01T08:45:00Z")
                .travelTimeFromPreviousStop(900)
                .build();

            VRPAssignmentResponse.VRPTaskDto task3 = VRPAssignmentResponse.VRPTaskDto.builder()
                .orderId(parcelId3)
                .sequenceIndex(0)
                .estimatedArrivalTime("2024-01-01T08:30:00Z")
                .travelTimeFromPreviousStop(600)
                .build();

            Map<String, List<VRPAssignmentResponse.VRPTaskDto>> assignments = new HashMap<>();
            assignments.put(shipperId1, Arrays.asList(task1, task2));
            assignments.put(shipperId2, Arrays.asList(task3));

            VRPAssignmentResponse vrpResponse = VRPAssignmentResponse.builder()
                .assignments(assignments)
                .unassignedOrders(new ArrayList<>())
                .statistics(VRPAssignmentResponse.Statistics.builder()
                    .totalShippers(2)
                    .totalOrders(3)
                    .assignedOrders(3)
                    .averageOrdersPerShipper(1.5)
                    .workloadVariance(0.25)
                    .build())
                .build();

            BaseResponse<VRPAssignmentResponse> baseResponse = BaseResponse.<VRPAssignmentResponse>builder()
                .success(true)
                .result(vrpResponse)
                .build();

            when(zoneServiceClient.solveVRPAssignment(any())).thenReturn(baseResponse);

            DeliveryAssignment assignment1 = DeliveryAssignment.builder()
                .id(UUID.randomUUID())
                .shipperId(shipperId1)
                .deliveryAddressId(deliveryAddressId)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .parcels(new ArrayList<>())
                .build();

            DeliveryAssignment assignment2 = DeliveryAssignment.builder()
                .id(UUID.randomUUID())
                .shipperId(shipperId2)
                .deliveryAddressId(deliveryAddressId)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .parcels(new ArrayList<>())
                .build();

            when(assignmentRepository.save(any(DeliveryAssignment.class)))
                .thenReturn(assignment1)
                .thenReturn(assignment2);

            // When
            AutoAssignmentResponse response = adminAssignmentService.createAutoAssignment(request);

            // Then
            assertNotNull(response);
            assertNotNull(response.getAssignments());
            assertEquals(2, response.getAssignments().size());
            assertTrue(response.getAssignments().containsKey(shipperId1));
            assertTrue(response.getAssignments().containsKey(shipperId2));
            
            List<AutoAssignmentResponse.AssignmentInfo> shipper1Assignments = response.getAssignments().get(shipperId1);
            assertNotNull(shipper1Assignments);
            assertEquals(1, shipper1Assignments.size()); // All parcels in same address = 1 assignment
            assertEquals(2, shipper1Assignments.get(0).getParcelIds().size());

            List<AutoAssignmentResponse.AssignmentInfo> shipper2Assignments = response.getAssignments().get(shipperId2);
            assertNotNull(shipper2Assignments);
            assertEquals(1, shipper2Assignments.size());
            assertEquals(1, shipper2Assignments.get(0).getParcelIds().size());

            assertNotNull(response.getStatistics());
            assertEquals(2, response.getStatistics().getTotalShippers());
            assertEquals(3, response.getStatistics().getTotalParcels());
            assertEquals(3, response.getStatistics().getAssignedParcels());

            verify(zoneServiceClient, times(1)).solveVRPAssignment(any());
            verify(assignmentRepository, atLeast(2)).save(any(DeliveryAssignment.class));
        }

        @Test
        @DisplayName("Should filter out already assigned parcels")
        void shouldFilterOutAlreadyAssignedParcels() {
            // Given
            String shipperId = "shipper-1";
            String parcelId1 = "parcel-1";
            String parcelId2 = "parcel-2"; // Already assigned
            String deliveryAddressId = "address-1";

            UUID sessionId = UUID.randomUUID();
            DeliverySession session = createMockSession(sessionId, shipperId, SessionStatus.CREATED);
            when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));
            
            Map<String, String> shipperSessionMap = new HashMap<>();
            shipperSessionMap.put(shipperId, sessionId.toString());

            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .shipperSessionMap(shipperSessionMap)
                .shipperIds(Arrays.asList(shipperId))
                .parcelIds(Arrays.asList(parcelId1, parcelId2))
                .build();

            ParcelResponse parcel1 = createMockParcel(parcelId1, deliveryAddressId, 10.8505, 106.7718, "NORMAL");
            ParcelResponse parcel2 = createMockParcel(parcelId2, deliveryAddressId, 10.8550, 106.7800, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId1)).thenReturn(parcel1);
            when(parcelServiceClient.fetchParcelResponse(parcelId2)).thenReturn(parcel2);
            when(assignmentParcelRepository.existsByParcelId(parcelId1)).thenReturn(false);
            when(assignmentParcelRepository.existsByParcelId(parcelId2)).thenReturn(true); // Already assigned

            // Mock VRP response with only parcel1
            VRPAssignmentResponse.VRPTaskDto task1 = VRPAssignmentResponse.VRPTaskDto.builder()
                .orderId(parcelId1)
                .sequenceIndex(0)
                .estimatedArrivalTime("2024-01-01T08:30:00Z")
                .travelTimeFromPreviousStop(600)
                .build();

            Map<String, List<VRPAssignmentResponse.VRPTaskDto>> assignments = new HashMap<>();
            assignments.put(shipperId, Arrays.asList(task1));

            VRPAssignmentResponse vrpResponse = VRPAssignmentResponse.builder()
                .assignments(assignments)
                .unassignedOrders(new ArrayList<>())
                .build();

            BaseResponse<VRPAssignmentResponse> baseResponse = BaseResponse.<VRPAssignmentResponse>builder()
                .success(true)
                .result(vrpResponse)
                .build();

            when(zoneServiceClient.solveVRPAssignment(any())).thenReturn(baseResponse);

            DeliveryAssignment assignment = DeliveryAssignment.builder()
                .id(UUID.randomUUID())
                .shipperId(shipperId)
                .deliveryAddressId(deliveryAddressId)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .parcels(new ArrayList<>())
                .build();

            when(assignmentRepository.save(any(DeliveryAssignment.class))).thenReturn(assignment);

            // When
            AutoAssignmentResponse response = adminAssignmentService.createAutoAssignment(request);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getAssignments().size());
            List<AutoAssignmentResponse.AssignmentInfo> shipperAssignments = response.getAssignments().get(shipperId);
            assertNotNull(shipperAssignments);
            assertEquals(1, shipperAssignments.size());
            assertEquals(1, shipperAssignments.get(0).getParcelIds().size());
            assertTrue(shipperAssignments.get(0).getParcelIds().contains(parcelId1));
            assertFalse(shipperAssignments.get(0).getParcelIds().contains(parcelId2));
        }

        @Test
        @DisplayName("Should throw exception when VRP API fails")
        void shouldThrowExceptionWhenVRPApiFails() {
            // Given
            String shipperId = "shipper-1";
            String parcelId = "parcel-1";

            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .shipperIds(Arrays.asList(shipperId))
                .parcelIds(Arrays.asList(parcelId))
                .build();

            ParcelResponse parcel = createMockParcel(parcelId, "address-1", 10.8505, 106.7718, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId)).thenReturn(parcel);
            when(assignmentParcelRepository.existsByParcelId(parcelId)).thenReturn(false);

            BaseResponse<VRPAssignmentResponse> baseResponse = BaseResponse.<VRPAssignmentResponse>builder()
                .success(false)
                .message("VRP solver failed")
                .build();

            when(zoneServiceClient.solveVRPAssignment(any())).thenReturn(baseResponse);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                adminAssignmentService.createAutoAssignment(request);
            });

            assertTrue(exception.getMessage().contains("Failed to solve VRP assignment"));
            verify(assignmentRepository, never()).save(any(DeliveryAssignment.class));
        }

        @Test
        @DisplayName("Should throw exception when all parcels are already assigned")
        void shouldThrowExceptionWhenAllParcelsAlreadyAssigned() {
            // Given
            String parcelId = "parcel-1";

            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .parcelIds(Arrays.asList(parcelId))
                .build();

            ParcelResponse parcel = createMockParcel(parcelId, "address-1", 10.8505, 106.7718, "NORMAL");

            when(parcelServiceClient.fetchParcelResponse(parcelId)).thenReturn(parcel);
            when(assignmentParcelRepository.existsByParcelId(parcelId)).thenReturn(true); // Already assigned

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                adminAssignmentService.createAutoAssignment(request);
            });

            assertTrue(exception.getMessage().contains("already assigned"));
            verify(zoneServiceClient, never()).solveVRPAssignment(any());
        }
    }
}
