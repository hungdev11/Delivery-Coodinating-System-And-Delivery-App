package com.ds.session.session_service.application.controllers.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ds.session.session_service.business.v1.services.AdminAssignmentService;
import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.request.AutoAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.request.ManualAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.response.AutoAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.ManualAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAssignmentController Tests")
class AdminAssignmentControllerTest {

    @Mock
    private AdminAssignmentService adminAssignmentService;

    @InjectMocks
    private AdminAssignmentController adminAssignmentController;

    @BeforeEach
    void setUp() {
        reset(adminAssignmentService);
    }

    @Nested
    @DisplayName("Manual Assignment Endpoint Tests")
    class ManualAssignmentEndpointTests {

        @Test
        @DisplayName("Should create manual assignment successfully")
        void shouldCreateManualAssignmentSuccessfully() {
            // Given
            String shipperId = "shipper-1";
            List<String> parcelIds = Arrays.asList("parcel-1", "parcel-2");
            String deliveryAddressId = "address-1";

            ManualAssignmentRequest request = ManualAssignmentRequest.builder()
                .shipperId(shipperId)
                .parcelIds(parcelIds)
                .build();

            ManualAssignmentResponse response = ManualAssignmentResponse.builder()
                .assignmentId(UUID.randomUUID())
                .shipperId(shipperId)
                .deliveryAddressId(deliveryAddressId)
                .parcelIds(parcelIds)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .build();

            when(adminAssignmentService.createManualAssignment(request)).thenReturn(response);

            // When
            ResponseEntity<BaseResponse<ManualAssignmentResponse>> result = 
                adminAssignmentController.createManualAssignment(request);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNotNull(result.getBody().getResult()); // Success if result is not null
            assertEquals(shipperId, result.getBody().getResult().getShipperId());
            assertEquals(2, result.getBody().getResult().getParcelIds().size());

            verify(adminAssignmentService, times(1)).createManualAssignment(request);
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() {
            // Given
            ManualAssignmentRequest request = ManualAssignmentRequest.builder()
                .shipperId("shipper-1")
                .parcelIds(Arrays.asList("parcel-1", "parcel-2"))
                .build();

            when(adminAssignmentService.createManualAssignment(request))
                .thenThrow(new IllegalArgumentException("All parcels must have the same delivery address"));

            // When
            ResponseEntity<BaseResponse<ManualAssignmentResponse>> result = 
                adminAssignmentController.createManualAssignment(request);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNull(result.getBody().getResult()); // Error if result is null
            assertNotNull(result.getBody().getMessage());
            assertTrue(result.getBody().getMessage().contains("same delivery address"));

            verify(adminAssignmentService, times(1)).createManualAssignment(request);
        }

        @Test
        @DisplayName("Should return 500 when service throws exception")
        void shouldReturn500WhenServiceThrowsException() {
            // Given
            ManualAssignmentRequest request = ManualAssignmentRequest.builder()
                .shipperId("shipper-1")
                .parcelIds(Arrays.asList("parcel-1"))
                .build();

            when(adminAssignmentService.createManualAssignment(request))
                .thenThrow(new RuntimeException("Database error"));

            // When
            ResponseEntity<BaseResponse<ManualAssignmentResponse>> result = 
                adminAssignmentController.createManualAssignment(request);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNull(result.getBody().getResult()); // Error if result is null
            assertNotNull(result.getBody().getMessage());
            assertTrue(result.getBody().getMessage().contains("Failed to create manual assignment"));

            verify(adminAssignmentService, times(1)).createManualAssignment(request);
        }
    }

    @Nested
    @DisplayName("Auto Assignment Endpoint Tests")
    class AutoAssignmentEndpointTests {

