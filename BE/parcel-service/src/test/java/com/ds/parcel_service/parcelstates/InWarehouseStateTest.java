package com.ds.parcel_service.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.parcelstates.InWarehouseState;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InWarehouseStateTest {

    private final InWarehouseState state = new InWarehouseState();

    @Test
    void shouldTransitionToOnRoute_WhenScanQrEvent() {
        // Arrange
        ParcelEvent event = ParcelEvent.SCAN_QR;
        
        // Act
        ParcelStatus nextStatus = state.handleTransition(event);
        
        // Assert
        assertEquals(ParcelStatus.ON_ROUTE, nextStatus);
    }

    @Test
    void shouldThrowException_WhenInvalidEvent() {
        // Arrange
        ParcelEvent invalidEvent = ParcelEvent.ACCIDENT; 
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            state.handleTransition(invalidEvent);
        });
    }
}
