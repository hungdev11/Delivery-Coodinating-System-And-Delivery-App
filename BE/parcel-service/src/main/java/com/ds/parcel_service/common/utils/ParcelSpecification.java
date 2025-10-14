package com.ds.parcel_service.common.utils;

import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;

import jakarta.persistence.criteria.Predicate;

public class ParcelSpecification {
    public static Specification<Parcel> buildeSpecification(ParcelFilterRequest filter) {
        return (root, query, builder) -> {
            Predicate predicate = builder.conjunction(); // start with AND opt
            if (Objects.nonNull(filter.getStatus())) {
                predicate = builder.and(predicate, 
                    builder.equal(root.get("status"), filter.getStatus())
                );
            }

            if (Objects.nonNull(filter.getDeliveryType())) {
                predicate = builder.and(predicate, 
                    builder.equal(root.get("deliveryType"), filter.getDeliveryType())
                );
            }

            //filter by range of time
            if (Objects.nonNull(filter.getCreatedFrom())) {
                predicate = builder.and(predicate, 
                    builder.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
            }
            if (Objects.nonNull(filter.getCreatedTo())) {
                predicate = builder.and(predicate, 
                    builder.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedTo()));
            }

            return predicate;
        };
    }
}
