package com.ds.session.session_service.app_context.repositories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliveryAssignmentParcel;

/**
 * Repository tests for DeliveryAssignmentParcel
 * Tests 1-n relationship queries
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryAssignmentParcelRepository Tests")
class DeliveryAssignmentParcelRepositoryTest {

    @Mock
    private DeliveryAssignmentParcelRepository repository;

    private UUID assignmentId;
    private String parcelId1;
    private String parcelId2;
    private DeliveryAssignmentParcel parcel1;
    private DeliveryAssignmentParcel parcel2;

    @BeforeEach
    void setUp() {
        assignmentId = UUID.randomUUID();
        parcelId1 = UUID.randomUUID().toString();
        parcelId2 = UUID.randomUUID().toString();

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .id(assignmentId)
                .shipperId(UUID.randomUUID().toString())
                .deliveryAddressId(UUID.randomUUID().toString())
                .build();

        parcel1 = DeliveryAssignmentParcel.builder()
                .assignment(assignment)
                .parcelId(parcelId1)
                .build();

        parcel2 = DeliveryAssignmentParcel.builder()
                .assignment(assignment)
                .parcelId(parcelId2)
                .build();
    }

    @Test
    @DisplayName("Should find all parcels for an assignment")
    void shouldFindParcelsByAssignmentId() {
        // Arrange
        List<DeliveryAssignmentParcel> expectedParcels = Arrays.asList(parcel1, parcel2);
        when(repository.findByAssignmentId(assignmentId)).thenReturn(expectedParcels);

        // Act
        List<DeliveryAssignmentParcel> result = repository.findByAssignmentId(assignmentId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(parcel1));
        assertTrue(result.contains(parcel2));

        verify(repository, times(1)).findByAssignmentId(assignmentId);
    }

    @Test
    @DisplayName("Should find all assignments containing a specific parcel")
    void shouldFindAssignmentsByParcelId() {
        // Arrange
        List<DeliveryAssignmentParcel> expectedAssignments = Arrays.asList(parcel1);
        when(repository.findByParcelId(parcelId1)).thenReturn(expectedAssignments);

        // Act
        List<DeliveryAssignmentParcel> result = repository.findByParcelId(parcelId1);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(parcelId1, result.get(0).getParcelId());

        verify(repository, times(1)).findByParcelId(parcelId1);
    }

    @Test
    @DisplayName("Should check if parcel exists in any assignment")
    void shouldCheckIfParcelExists() {
        // Arrange
        when(repository.existsByParcelId(parcelId1)).thenReturn(true);

        // Act
        boolean exists = repository.existsByParcelId(parcelId1);

        // Assert
        assertTrue(exists);

        verify(repository, times(1)).existsByParcelId(parcelId1);
    }

    @Test
    @DisplayName("Should delete all parcels for an assignment")
    void shouldDeleteParcelsByAssignmentId() {
        // Arrange
        doNothing().when(repository).deleteByAssignmentId(assignmentId);

        // Act
        repository.deleteByAssignmentId(assignmentId);

        // Assert
        verify(repository, times(1)).deleteByAssignmentId(assignmentId);
    }
}
