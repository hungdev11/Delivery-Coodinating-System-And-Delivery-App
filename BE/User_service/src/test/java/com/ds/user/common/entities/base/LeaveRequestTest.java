package com.ds.user.common.entities.base;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for LeaveRequest entity
 */
@DisplayName("LeaveRequest Entity Tests")
class LeaveRequestTest {

    private DeliveryMan deliveryMan;
    private WorkingShift shift;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp() {
        deliveryMan = DeliveryMan.builder()
                .id(UUID.randomUUID())
                .vehicleType("BIKE")
                .capacityKg(50.0)
                .build();

        shift = WorkingShift.builder()
                .id(UUID.randomUUID())
                .deliveryMan(deliveryMan)
                .dayOfWeek(1)
                .startTime(java.time.LocalTime.of(8, 0))
                .endTime(java.time.LocalTime.of(18, 0))
                .build();

        leaveRequest = LeaveRequest.builder()
                .deliveryMan(deliveryMan)
                .shift(shift)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(4))
                .reason("Personal leave")
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("Should create leave request with all fields")
    void shouldCreateLeaveRequestWithAllFields() {
        // Assert
        assertEquals(deliveryMan, leaveRequest.getDeliveryMan());
        assertEquals(shift, leaveRequest.getShift());
        assertNotNull(leaveRequest.getStartTime());
        assertNotNull(leaveRequest.getEndTime());
        assertEquals("Personal leave", leaveRequest.getReason());
        assertEquals("PENDING", leaveRequest.getStatus());
    }

    @Test
    @DisplayName("Should allow leave request without specific shift")
    void shouldAllowLeaveRequestWithoutShift() {
        // Arrange
        LeaveRequest leaveWithoutShift = LeaveRequest.builder()
                .deliveryMan(deliveryMan)
                .shift(null) // Applies to all shifts during time period
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(8))
                .reason("Full day leave")
                .status("PENDING")
                .build();

        // Assert
        assertNull(leaveWithoutShift.getShift());
        assertNotNull(leaveWithoutShift.getDeliveryMan());
    }

    @Test
    @DisplayName("Should have default status as PENDING")
    void shouldHaveDefaultStatusAsPending() {
        // Arrange
        LeaveRequest leave = LeaveRequest.builder()
                .deliveryMan(deliveryMan)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(4))
                .build();

        // Assert
        assertEquals("PENDING", leave.getStatus());
    }

    @Test
    @DisplayName("Should update timestamp on pre-update")
    void shouldUpdateTimestampOnPreUpdate() throws InterruptedException {
        // Arrange
        LocalDateTime originalUpdatedAt = leaveRequest.getUpdatedAt();

        // Act
        Thread.sleep(10); // Small delay
        leaveRequest.setReason("Updated reason");
        leaveRequest.preUpdateTimestamps();

        // Assert
        assertNotNull(leaveRequest.getUpdatedAt());
        if (originalUpdatedAt != null) {
            assertTrue(leaveRequest.getUpdatedAt().isAfter(originalUpdatedAt) || 
                      leaveRequest.getUpdatedAt().isEqual(originalUpdatedAt));
        }
    }
}
