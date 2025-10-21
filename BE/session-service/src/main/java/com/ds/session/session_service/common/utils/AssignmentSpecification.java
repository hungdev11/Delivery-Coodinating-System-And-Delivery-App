package com.ds.session.session_service.common.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;

public class AssignmentSpecification {

    public static Specification<DeliveryAssignment> byDeliveryManId(UUID deliveryManId) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("deliveryManId"), deliveryManId.toString());
    }

    public static Specification<DeliveryAssignment> hasStatusIn(List<String> status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(status);
        };
    }

    public static Specification<DeliveryAssignment> isCreatedAtBetween(LocalDate start, LocalDate end) {
        return (root, query, criteriaBuilder) -> {
        if (start == null || end == null) {
            return criteriaBuilder.conjunction();
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        
        LocalDateTime endDateTimeExclusive = end.plusDays(1).atStartOfDay(); 

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(root.get("scanedAt"), startDateTime), // >= 15/10 00:00:00
            criteriaBuilder.lessThan(root.get("scanedAt"), endDateTimeExclusive)        // < 16/10 00:00:00
        );

    };
    }

    public static Specification<DeliveryAssignment> isCompletedAtBetween(LocalDate start, LocalDate end) {
        return (root, query, criteriaBuilder) -> {
            if (start == null || end == null) {
                return criteriaBuilder.conjunction();
            }
            LocalDateTime startDateTime = start.atStartOfDay();
        
        LocalDateTime endDateTimeExclusive = end.plusDays(1).atStartOfDay(); 

        return criteriaBuilder.and(
            criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), startDateTime), // >= 15/10 00:00:00
            criteriaBuilder.lessThan(root.get("updatedAt"), endDateTimeExclusive)        // < 16/10 00:00:00
        );

        };
    }
}
