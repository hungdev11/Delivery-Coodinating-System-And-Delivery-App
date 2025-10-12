package com.ds.parcel_service.parcelstates;

import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.parcelstates.DeliveredState;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeliveredStateTest {

    private final DeliveredState state = new DeliveredState();

    @Test
    void shouldTransitionToSucceeded_WhenTimeout() {
        ParcelStatus nextStatus = state.handleTransition(ParcelEvent.CONFIRM_TIMEOUT);
        assertEquals(ParcelStatus.SUCCESSED, nextStatus);
    }

    @Test
    void shouldTransitionToFailed_WhenRejected() {
        ParcelStatus nextStatus = state.handleTransition(ParcelEvent.CUSTOMER_REJECT);
        assertEquals(ParcelStatus.FAILED, nextStatus);
    }

    @Test
    void shouldRemainDelivered_WhenReminder() {
        // Trạng thái lặp (loop state)
        ParcelStatus nextStatus = state.handleTransition(ParcelEvent.CONFIRM_REMINDER);
        assertEquals(ParcelStatus.DELIVERED, nextStatus);
    }

    @Test
    void shouldThrowException_WhenInvalidEvent() {
        // Sự kiện không hợp lệ tại Delivered
        assertThrows(IllegalArgumentException.class, () -> {
            state.handleTransition(ParcelEvent.DELIVERY_SUCCESSFUL);
        });
    }
}
