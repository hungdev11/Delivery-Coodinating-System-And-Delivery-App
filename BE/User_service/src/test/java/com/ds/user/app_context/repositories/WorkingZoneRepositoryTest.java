package com.ds.user.app_context.repositories;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.user.common.entities.base.DeliveryMan;
import com.ds.user.common.entities.base.WorkingZone;

/**
 * Repository tests for WorkingZone
 * Tests complex queries and relationships
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkingZoneRepository Tests")
class WorkingZoneRepositoryTest {

    @Mock
    private WorkingZoneRepository workingZoneRepository;

    private UUID deliveryManId;
    private String zoneId1;
    private String zoneId2;
    private WorkingZone zone1;
    private WorkingZone zone2;

    @BeforeEach
    void setUp() {
        deliveryManId = UUID.randomUUID();
        zoneId1 = UUID.randomUUID().toString();
        zoneId2 = UUID.randomUUID().toString();

        DeliveryMan deliveryMan = DeliveryMan.builder()
                .id(deliveryManId)
                .vehicleType("BIKE")
                .capacityKg(50.0)
                .build();

        zone1 = WorkingZone.builder()
                .deliveryMan(deliveryMan)
                .zoneId(zoneId1)
                .order(1)
                .build();

        zone2 = WorkingZone.builder()
                .deliveryMan(deliveryMan)
                .zoneId(zoneId2)
                .order(2)
                .build();
    }

    @Test
    @DisplayName("Should find all working zones for delivery man ordered by priority")
    void shouldFindZonesOrderedByPriority() {
        // Arrange
        List<WorkingZone> expectedZones = Arrays.asList(zone1, zone2);
        when(workingZoneRepository.findByDeliveryManIdOrderByOrderAsc(deliveryManId))
                .thenReturn(expectedZones);

        // Act
        List<WorkingZone> result = workingZoneRepository.findByDeliveryManIdOrderByOrderAsc(deliveryManId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getOrder());
        assertEquals(2, result.get(1).getOrder());
        assertEquals(zone1, result.get(0));
        assertEquals(zone2, result.get(1));

        verify(workingZoneRepository, times(1)).findByDeliveryManIdOrderByOrderAsc(deliveryManId);
    }

    @Test
    @DisplayName("Should find working zone by delivery man and zone ID")
    void shouldFindZoneByDeliveryManAndZoneId() {
        // Arrange
        when(workingZoneRepository.findByDeliveryManIdAndZoneId(deliveryManId, zoneId1))
                .thenReturn(Optional.of(zone1));

        // Act
        Optional<WorkingZone> result = workingZoneRepository.findByDeliveryManIdAndZoneId(deliveryManId, zoneId1);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(zone1, result.get());
        assertEquals(zoneId1, result.get().getZoneId());

        verify(workingZoneRepository, times(1)).findByDeliveryManIdAndZoneId(deliveryManId, zoneId1);
    }

    @Test
    @DisplayName("Should count working zones for delivery man")
    void shouldCountZonesForDeliveryMan() {
        // Arrange
        when(workingZoneRepository.countByDeliveryManId(deliveryManId)).thenReturn(2L);

        // Act
        long count = workingZoneRepository.countByDeliveryManId(deliveryManId);

        // Assert
        assertEquals(2, count);

        verify(workingZoneRepository, times(1)).countByDeliveryManId(deliveryManId);
    }

    @Test
    @DisplayName("Should check if delivery man has working zones")
    void shouldCheckIfDeliveryManHasZones() {
        // Arrange
        when(workingZoneRepository.existsByDeliveryManId(deliveryManId)).thenReturn(true);

        // Act
        boolean exists = workingZoneRepository.existsByDeliveryManId(deliveryManId);

        // Assert
        assertTrue(exists);

        verify(workingZoneRepository, times(1)).existsByDeliveryManId(deliveryManId);
    }
}
