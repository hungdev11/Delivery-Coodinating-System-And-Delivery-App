package com.ds.user.common.entities.base;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for WorkingShift entity
 */
@DisplayName("WorkingShift Entity Tests")
class WorkingShiftTest {

    private DeliveryMan deliveryMan;
    private WorkingShift workingShift;

    @BeforeEach
    void setUp() {
        deliveryMan = DeliveryMan.builder()
                .id(UUID.randomUUID())
                .vehicleType("BIKE")
                .capacityKg(50.0)
                .build();

        workingShift = WorkingShift.builder()
                .deliveryMan(deliveryMan)
                .dayOfWeek(1) // Monday
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .maxSessionTimeHours(4.0)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should create working shift with default values")
    void shouldCreateWorkingShiftWithDefaults() {
        // Arrange
        WorkingShift shift = WorkingShift.builder()
                .deliveryMan(deliveryMan)
                .dayOfWeek(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        // Assert
        assertEquals(4.0, shift.getMaxSessionTimeHours()); // Default
        assertTrue(shift.getIsActive()); // Default
    }

    @Test
    @DisplayName("Should convert day of week integer to enum")
    void shouldConvertDayOfWeekToEnum() {
        // Arrange
        workingShift.setDayOfWeek(1); // Monday

        // Act
        DayOfWeek dayOfWeek = workingShift.getDayOfWeekEnum();

        // Assert
        assertEquals(DayOfWeek.MONDAY, dayOfWeek);
    }

    @Test
    @DisplayName("Should handle different day of week values")
    void shouldHandleDifferentDayOfWeekValues() {
        // Test all days
        for (int i = 1; i <= 7; i++) {
            workingShift.setDayOfWeek(i);
            DayOfWeek expectedDay = DayOfWeek.of(i);
            assertEquals(expectedDay, workingShift.getDayOfWeekEnum(), 
                "Day " + i + " should map to " + expectedDay);
        }
    }

    @Test
    @DisplayName("Should allow different max session times for morning and afternoon")
    void shouldAllowDifferentMaxSessionTimes() {
        // Arrange - Morning shift
        WorkingShift morningShift = WorkingShift.builder()
                .deliveryMan(deliveryMan)
                .dayOfWeek(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .maxSessionTimeHours(3.5) // Morning: 3.5h
                .build();

        // Arrange - Afternoon shift
        WorkingShift afternoonShift = WorkingShift.builder()
                .deliveryMan(deliveryMan)
                .dayOfWeek(1)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(18, 0))
                .maxSessionTimeHours(4.5) // Afternoon: 4.5h
                .build();

        // Assert
        assertEquals(3.5, morningShift.getMaxSessionTimeHours());
        assertEquals(4.5, afternoonShift.getMaxSessionTimeHours());
    }
}
