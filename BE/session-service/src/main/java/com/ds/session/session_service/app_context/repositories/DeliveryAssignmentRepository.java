package com.ds.session.session_service.app_context.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.SessionStatus;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID>, JpaSpecificationExecutor<DeliveryAssignment>{

    // --- Các phương thức này được yêu cầu bởi SessionService (mô hình Session-based) ---

    /**
     * Đếm số lượng task trong một phiên đang ở trạng thái PENDING.
     * (Dùng để kiểm tra xem có thể tự động hoàn thành phiên không)
     */
    long countBySession_IdAndStatus(UUID sessionId, AssignmentStatus status);

    /**
     * Tìm tất cả các task trong một phiên đang ở trạng thái cụ thể.
     * (Ví dụ: Tìm tất cả task PENDING để hủy khi phiên bị FAILED)
     */
    List<DeliveryAssignment> findBySession_IdAndStatus(UUID sessionId, AssignmentStatus status);

    /**
     * Tìm một assignment (task) dựa trên session_id và parcel_id.
     * (Dùng để kiểm tra xem đơn hàng đã được quét vào phiên này chưa)
     */
    Optional<DeliveryAssignment> findBySession_IdAndParcelId(UUID sessionId, String parcelId);
    
    /**
     * Kiểm tra xem một task (parcel) đã tồn tại trong phiên (session) chưa.
     * Tối ưu hơn 'findBySession_IdAndParcelId' nếu chỉ cần check true/false.
     */
    boolean existsBySession_IdAndParcelId(UUID sessionId, String parcelId);

    /**
     * Tìm một assignment đang hoạt động (phiên CREATED hoặc IN_PROGRESS)
     * dựa trên parcelId.
     * (Dùng để ngăn 1 đơn hàng bị 2 shipper khác nhau quét cùng lúc)
     */
    @Query("SELECT da FROM DeliveryAssignment da JOIN da.session s WHERE da.parcelId = :parcelId AND s.status = :status")
    Optional<DeliveryAssignment> findActiveByParcelId(String parcelId, SessionStatus status);

    /**
     * Tìm tất cả assignments trong một session.
     * (Dùng để lấy danh sách parcels trong session)
     */
    List<DeliveryAssignment> findBySession_Id(UUID sessionId);
    
    /**
     * Tìm assignment mới nhất của một parcel (bất kỳ trạng thái session nào).
     * (Dùng để kiểm tra lịch sử giao hàng)
     */
    Optional<DeliveryAssignment> findFirstByParcelIdOrderByUpdatedAtDesc(String parcelId);
}
