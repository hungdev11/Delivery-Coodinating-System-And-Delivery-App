package com.ds.parcel_service.common.utils;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.business.v1.services.ParcelService;
import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {
    private final ParcelRepository parcelRepository;
    private final ParcelService parcelService;

    private static final int TIMEOUT_HOURS = 48;

    /**
     * Job 1: Kiểm tra hết thời gian (Timeout) sau 24 giờ.
     * Chạy mỗi giờ để bắt các đơn hàng đã quá hạn.
     */
    @Scheduled(cron = "0 0 * * * *") // Chạy vào phút 0, giờ 0 của mỗi giờ
    public void checkConfirmationTimeout() {
        log.info("Starting timeout confirmation check...");
        
        LocalDateTime deadline = LocalDateTime.now().minusHours(TIMEOUT_HOURS);
        
        // 1. Tìm tất cả đơn hàng đang ở DELIVERED và đã quá 24h từ lúc deliveredAt
        List<Parcel> timedOutParcels = parcelRepository.findByStatusAndDeliveredAtBefore(
            ParcelStatus.DELIVERED, deadline
        );

        for (Parcel parcel : timedOutParcels) {
            try {
                // 2. Kích hoạt chuyển trạng thái sang SUCCESSED bằng sự kiện TIMEOUT_CONFIRM
                log.info("Parcel {} timed out. Transitioning to SUCCESSED.", parcel.getId());
                parcelService.changeParcelStatus(parcel.getId(), ParcelEvent.CONFIRM_TIMEOUT);
            } catch (Exception e) {
                log.error("Failed to process timeout for parcel {}: {}", parcel.getId(), e.getMessage());
                // Xử lý lỗi: ghi log, hoặc thử lại
            }
        }
    }

    /**
     * Job 2: Gửi nhắc nhở xác nhận mỗi giờ (cho 24 giờ đầu).
     * Chạy mỗi giờ để gửi thông báo.
     */
    @Scheduled(cron = "0 5 * * * *") // Chạy vào phút thứ 5 của mỗi giờ (tránh xung đột với Job 1)
    public void sendHourlyReminder() {
        log.info("Starting hourly confirmation reminder job...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(TIMEOUT_HOURS);

        // 1. Tìm tất cả đơn hàng đang ở DELIVERED và CHƯA quá 24h
        List<Parcel> parcelsToRemind = parcelRepository.findByStatusAndDeliveredAtBetween(
            ParcelStatus.DELIVERED, twentyFourHoursAgo, now
        );

        for (Parcel parcel : parcelsToRemind) {
            // 2. Chỉ nhắc nhở nếu đã qua ít nhất 1 giờ (tránh nhắc ngay lập tức)
            if (parcel.getDeliveredAt().isBefore(now.minusHours(1))) {
                // notificationService.sendReminder(parcel.getId(), "Vui lòng xác nhận đơn hàng.");
                
                // Kích hoạt Event để ghi log hoặc State Object tự chuyển về DELIVERED (lặp)
                try {
                     parcelService.changeParcelStatus(parcel.getId(), ParcelEvent.CONFIRM_REMINDER);
                     log.debug("Sent reminder for parcel {}", parcel.getId());
                } catch (Exception e) {
                    log.error("Failed to send reminder event for parcel {}: {}", parcel.getId(), e.getMessage());
                }
            }
        }
    }
}
