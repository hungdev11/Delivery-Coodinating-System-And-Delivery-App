package com.ds.parcel_service.parcelstates;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.business.v1.services.ParcelService;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParcelServiceTransitionTest {

    @Mock
    private ParcelRepository parcelRepository;

    @InjectMocks
    private ParcelService parcelService; 

    private UUID testId;
    private Parcel parcel;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        // Giả lập một Parcel cơ bản
        parcel = Parcel.builder()
                .id(testId)
                .status(ParcelStatus.IN_WAREHOUSE) // Trạng thái ban đầu
                .build();
    }
    
    // Phương thức kiểm tra cơ chế Transition Logic và DB Save
    private void assertTransition(ParcelStatus start, ParcelEvent event, ParcelStatus expectedEnd, boolean shouldCallSetDeliveredAt) {
        // Arrange
        parcel.setStatus(start);
        when(parcelRepository.findById(testId)).thenReturn(Optional.of(parcel));
        when(parcelRepository.save(any(Parcel.class))).thenReturn(parcel);

        // Act
        ParcelResponse result = parcelService.changeParcelStatus(testId, event);

        // Assert
        assertEquals(expectedEnd, result.getStatus());
        verify(parcelRepository, times(1)).save(any(Parcel.class));
        
        // Kiểm tra logic cập nhật deliveredAt
        if (shouldCallSetDeliveredAt) {
            assertNotNull(result.getDeliveredAt());
        } else {
            // Đảm bảo không set deliveredAt nếu không chuyển sang DELIVERED
            assertNull(result.getDeliveredAt()); 
        }
    }

    //---------------------------------------------------------
    // KỊCH BẢN 1: IN_WAREHOUSE
    //---------------------------------------------------------

    @Test
    void shouldTransition_InWarehouseToOnRoute_OnScanQr() {
        assertTransition(ParcelStatus.IN_WAREHOUSE, ParcelEvent.SCAN_QR, ParcelStatus.ON_ROUTE, false);
    }
    
    //---------------------------------------------------------
    // KỊCH BẢN 2: ON_ROUTE (Kiểm tra đa trạng thái)
    //---------------------------------------------------------

    @Test
    void shouldTransition_OnRouteToDelivered_OnSuccessfulDelivery() {
        assertTransition(ParcelStatus.ON_ROUTE, ParcelEvent.DELIVERY_SUCCESSFUL, ParcelStatus.DELIVERED, true);
    }

    @Test
    void shouldTransition_OnRouteToInWarehouse_OnPostpone() {
        assertTransition(ParcelStatus.ON_ROUTE, ParcelEvent.POSTPONE, ParcelStatus.IN_WAREHOUSE, false);
    }

    @Test
    void shouldTransition_OnRouteToFailed_OnAccident() {
        assertTransition(ParcelStatus.ON_ROUTE, ParcelEvent.ACCIDENT, ParcelStatus.FAILED, false);
    }

    //---------------------------------------------------------
    // KỊCH BẢN 3: DELIVERED (Kiểm tra đa trạng thái + Lặp)
    //---------------------------------------------------------
    
    @Test
    void shouldTransition_DeliveredToSucceeded_OnTimeout() {
        assertTransition(ParcelStatus.DELIVERED, ParcelEvent.CONFIRM_TIMEOUT, ParcelStatus.SUCCESSED, false);
    }
    
    @Test
    void shouldTransition_DeliveredToFailed_OnCustomerReject() {
        assertTransition(ParcelStatus.DELIVERED, ParcelEvent.CUSTOMER_REJECT, ParcelStatus.FAILED, false);
    }

    @Test
    void shouldRemainState_DeliveredToDelivered_OnReminder() {
        // Kiểm tra trạng thái lặp (State Handler trả về chính nó)
        parcel.setStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findById(testId)).thenReturn(Optional.of(parcel));
        
        // Act
        ParcelResponse result = parcelService.changeParcelStatus(testId, ParcelEvent.CONFIRM_REMINDER);

        // Assert
        assertEquals(ParcelStatus.DELIVERED, result.getStatus());
        // Kiểm tra hàm save KHÔNG được gọi (vì trạng thái không đổi)
        verify(parcelRepository, never()).save(any(Parcel.class)); 
    }
    
    //---------------------------------------------------------
    // KỊCH BẢN 4: KIỂM TRA TÍNH BẤT HỢP LỆ
    //---------------------------------------------------------

    @Test
    void shouldThrowIllegalStateException_WhenTransitionPathIsInvalidByServiceMachine() {
        // Arrange: Đang ở DELIVERED, State Handler trả về SUCCESSED (hợp lệ)
        parcel.setStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findById(testId)).thenReturn(Optional.of(parcel));
        
        // BUT: Giả lập State Machine trong Service bị lỗi, không cho phép DELIVERED -> SUCCESSED
        // (Trong code thật: DELIVERED -> SUCCESSED là hợp lệ, ta dùng kịch bản khác)
        
        // Kịch bản vi phạm thực tế: SUCCESSED -> FAILED (Trạng thái cuối không được chuyển)
        parcel.setStatus(ParcelStatus.SUCCESSED);
        when(parcelRepository.findById(testId)).thenReturn(Optional.of(parcel));

        // State Handler (SuccessedState) trả về SUCCESSED (trạng thái lặp)
        // Service's isTransitionValid(SUCCESSED, SUCCESSED) == FALSE
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            parcelService.changeParcelStatus(testId, ParcelEvent.CONFIRM_REMINDER); 
        }, "Phải ném ra IllegalStateException vì SUCCESSED là trạng thái cuối.");

        verify(parcelRepository, never()).save(any(Parcel.class));
    }
    
    @Test
    void shouldThrowIllegalArgumentException_WhenEventIsInvalidForState() {
        // Arrange: Đang ở IN_WAREHOUSE
        parcel.setStatus(ParcelStatus.IN_WAREHOUSE);
        when(parcelRepository.findById(testId)).thenReturn(Optional.of(parcel));

        // Act & Assert: DELIVERY_SUCCESSFUL không hợp lệ tại IN_WAREHOUSE
        assertThrows(IllegalArgumentException.class, () -> {
            parcelService.changeParcelStatus(testId, ParcelEvent.DELIVERY_SUCCESSFUL);
        });
        
        verify(parcelRepository, never()).save(any(Parcel.class));
    }
}
