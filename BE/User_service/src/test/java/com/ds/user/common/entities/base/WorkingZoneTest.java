package com.ds.user.common.entities.base;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for WorkingZone entity
 */
@DisplayName("WorkingZone Entity Tests")
class WorkingZoneTest {

    private DeliveryMan deliveryMan;
    private WorkingZone workingZone;
    private String zoneId;

    @BeforeEach
    void setUp() {
        deliveryMan = DeliveryMan.builder()
                .id(UUID.randomUUID())
                .vehicleType("BIKE")
                .capacityKg(50.0)
                .build();

        zoneId = UUID.randomUUID().toString();

        workingZone = WorkingZone.builder()
                .deliveryMan(deliveryMan)
                .zoneId(zoneId)
                .order(1)
                .build();
    }

    @Test
    @DisplayName("Should create working zone with all fields")
    void shouldCreateWorkingZoneWithAllFields() {
        // Assert
        assertNotNull(workingZone.getDeliveryMan());
        assertEquals(deliveryMan, workingZone.getDeliveryMan());
        assertEquals(zoneId, workingZone.getZoneId());
        assertEquals(1, workingZone.getOrder());
    }

    @Test
    @DisplayName("Should allow multiple zones for same delivery man with different orders")
    void shouldAllowMultipleZonesWithDifferentOrders() {
        // Arrange
        WorkingZone zone1 = WorkingZone.builder()
                .deliveryMan(deliveryMan)
                .zoneId(zoneId)
                .order(1)
                .build();

        WorkingZone zone2 = WorkingZone.builder()
                .deliveryMan(deliveryMan)
                .zoneId(UUID.randomUUID().toString())
                .order(2)
                .build();

        // Assert
        assertEquals(1, zone1.getOrder());
        assertEquals(2, zone2.getOrder());
        assertEquals(deliveryMan, zone1.getDeliveryMan());
        assertEquals(deliveryMan, zone2.getDeliveryMan());
    }

    @Test
    @DisplayName("Should update timestamp on pre-update")
    void shouldUpdateTimestampOnPreUpdate() throws InterruptedException {
        // Arrange
        workingZone.setOrder(2);

        // Act
        Thread.sleep(10); // Small delay to ensure timestamp difference
        workingZone.preUpdateTimestamps();

        // Assert
        assertNotNull(workingZone.getUpdatedAt());
    }
}