        @Test
        @DisplayName("Should create auto assignments successfully")
        void shouldCreateAutoAssignmentsSuccessfully() {
            // Given
            String shipperId1 = "shipper-1";
            String shipperId2 = "shipper-2";
            List<String> shipperIds = Arrays.asList(shipperId1, shipperId2);
            List<String> parcelIds = Arrays.asList("parcel-1", "parcel-2", "parcel-3");

            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .shipperIds(shipperIds)
                .parcelIds(parcelIds)
                .vehicle("motorbike")
                .mode("v2-full")
                .build();

            AutoAssignmentResponse.AssignmentInfo assignment1 = AutoAssignmentResponse.AssignmentInfo.builder()
                .assignmentId(UUID.randomUUID())
                .deliveryAddressId("address-1")
                .parcelIds(Arrays.asList("parcel-1", "parcel-2"))
                .status(AssignmentStatus.PENDING)
                .build();

            AutoAssignmentResponse.AssignmentInfo assignment2 = AutoAssignmentResponse.AssignmentInfo.builder()
                .assignmentId(UUID.randomUUID())
                .deliveryAddressId("address-2")
                .parcelIds(Arrays.asList("parcel-3"))
                .status(AssignmentStatus.PENDING)
                .build();

            Map<String, List<AutoAssignmentResponse.AssignmentInfo>> assignments = new HashMap<>();
            assignments.put(shipperId1, Arrays.asList(assignment1));
            assignments.put(shipperId2, Arrays.asList(assignment2));

            AutoAssignmentResponse response = AutoAssignmentResponse.builder()
                .assignments(assignments)
                .unassignedParcels(Arrays.asList())
                .statistics(AutoAssignmentResponse.Statistics.builder()
                    .totalShippers(2)
                    .totalParcels(3)
                    .assignedParcels(3)
                    .averageParcelsPerShipper(1.5)
                    .workloadVariance(0.25)
                    .build())
                .build();

            when(adminAssignmentService.createAutoAssignment(request)).thenReturn(response);

            // When
            ResponseEntity<BaseResponse<AutoAssignmentResponse>> result = 
                adminAssignmentController.createAutoAssignment(request);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.CREATED, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNotNull(result.getBody().getResult()); // Success if result is not null
            assertEquals(2, result.getBody().getResult().getAssignments().size());
            assertEquals(3, result.getBody().getResult().getStatistics().getAssignedParcels());

            verify(adminAssignmentService, times(1)).createAutoAssignment(request);
        }

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400WhenValidationFails() {
            // Given
            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .shipperIds(Arrays.asList("shipper-1"))
                .parcelIds(Arrays.asList("parcel-1"))
                .build();

            when(adminAssignmentService.createAutoAssignment(request))
                .thenThrow(new IllegalArgumentException("All parcels are already assigned"));

            // When
            ResponseEntity<BaseResponse<AutoAssignmentResponse>> result = 
                adminAssignmentController.createAutoAssignment(request);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNull(result.getBody().getResult()); // Error if result is null
            assertNotNull(result.getBody().getMessage());
            assertTrue(result.getBody().getMessage().contains("already assigned"));

            verify(adminAssignmentService, times(1)).createAutoAssignment(request);
        }

        @Test
        @DisplayName("Should return 500 when VRP API fails")
        void shouldReturn500WhenVRPApiFails() {
            // Given
            AutoAssignmentRequest request = AutoAssignmentRequest.builder()
                .shipperIds(Arrays.asList("shipper-1"))
                .parcelIds(Arrays.asList("parcel-1"))
                .build();

            when(adminAssignmentService.createAutoAssignment(request))
                .thenThrow(new RuntimeException("VRP solver failed"));

            // When
            ResponseEntity<BaseResponse<AutoAssignmentResponse>> result = 
                adminAssignmentController.createAutoAssignment(request);

            // Then
            assertNotNull(result);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
            assertNotNull(result.getBody());
            assertNull(result.getBody().getResult()); // Error if result is null
            assertNotNull(result.getBody().getMessage());
            assertTrue(result.getBody().getMessage().contains("Failed to create auto assignment"));

            verify(adminAssignmentService, times(1)).createAutoAssignment(request);
        }
    }
}
