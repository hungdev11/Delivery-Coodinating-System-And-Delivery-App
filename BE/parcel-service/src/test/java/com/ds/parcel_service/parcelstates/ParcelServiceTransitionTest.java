package com.ds.parcel_service.parcelstates;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.business.v1.services.ParcelService;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

/**
 * Kiểm tra toàn diện Máy trạng thái (State Machine) của ParcelService.
 * Sử dụng @Nested để nhóm các bài test theo trạng thái ban đầu.
 */
@ExtendWith(MockitoExtension.class)
class ParcelServiceTransitionTest {

    @Mock
    private ParcelRepository parcelRepository;
    
    // Giả lập các State Handler
    // Chúng ta không cần mock toàn bộ Map, vì service sẽ tự động
    // khởi tạo chúng. Chúng ta chỉ cần giả lập hành vi của findById và save.
    // Lưu ý: Các State Handler (ví dụ: InWarehouseState) PHẢI được
    // implement trong code của bạn để ném IllegalArgumentException
    // cho các Event không hợp lệ.

    @InjectMocks
    private ParcelService parcelService; 

    private UUID testId;
    private Parcel parcel;

    @BeforeEach
    void commonSetup() {
        testId = UUID.randomUUID();
        parcel = Parcel.builder()
                .id(testId)
                .code("TEST-001")
                // Trạng thái ban đầu sẽ được đặt trong từng @Nested class
                .build();
        
        // Mock chung cho tất cả các test
        // lenient() cho phép mock này được định nghĩa nhưng có thể không được gọi
        lenient().when(parcelRepository.findById(testId)).thenReturn(Optional.of(parcel));
        lenient().when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- CÁC PHƯƠNG THỨC HỖ TRỢ (HELPER METHODS) ---

    /**
     * Phương thức helper chính để kiểm tra một chuyển đổi trạng thái HỢP LỆ.
     */
    private void assertTransition(ParcelEvent event, ParcelStatus expectedEnd, boolean shouldCallSetDeliveredAt) {
        // Act
        ParcelResponse result = parcelService.changeParcelStatus(testId, event);

        // Assert
        assertEquals(expectedEnd, result.getStatus(), "Trạng thái cuối cùng không đúng.");
        verify(parcelRepository, times(1)).save(any(Parcel.class)); // Phải được gọi save
        
        if (shouldCallSetDeliveredAt) {
            assertNotNull(result.getDeliveredAt(), "deliveredAt đáng lẽ phải được set.");
        } else {
            // Giả sử deliveredAt là null ban đầu
            assertNull(result.getDeliveredAt(), "deliveredAt đáng lẽ không được set.");
        }
    }

    /**
     * Phương thức helper để kiểm tra một sự kiện HỢP LỆ nhưng KHÔNG THAY ĐỔI trạng thái.
     * (Ví dụ: Gửi lời nhắc)
     */
    private void assertRemainState(ParcelEvent event, ParcelStatus expectedState) {
        // Act
        ParcelResponse result = parcelService.changeParcelStatus(testId, event);

        // Assert
        assertEquals(expectedState, result.getStatus(), "Trạng thái đáng lẽ không đổi.");
        // KHÔNG được gọi save, vì service có logic return sớm
        verify(parcelRepository, never()).save(any(Parcel.class)); 
    }

    /**
     * Phương thức helper để kiểm tra một sự kiện BẤT HỢP LỆ cho trạng thái hiện tại.
     * Mong đợi IParcelState handler ném ra IllegalArgumentException.
     */
    private void assertInvalidEvent(ParcelEvent invalidEvent) {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            parcelService.changeParcelStatus(testId, invalidEvent);
        }, "Event " + invalidEvent + " đáng lẽ phải bất hợp lệ cho trạng thái " + parcel.getStatus());
        
