package com.ds.session.session_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.request.StartSessionRequest;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.infrastructure.kafka.ParcelEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService Story 9 Tests - Admin Session Creation & Shipper Workflow")
class SessionServiceStory9Test {

    @Mock
    private DeliverySessionRepository sessionRepository;

    @Mock
    private DeliveryAssignmentRepository assignmentRepository;

    @Mock
    private ParcelEventPublisher parcelEventPublisher;

    @InjectMocks
    private SessionService sessionService;

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
        reset(sessionRepository, assignmentRepository, parcelEventPublisher);
    }

    @Test
    @DisplayName("Should create session with PENDING assignments and set them to IN_PROGRESS")
    void shouldCreateSessionWithPendingAssignments() {
        // Given
        String shipperId = "shipper-1";
        UUID assignmentId1 = UUID.randomUUID();
        UUID assignmentId2 = UUID.randomUUID();
        String parcelId1 = "parcel-1";
        String parcelId2 = "parcel-2";

        CreateSessionRequest request = new CreateSessionRequest();
        request.setDeliveryManId(shipperId);
        request.setAssignmentsIds(Arrays.asList(assignmentId1.toString(), assignmentId2.toString()));

        DeliveryAssignment assignment1 = createMockAssignment(assignmentId1, shipperId, AssignmentStatus.PENDING, parcelId1);
        DeliveryAssignment assignment2 = createMockAssignment(assignmentId2, shipperId, AssignmentStatus.PENDING, parcelId2);

        when(assignmentRepository.findById(assignmentId1)).thenReturn(java.util.Optional.of(assignment1));
        when(assignmentRepository.findById(assignmentId2)).thenReturn(java.util.Optional.of(assignment2));
        when(sessionRepository.findByDeliveryManIdAndStatus(shipperId, SessionStatus.IN_PROGRESS))
            .thenReturn(java.util.Optional.empty());
        when(sessionRepository.findByDeliveryManIdAndStatus(shipperId, SessionStatus.CREATED))
            .thenReturn(java.util.Optional.empty());

        DeliverySession savedSession = DeliverySession.builder()
            .id(UUID.randomUUID())
            .deliveryManId(shipperId)
            .status(SessionStatus.IN_PROGRESS)
            .startTime(LocalDateTime.now())
            .assignments(Arrays.asList(assignment1, assignment2))
            .build();

        when(sessionRepository.save(any(DeliverySession.class))).thenReturn(savedSession);

        // When
        SessionResponse response = sessionService.createSession(request);

        // Then
        assertNotNull(response);
        assertEquals(SessionStatus.IN_PROGRESS, response.getStatus());
        assertEquals(2, response.getAssignments().size());

        // Verify assignments were saved (acceptTask and startTask are called internally)
        verify(assignmentRepository, times(2)).findById(any());
        verify(sessionRepository, times(1)).save(any(DeliverySession.class));

        // Verify parcels were updated to ON_ROUTE
        verify(parcelEventPublisher, times(1)).publish(parcelId1, com.ds.session.session_service.common.enums.ParcelEvent.SCAN_QR);
        verify(parcelEventPublisher, times(1)).publish(parcelId2, com.ds.session.session_service.common.enums.ParcelEvent.SCAN_QR);
        
        // Verify assignment statuses were updated
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment1.getStatus());
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment2.getStatus());
    }

    @Test
    @DisplayName("Should start session and update ACCEPTED assignments to IN_PROGRESS and parcels to ON_ROUTE")
    void shouldStartSessionAndUpdateAssignments() {
        // Given
        UUID sessionId = UUID.randomUUID();
        String shipperId = "shipper-1";
        UUID assignmentId1 = UUID.randomUUID();
        UUID assignmentId2 = UUID.randomUUID();
        String parcelId1 = "parcel-1";
        String parcelId2 = "parcel-2";

        DeliveryAssignment assignment1 = createMockAssignment(assignmentId1, shipperId, AssignmentStatus.ACCEPTED, parcelId1);
        DeliveryAssignment assignment2 = createMockAssignment(assignmentId2, shipperId, AssignmentStatus.ACCEPTED, parcelId2);

        DeliverySession session = DeliverySession.builder()
            .id(sessionId)
            .deliveryManId(shipperId)
            .status(SessionStatus.CREATED)
            .startTime(LocalDateTime.now())
            .assignments(Arrays.asList(assignment1, assignment2))
            .build();

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(session));

        DeliverySession savedSession = DeliverySession.builder()
            .id(sessionId)
            .deliveryManId(shipperId)
            .status(SessionStatus.IN_PROGRESS)
            .startTime(LocalDateTime.now())
            .assignments(Arrays.asList(assignment1, assignment2))
            .build();

        when(sessionRepository.save(any(DeliverySession.class))).thenReturn(savedSession);

        StartSessionRequest startRequest = new StartSessionRequest();
        startRequest.setStartLocationLat(10.8505);
        startRequest.setStartLocationLon(106.7718);
        startRequest.setStartLocationTimestamp(LocalDateTime.now());

        // When
        SessionResponse response = sessionService.startSession(sessionId, startRequest);

        // Then
        assertNotNull(response);
        assertEquals(SessionStatus.IN_PROGRESS, response.getStatus());

        // Verify session was saved
        verify(sessionRepository, times(1)).save(any(DeliverySession.class));

        // Verify parcels were updated to ON_ROUTE
        verify(parcelEventPublisher, times(1)).publish(parcelId1, com.ds.session.session_service.common.enums.ParcelEvent.SCAN_QR);
        verify(parcelEventPublisher, times(1)).publish(parcelId2, com.ds.session.session_service.common.enums.ParcelEvent.SCAN_QR);
        
        // Verify assignment statuses were updated
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment1.getStatus());
        assertEquals(AssignmentStatus.IN_PROGRESS, assignment2.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when creating session with assignment not in PENDING or ASSIGNED status")
    void shouldThrowExceptionWhenAssignmentNotPendingOrAssigned() {
        // Given
        String shipperId = "shipper-1";
        UUID assignmentId = UUID.randomUUID();

        CreateSessionRequest request = new CreateSessionRequest();
        request.setDeliveryManId(shipperId);
        request.setAssignmentsIds(Arrays.asList(assignmentId.toString()));

        DeliveryAssignment assignment = createMockAssignment(assignmentId, shipperId, AssignmentStatus.IN_PROGRESS, "parcel-1");

        when(assignmentRepository.findById(assignmentId)).thenReturn(java.util.Optional.of(assignment));
        when(sessionRepository.findByDeliveryManIdAndStatus(shipperId, SessionStatus.IN_PROGRESS))
            .thenReturn(java.util.Optional.empty());
        when(sessionRepository.findByDeliveryManIdAndStatus(shipperId, SessionStatus.CREATED))
            .thenReturn(java.util.Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            sessionService.createSession(request);
        });

        assertTrue(exception.getMessage().contains("not in PENDING or ASSIGNED status"));
        verify(sessionRepository, never()).save(any(DeliverySession.class));
    }
}
