package com.ds.session.session_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliveryAssignmentParcel;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.common.mapper.ParcelMapper;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryAssignmentService Story 9 Tests - Shipper Accept Task")
class DeliveryAssignmentServiceStory9Test {

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private ParcelServiceClient parcelServiceClient;

    @Mock
    private ParcelMapper parcelMapper;

    @InjectMocks
    private DeliveryAssignmentService deliveryAssignmentService;

    private DeliveryAssignment createMockAssignment(UUID id, String shipperId, AssignmentStatus status, String... parcelIds) {
        DeliveryAssignment assignment = DeliveryAssignment.builder()
            .id(id)
            .shipperId(shipperId)
            .status(status)
            .assignedAt(LocalDateTime.now())
            .parcels(new ArrayList<>())
            .build();

        for (String parcelId : parcelIds) {
            DeliveryAssignmentParcel assignmentParcel = DeliveryAssignmentParcel.builder()
                .parcelId(parcelId)
                .assignment(assignment)
                .build();
            assignment.addParcel(assignmentParcel);
        }

        return assignment;
    }

    @BeforeEach
    void setUp() {
        reset(deliveryAssignmentRepository, parcelServiceClient, parcelMapper);
    }

    @Test
    @DisplayName("Should accept PENDING assignment and set status to ACCEPTED")
    void shouldAcceptPendingAssignment() {
        // Given
        UUID shipperId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        String parcelId = "parcel-1";

        DeliveryAssignment assignment = createMockAssignment(assignmentId, shipperId.toString(), AssignmentStatus.PENDING, parcelId);

        when(deliveryAssignmentRepository.findById(assignmentId)).thenReturn(java.util.Optional.of(assignment));

        ParcelResponse parcelResponse = ParcelResponse.builder()
            .id(parcelId)
            .receiverName("John Doe")
            .status("PENDING")
            .build();

        when(parcelServiceClient.fetchParcelResponse(parcelId)).thenReturn(parcelResponse);
        when(parcelMapper.toParcelInfo(parcelResponse)).thenReturn(null); // Simplified for test

        // When
        DeliveryAssignmentResponse response = deliveryAssignmentService.acceptTask(shipperId, assignmentId);

        // Then
        assertNotNull(response);
        assertEquals(AssignmentStatus.ACCEPTED, assignment.getStatus());
        assertNotNull(assignment.getScanedAt());

        verify(deliveryAssignmentRepository, times(1)).save(assignment);
    }

    @Test
    @DisplayName("Should accept ASSIGNED assignment (backward compatibility)")
    void shouldAcceptAssignedAssignment() {
        // Given
        UUID shipperId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        String parcelId = "parcel-1";

        DeliveryAssignment assignment = createMockAssignment(assignmentId, shipperId.toString(), AssignmentStatus.ASSIGNED, parcelId);

        when(deliveryAssignmentRepository.findById(assignmentId)).thenReturn(java.util.Optional.of(assignment));

        ParcelResponse parcelResponse = ParcelResponse.builder()
            .id(parcelId)
            .receiverName("John Doe")
            .status("PENDING")
            .build();

        when(parcelServiceClient.fetchParcelResponse(parcelId)).thenReturn(parcelResponse);
        when(parcelMapper.toParcelInfo(parcelResponse)).thenReturn(null);

        // When
        DeliveryAssignmentResponse response = deliveryAssignmentService.acceptTask(shipperId, assignmentId);

        // Then
        assertNotNull(response);
        assertEquals(AssignmentStatus.ACCEPTED, assignment.getStatus());

        verify(deliveryAssignmentRepository, times(1)).save(assignment);
    }

    @Test
    @DisplayName("Should throw exception when assignment is not in PENDING or ASSIGNED status")
    void shouldThrowExceptionWhenAssignmentNotPendingOrAssigned() {
        // Given
        UUID shipperId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        DeliveryAssignment assignment = createMockAssignment(assignmentId, shipperId.toString(), AssignmentStatus.IN_PROGRESS, "parcel-1");

        when(deliveryAssignmentRepository.findById(assignmentId)).thenReturn(java.util.Optional.of(assignment));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            deliveryAssignmentService.acceptTask(shipperId, assignmentId);
        });

        assertTrue(exception.getMessage().contains("not in PENDING or ASSIGNED status"));
        verify(deliveryAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when assignment does not belong to shipper")
    void shouldThrowExceptionWhenAssignmentDoesNotBelongToShipper() {
        // Given
        UUID shipperId = UUID.randomUUID();
        UUID otherShipperId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        DeliveryAssignment assignment = createMockAssignment(assignmentId, otherShipperId.toString(), AssignmentStatus.PENDING, "parcel-1");

        when(deliveryAssignmentRepository.findById(assignmentId)).thenReturn(java.util.Optional.of(assignment));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            deliveryAssignmentService.acceptTask(shipperId, assignmentId);
        });

        assertTrue(exception.getMessage().contains("does not belong to delivery man"));
        verify(deliveryAssignmentRepository, never()).save(any());
    }
}