        verify(parcelRepository, never()).save(any(Parcel.class)); // Không được save nếu có lỗi
    }

    // --- CÁC BÀI TEST ĐƯỢC NHÓM THEO TRẠNG THÁI ---

    @Nested
    @DisplayName("Khi trạng thái là IN_WAREHOUSE")
    class WhenInWarehouse {
        @BeforeEach
        void setup() {
            parcel.setStatus(ParcelStatus.IN_WAREHOUSE);
        }

        @Test
        @DisplayName("Nên chuyển sang ON_ROUTE khi SCAN_QR")
        void shouldTransitionToOnRoute_OnScanQr() {
            assertTransition(ParcelEvent.SCAN_QR, ParcelStatus.ON_ROUTE, false);
        }

        @Test
        @DisplayName("Nên ném lỗi khi DELIVERY_SUCCESSFUL (Event bất hợp lệ)")
        void shouldThrowException_OnDeliverySuccessful() {
            assertInvalidEvent(ParcelEvent.DELIVERY_SUCCESSFUL);
        }
    }

    @Nested
    @DisplayName("Khi trạng thái là ON_ROUTE")
    class WhenOnRoute {
        @BeforeEach
        void setup() {
            parcel.setStatus(ParcelStatus.ON_ROUTE);
        }

        @Test
        @DisplayName("Nên chuyển sang DELIVERED khi DELIVERY_SUCCESSFUL")
        void shouldTransitionToDelivered_OnDeliverySuccessful() {
            // Đây là lần duy nhất 'shouldCallSetDeliveredAt' là true
            assertTransition(ParcelEvent.DELIVERY_SUCCESSFUL, ParcelStatus.DELIVERED, true);
        }

        @Test
        @DisplayName("Nên chuyển sang FAILED khi CAN_NOT_DELIVERY")
        void shouldTransitionToFailed_OnCannotDelivery() {
            assertTransition(ParcelEvent.CAN_NOT_DELIVERY, ParcelStatus.FAILED, false);
        }

        @Test
        @DisplayName("Nên chuyển sang DELAYED khi POSTPONE")
        void shouldTransitionToDelayed_OnPostpone() {
            assertTransition(ParcelEvent.POSTPONE, ParcelStatus.DELAYED, false);
        }

        @Test
        @DisplayName("Nên ném lỗi khi SCAN_QR (Event bất hợp lệ)")
        void shouldThrowException_OnScanQr() {
            assertInvalidEvent(ParcelEvent.SCAN_QR);
        }
    }

    @Nested
    @DisplayName("Khi trạng thái là DELIVERED")
    class WhenDelivered {
        @BeforeEach
        void setup() {
            parcel.setStatus(ParcelStatus.DELIVERED);
            // Giả lập là nó đã được set deliveredAt
            parcel.setDeliveredAt(LocalDateTime.now().minusMinutes(1));
        }

        @Test
        @DisplayName("Nên chuyển sang SUCCEEDED khi CUSTOMER_RECEIVED")
        void shouldTransitionToSucceeded_OnCustomerReceived() {
            assertTransition(ParcelEvent.CUSTOMER_RECEIVED, ParcelStatus.SUCCEEDED, true);
        }

        @Test
        @DisplayName("Nên chuyển sang SUCCEEDED khi CONFIRM_TIMEOUT")
        void shouldTransitionToSucceeded_OnConfirmTimeout() {
            assertTransition(ParcelEvent.CONFIRM_TIMEOUT, ParcelStatus.SUCCEEDED, true);
        }

        @Test
        @DisplayName("Nên chuyển sang FAILED khi CUSTOMER_REJECT")
        void shouldTransitionToFailed_OnCustomerReject() {
            assertTransition(ParcelEvent.CUSTOMER_REJECT, ParcelStatus.FAILED, true);
        }

        @Test
        @DisplayName("Nên chuyển sang DISPUTE khi CUSTOMER_CONFIRM_NOT_RECEIVED")
        void shouldTransitionToDispute_OnCustomerConfirmNotReceived() {
            assertTransition(ParcelEvent.CUSTOMER_CONFIRM_NOT_RECEIVED, ParcelStatus.DISPUTE, true);
        }

        @Test
        @DisplayName("Nên giữ nguyên trạng thái DELIVERED khi CONFIRM_REMINDER")
        void shouldRemainInDelivered_OnConfirmReminder() {
            assertRemainState(ParcelEvent.CONFIRM_REMINDER, ParcelStatus.DELIVERED);
        }
    }

    @Nested
    @DisplayName("Khi trạng thái là DISPUTE")
    class WhenInDispute {
        @BeforeEach
        void setup() {
            parcel.setStatus(ParcelStatus.DISPUTE);
        }

        @Test
        @DisplayName("Nên chuyển sang SUCCEEDED khi MISSUNDERSTANDING_DISPUTE")
        void shouldTransitionToSucceeded_OnMisunderstanding() {
            assertTransition(ParcelEvent.MISSUNDERSTANDING_DISPUTE, ParcelStatus.SUCCEEDED, false);
        }

        @Test
        @DisplayName("Nên chuyển sang LOST khi FAULT_DISPUTE")
        void shouldTransitionToLost_OnFault() {
            assertTransition(ParcelEvent.FAULT_DISPUTE, ParcelStatus.LOST, false);
        }
    }

    @Nested
    @DisplayName("Khi trạng thái là DELAYED")
    class WhenDelayed {
        @BeforeEach
        void setup() {
            parcel.setStatus(ParcelStatus.DELAYED);
        }

        @Test
        @DisplayName("Nên chuyển sang IN_WAREHOUSE khi END_SESSION")
        void shouldTransitionToInWarehouse_OnEndSession() {
            // Giả sử sự kiện END_SESSION sẽ kích hoạt việc đưa hàng về kho
            assertTransition(ParcelEvent.END_SESSION, ParcelStatus.IN_WAREHOUSE, false);
        }
    }

    @Nested
    @DisplayName("Khi ở các trạng thái CUỐI (Terminal States)")
    class WhenInTerminalState {

        @ParameterizedTest
        @EnumSource(value = ParcelStatus.class, names = {"FAILED", "SUCCEEDED", "LOST"})
        @DisplayName("Nên ném lỗi khi nhận bất kỳ Event nào")
        void shouldThrowException_OnAnyEvent(ParcelStatus terminalState) {
            // Arrange
            parcel.setStatus(terminalState);
            
            // Act & Assert
            // Bất kỳ sự kiện nào (ví dụ: SCAN_QR) cũng phải ném ra lỗi
            // Lỗi này đến từ State Handler (ví dụ: FailedState)
            assertInvalidEvent(ParcelEvent.SCAN_QR);
            assertInvalidEvent(ParcelEvent.DELIVERY_SUCCESSFUL);
        }
    }
}