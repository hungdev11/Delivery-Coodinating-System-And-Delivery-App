package com.ds.session.session_service.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.common.enums.AssignmentStatus; 
import com.ds.session.session_service.common.enums.SessionStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate; 
import lombok.extern.slf4j.Slf4j; 

@Slf4j
public class AssignmentSpecification {

    /**
     * Lọc các task (Assignment) dựa trên deliveryManId (shipper).
     * Yêu cầu JOIN sang bảng DeliverySession.
     */
    public static Specification<DeliveryAssignment> byDeliveryManId(UUID deliveryManId) {
        return (root, query, criteriaBuilder) -> {
            // Dùng LEFT JOIN để đảm bảo query không bị lỗi nếu join rỗng
            Join<DeliveryAssignment, DeliverySession> sessionJoin = root.join("session", JoinType.LEFT);
            return criteriaBuilder.equal(sessionJoin.get("deliveryManId"), deliveryManId.toString());
        };
    }

    /**
     * Lọc các task dựa trên TRẠNG THÁI CỦA PHIÊN (SESSION).
     * Dùng cho 'getDailyTasks' (IN_PROGRESS) hoặc 'getTasksHistory' (COMPLETED, FAILED).
     */
    public static Specification<DeliveryAssignment> bySessionStatus(SessionStatus sessionStatus) {
        return (root, query, criteriaBuilder) -> {
            Join<DeliveryAssignment, DeliverySession> sessionJoin = root.join("session", JoinType.LEFT);
            return criteriaBuilder.equal(sessionJoin.get("status"), sessionStatus);
        };
    }

     /**
     * Lọc các task dựa trên TRẠNG THÁI CỦA PHIÊN (SESSION) - dạng danh sách.
     */
    public static Specification<DeliveryAssignment> bySessionStatusIn(List<SessionStatus> sessionStatuses) {
        return (root, query, criteriaBuilder) -> {
            if (sessionStatuses == null || sessionStatuses.isEmpty()) {
                 return criteriaBuilder.conjunction();
            }
            Join<DeliveryAssignment, DeliverySession> sessionJoin = root.join("session", JoinType.LEFT);
            return sessionJoin.get("status").in(sessionStatuses);
        };
    }

    /**
     * Lọc các task dựa trên sessionId cụ thể.
     */
    public static Specification<DeliveryAssignment> bySessionId(UUID sessionId) {
        return (root, query, criteriaBuilder) -> {
            Join<DeliveryAssignment, DeliverySession> sessionJoin = root.join("session", JoinType.INNER);
            return criteriaBuilder.equal(sessionJoin.get("id"), sessionId);
        };
    }

    /**
     * Lọc các task (Assignment) dựa trên danh sách trạng thái (của chính task đó).
     */
    public static Specification<DeliveryAssignment> hasAssignmentStatusIn(List<String> status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isEmpty()) {
                return criteriaBuilder.conjunction(); // Không lọc gì cả
            }
            
            // Chuyển đổi List<String> sang List<AssignmentStatus> (Enum)
            try {
                List<AssignmentStatus> enumStatuses = status.stream()
                    .map(String::toUpperCase) // Đảm bảo là chữ hoa
                    .map(AssignmentStatus::valueOf) // Chuyển sang Enum
                    .collect(Collectors.toList());
                
                return root.get("status").in(enumStatuses);

            } catch (IllegalArgumentException e) {
                // Xử lý nếu client gửi "status" bậy (ví dụ: "DELIVEREDD")
                // Trả về 0 kết quả
                log.debug("[session-service] [AssignmentSpecification.build] Invalid status value provided: {}", status);
                return criteriaBuilder.disjunction(); 
            }
        };
    }

    /**
     * Lọc theo thời gian TẠO TASK (scanedAt).
     * Nhận vào String thay vì LocalDate để linh hoạt hơn (controller gửi String).
     */
    public static Specification<DeliveryAssignment> isCreatedAtBetween(String start, String end) {
        return (root, query, criteriaBuilder) -> {
            try {
                // Hỗ trợ lọc 1 chiều (chỉ có start hoặc chỉ có end)
                LocalDateTime startDateTime = StringUtils.hasText(start) ? LocalDate.parse(start).atStartOfDay() : null;
                // Kết thúc là cuối ngày (23:59:59.999...)
                LocalDateTime endDateTime = StringUtils.hasText(end) ? LocalDate.parse(end).atTime(LocalTime.MAX) : null;

                if (startDateTime != null && endDateTime != null) {
                    return criteriaBuilder.between(root.get("scanedAt"), startDateTime, endDateTime);
                } else if (startDateTime != null) {
                    return criteriaBuilder.greaterThanOrEqualTo(root.get("scanedAt"), startDateTime);
                } else if (endDateTime != null) {
                    return criteriaBuilder.lessThanOrEqualTo(root.get("scanedAt"), endDateTime);
                } else {
                    return criteriaBuilder.conjunction(); // Không lọc
                }
            } catch (DateTimeParseException e) {
                // Client gửi định dạng ngày sai
                log.error("Invalid date format provided for createdAt: start={}, end={}", start, end, e);
                return criteriaBuilder.disjunction(); // Trả về 0 kết quả
            }
        };
    }

    /**
     * Lọc theo thời gian HOÀN THÀNH TASK (updatedAt).
     * Nhận vào String thay vì LocalDate.
     */
    public static Specification<DeliveryAssignment> isCompletedAtBetween(String start, String end) {
        return (root, query, criteriaBuilder) -> {
             try {
                LocalDateTime startDateTime = StringUtils.hasText(start) ? LocalDate.parse(start).atStartOfDay() : null;
                LocalDateTime endDateTime = StringUtils.hasText(end) ? LocalDate.parse(end).atTime(LocalTime.MAX) : null;

                Predicate terminalStatusSpec = 
                    root.get("status").in(AssignmentStatus.COMPLETED, AssignmentStatus.FAILED);

                if (startDateTime != null && endDateTime != null) {
                    Predicate dateSpec = 
                        criteriaBuilder.between(root.get("updatedAt"), startDateTime, endDateTime);
                    return criteriaBuilder.and(dateSpec, terminalStatusSpec);
                    
                } else if (startDateTime != null) {
                     Predicate dateSpec = 
                        criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), startDateTime);
                     return criteriaBuilder.and(dateSpec, terminalStatusSpec);

                } else if (endDateTime != null) {
                    Predicate dateSpec = 
                        criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), endDateTime);
                    return criteriaBuilder.and(dateSpec, terminalStatusSpec);

                } else {
                    // Nếu không lọc ngày, không cần lọc status
                    // Trả về terminalStatusSpec nếu client chỉ muốn lọc 
                    // task đã hoàn thành mà không lọc ngày.
                    // Nếu muốn trả về tất cả, dùng criteriaBuilder.conjunction()
                    return terminalStatusSpec; 
                }
            } catch (DateTimeParseException e) {
                log.error("Invalid date format provided for completedAt: start={}, end={}", start, end, e);
                return criteriaBuilder.disjunction(); // Trả về 0 kết quả
            }
        };
    }
}
