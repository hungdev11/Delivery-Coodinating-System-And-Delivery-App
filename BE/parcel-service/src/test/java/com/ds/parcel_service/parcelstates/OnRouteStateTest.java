package com.ds.parcel_service.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.parcelstates.OnRouteState;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OnRouteStateTest {

    private final OnRouteState state = new OnRouteState();

    @Test
    void shouldTransitionToDelivered_OnSuccessfulDelivery() {
        assertEquals(ParcelStatus.DELIVERED, state.handleTransition(ParcelEvent.DELIVERY_SUCCESSFUL));
    }

    @Test
    void shouldTransitionToInWarehouse_OnPostpone() {
        assertEquals(ParcelStatus.IN_WAREHOUSE, state.handleTransition(ParcelEvent.POSTPONE));
    }

    @Test
    void shouldTransitionToFailed_OnAccident() {
        assertEquals(ParcelStatus.FAILED, state.handleTransition(ParcelEvent.ACCIDENT));
    }

    @Test
    void shouldTransitionToFailed_OnCannotDelivery() {
        assertEquals(ParcelStatus.FAILED, state.handleTransition(ParcelEvent.CAN_NOT_DELIVERY));
    }

    @Test
    void shouldThrowException_WhenInvalidEvent() {
        assertThrows(IllegalArgumentException.class, () -> {
            state.handleTransition(ParcelEvent.CONFIRM_TIMEOUT);
        });
    }
}
