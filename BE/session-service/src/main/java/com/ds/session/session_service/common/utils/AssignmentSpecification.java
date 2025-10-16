package com.ds.session.session_service.common.utils;

import java.time.LocalDate;
import java.time.LocalTime;
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
            return criteriaBuilder.between(
                root.get("scanedAt"),
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
            );
        };
    }

    public static Specification<DeliveryAssignment> isCompletedAtBetween(LocalDate start, LocalDate end) {
        return (root, query, criteriaBuilder) -> {
            if (start == null || end == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.between(
                root.get("completedAt"),
                start.atStartOfDay(),
                end.atTime(LocalTime.MAX)
            );
        };
    }
}
