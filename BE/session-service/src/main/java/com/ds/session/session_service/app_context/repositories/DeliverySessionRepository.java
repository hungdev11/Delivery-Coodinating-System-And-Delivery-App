package com.ds.session.session_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.common.enums.SessionStatus;

@Repository
public interface DeliverySessionRepository extends JpaRepository<DeliverySession, UUID>, JpaSpecificationExecutor<DeliverySession> {

    /**
     * Tìm phiên (session) đang hoạt động (IN_PROGRESS) của một shipper.
     * Đây là hàm then chốt cho logic "Scan-to-Create-Session".
     */
    Optional<DeliverySession> findByDeliveryManIdAndStatus(String deliveryManId, SessionStatus status);

    /**
     * Tìm tất cả các phiên của shipper theo một trạng thái cụ thể.
     * (Ví dụ: Lấy tất cả các phiên đã COMPLETED)
     */
    List<DeliverySession> findAllByDeliveryManIdAndStatus(String deliveryManId, SessionStatus status);

    /**
     * ĐÂY CHÍNH LÀ FILTER THEO NGÀY MÀ BẠN HỎI:
     * Tìm tất cả các phiên của shipper, theo trạng thái, VÀ
     * có thời gian KẾT THÚC (endTime) nằm trong khoảng A và B.
     * (Dùng để lọc lịch sử, ví dụ: "Lấy các phiên đã hoàn thành trong tháng này")
     */
    List<DeliverySession> findAllByDeliveryManIdAndStatusAndEndTimeBetween(
            String deliveryManId,
            SessionStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime);
    
    /**
     * Find sessions by start time range and status
     * Used for auto-close scheduler to find sessions that need to be closed
     */
    List<DeliverySession> findByStartTimeBetweenAndStatusIn(
            LocalDateTime startTimeStart,
            LocalDateTime startTimeEnd,
            List<SessionStatus> statuses);
}
