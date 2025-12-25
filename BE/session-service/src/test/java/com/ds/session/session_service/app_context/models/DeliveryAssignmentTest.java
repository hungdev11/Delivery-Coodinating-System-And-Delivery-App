package com.ds.session.session_service.app_context.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ds.session.session_service.common.enums.AssignmentStatus;

/**
 * Tests for DeliveryAssignment entity
 * Tests new 1-n parcels relationship and status transitions
 */
@DisplayName("DeliveryAssignment Entity Tests")
class DeliveryAssignmentTest {

    private DeliveryAssignment assignment;
    private DeliverySession session;
    private String shipperId;
    private String deliveryAddressId;

    @BeforeEach
    void setUp() {
        shipperId = UUID.randomUUID().toString();
        deliveryAddressId = UUID.randomUUID().toString();
        
        assignment = DeliveryAssignment.builder()
                .shipperId(shipperId)
                .deliveryAddressId(deliveryAddressId)
                .status(AssignmentStatus.PENDING)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition from PENDING to ACCEPTED")
        void shouldTransitionFromPendingToAccepted() {
            // Arrange
            assignment.setStatus(AssignmentStatus.PENDING);

            // Act
            assignment.acceptTask();

            // Assert
            assertEquals(AssignmentStatus.ACCEPTED, assignment.getStatus());
            assertNotNull(assignment.getScanedAt());
        }

        @Test
        @DisplayName("Should transition from ASSIGNED to ACCEPTED (backward compatibility)")
        void shouldTransitionFromAssignedToAccepted() {
            // Arrange
            assignment.setStatus(AssignmentStatus.ASSIGNED);

            // Act
            assignment.acceptTask();

            // Assert
            assertEquals(AssignmentStatus.ACCEPTED, assignment.getStatus());
            assertNotNull(assignment.getScanedAt());
        }

        @Test
        @DisplayName("Should throw exception when accepting from invalid status")
        void shouldThrowExceptionWhenAcceptingFromInvalidStatus() {
            // Arrange
            assignment.setStatus(AssignmentStatus.IN_PROGRESS);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                assignment.acceptTask();
            });

            assertTrue(exception.getMessage().contains("Only assignments in PENDING or ASSIGNED status can be accepted"));
        }

        @Test
        @DisplayName("Should transition from ACCEPTED to IN_PROGRESS")
        void shouldTransitionFromAcceptedToInProgress() {
            // Arrange
            assignment.setStatus(AssignmentStatus.ACCEPTED);
            session = DeliverySession.builder()
                    .id(UUID.randomUUID())
                    .build();

            // Act
            assignment.startTask(session);

            // Assert
            assertEquals(AssignmentStatus.IN_PROGRESS, assignment.getStatus());
            assertEquals(session, assignment.getSession());
        }

        @Test
        @DisplayName("Should throw exception when starting from invalid status")
        void shouldThrowExceptionWhenStartingFromInvalidStatus() {
            // Arrange
            assignment.setStatus(AssignmentStatus.PENDING);
            session = DeliverySession.builder()
                    .id(UUID.randomUUID())
                    .build();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                assignment.startTask(session);
            });

            assertTrue(exception.getMessage().contains("Only assignments in ACCEPTED status can be started"));
        }
    }

    @Nested
    @DisplayName("Parcels Relationship Tests")
    class ParcelsRelationshipTests {

        @Test
        @DisplayName("Should add parcel to assignment")
        void shouldAddParcelToAssignment() {
            // Arrange
            String parcelId1 = UUID.randomUUID().toString();
            DeliveryAssignmentParcel parcel = DeliveryAssignmentParcel.builder()
                    .parcelId(parcelId1)
                    .build();

            // Act
            assignment.addParcel(parcel);

            // Assert
            assertNotNull(assignment.getParcels());
            assertEquals(1, assignment.getParcels().size());
            assertEquals(parcel, assignment.getParcels().get(0));
            assertEquals(assignment, parcel.getAssignment());
        }

        @Test
        @DisplayName("Should add multiple parcels to assignment")
        void shouldAddMultipleParcelsToAssignment() {
            // Arrange
            String parcelId1 = UUID.randomUUID().toString();
            String parcelId2 = UUID.randomUUID().toString();
            
            DeliveryAssignmentParcel parcel1 = DeliveryAssignmentParcel.builder()
                    .parcelId(parcelId1)
                    .build();
            DeliveryAssignmentParcel parcel2 = DeliveryAssignmentParcel.builder()
                    .parcelId(parcelId2)
                    .build();

            // Act
            assignment.addParcel(parcel1);
            assignment.addParcel(parcel2);

            // Assert
            assertEquals(2, assignment.getParcels().size());
            assertTrue(assignment.getParcels().contains(parcel1));
            assertTrue(assignment.getParcels().contains(parcel2));
        }

        @Test
        @DisplayName("Should initialize parcels list when null")
        void shouldInitializeParcelsListWhenNull() {
            // Arrange
            assignment.setParcels(null);
            DeliveryAssignmentParcel parcel = DeliveryAssignmentParcel.builder()
                    .parcelId(UUID.randomUUID().toString())
                    .build();

            // Act
            assignment.addParcel(parcel);

            // Assert
            assertNotNull(assignment.getParcels());
            assertEquals(1, assignment.getParcels().size());
        }
    }

    @Nested
    @DisplayName("Delivery Address Tests")
    class DeliveryAddressTests {

        @Test
        @DisplayName("Should store delivery address ID")
        void shouldStoreDeliveryAddressId() {
            // Arrange
            String addressId = UUID.randomUUID().toString();

            // Act
            assignment.setDeliveryAddressId(addressId);

            // Assert
            assertEquals(addressId, assignment.getDeliveryAddressId());
        }

        @Test
        @DisplayName("All parcels in assignment should share same delivery address")
        void allParcelsShouldShareSameDeliveryAddress() {
            // Arrange
            String addressId = UUID.randomUUID().toString();
            assignment.setDeliveryAddressId(addressId);

            DeliveryAssignmentParcel parcel1 = DeliveryAssignmentParcel.builder()
                    .parcelId(UUID.randomUUID().toString())
                    .build();
            DeliveryAssignmentParcel parcel2 = DeliveryAssignmentParcel.builder()
                    .parcelId(UUID.randomUUID().toString())
                    .build();

            // Act
            assignment.addParcel(parcel1);
            assignment.addParcel(parcel2);

            // Assert
            assertEquals(addressId, assignment.getDeliveryAddressId());
            // All parcels belong to same assignment which has single deliveryAddressId
            assertEquals(assignment, parcel1.getAssignment());
            assertEquals(assignment, parcel2.getAssignment());
        }
    }
}
