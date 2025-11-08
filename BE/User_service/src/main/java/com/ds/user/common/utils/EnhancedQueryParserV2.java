package com.ds.user.common.utils;

import com.ds.user.common.entities.common.filter.FilterOperator;
import com.ds.user.common.entities.common.filter.v2.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced query parser for V2 filter system
 * V2 allows operations between each pair of conditions/groups
 */
@Slf4j
public class EnhancedQueryParserV2 {

    /**
     * Parse V2 FilterGroup to JPA Specification
     * V2 structure has operators between each pair of items
     */
    public static <T> Specification<T> parseFilterGroup(FilterGroupItemV2 filterGroup, Class<T> entityClass) {
        if (filterGroup == null || filterGroup.getItems() == null || filterGroup.getItems().isEmpty()) {
            return Specification.where(null);
        }

        return (root, query, criteriaBuilder) -> {
            return buildPredicate(root, criteriaBuilder, filterGroup.getItems(), entityClass, query);
        };
    }

    /**
     * Build predicate from list of filter items
     * Items alternate between conditions/groups and operators
     */
    private static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder criteriaBuilder, 
                                               List<FilterItemV2> items, Class<T> entityClass,
                                               CriteriaQuery<?> query) {
        if (items == null || items.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        List<Predicate> predicates = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            FilterItemV2 item = items.get(i);
            
            if (item instanceof FilterConditionItemV2) {
                FilterConditionItemV2 condition = (FilterConditionItemV2) item;
                Predicate predicate = parseFilterCondition(root, criteriaBuilder, condition, entityClass);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            } else if (item instanceof FilterGroupItemV2) {
                FilterGroupItemV2 nestedGroup = (FilterGroupItemV2) item;
                Specification<T> nestedSpec = parseFilterGroup(nestedGroup, entityClass);
                if (nestedSpec != null) {
                    Predicate nestedPredicate = nestedSpec.toPredicate(root, query, criteriaBuilder);
                    if (nestedPredicate != null) {
                        predicates.add(nestedPredicate);
                    }
                }
            } else if (item instanceof FilterOperatorItemV2) {
                FilterOperatorItemV2 operator = (FilterOperatorItemV2) item;
                operators.add(operator.getValue());
            }
        }

        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

        // Combine predicates with operators between each pair
        return combinePredicates(criteriaBuilder, predicates, operators);
    }

    /**
     * Combine predicates with operators between each pair
     */
    private static Predicate combinePredicates(CriteriaBuilder criteriaBuilder, 
                                             List<Predicate> predicates, 
                                             List<String> operators) {
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

        // Build predicate tree from left to right
        // pred1 OP1 pred2 OP2 pred3 => (pred1 OP1 pred2) OP2 pred3
        Predicate result = predicates.get(0);
        
        for (int i = 1; i < predicates.size(); i++) {
            String operator = (i - 1 < operators.size()) ? operators.get(i - 1) : "AND";
            Predicate nextPredicate = predicates.get(i);
            
            if ("OR".equalsIgnoreCase(operator)) {
                result = criteriaBuilder.or(result, nextPredicate);
            } else {
                result = criteriaBuilder.and(result, nextPredicate);
            }
        }
        
        return result;
    }

    /**
     * Parse individual filter condition (reuse from EnhancedQueryParser)
     */
    private static <T> Predicate parseFilterCondition(Root<T> root, CriteriaBuilder criteriaBuilder, 
                                                    FilterConditionItemV2 condition, Class<T> entityClass) {
        try {
            String field = condition.getField();
            FilterOperator operator = condition.getOperator();
            Object value = condition.getValue();
            Boolean caseSensitive = condition.getCaseSensitive();

            // Get field path
            Path<?> fieldPath = getFieldPath(root, field);
            if (fieldPath == null) {
                log.warn("Field path not found: {}", field);
                return null;
            }

            // Use the same logic from EnhancedQueryParser
            return EnhancedQueryParser.buildPredicate(criteriaBuilder, fieldPath, operator, value, caseSensitive);
            
        } catch (Exception e) {
            log.error("Error parsing filter condition: field={}, operator={}, value={}, error={}", 
                    condition.getField(), condition.getOperator(), condition.getValue(), e.getMessage());
            return null;
        }
    }

    /**
     * Get field path from root, handling nested properties
     */
    private static <T> Path<?> getFieldPath(Root<T> root, String field) {
        try {
            String[] parts = field.split("\\.");
            Path<?> path = root;
            for (String part : parts) {
                path = path.get(part);
            }
            return path;
        } catch (Exception e) {
            log.warn("Error getting field path for: {}, error: {}", field, e.getMessage());
            return null;
        }
    }
}
