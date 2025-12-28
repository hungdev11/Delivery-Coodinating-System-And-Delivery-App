package com.ds.communication_service.common.utils;

import com.ds.communication_service.common.dto.filter.FilterOperator;
import com.ds.communication_service.common.dto.filter.v2.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Query parser for V2 filter system
 */
@Slf4j
public class EnhancedQueryParserV2 {

    public static <T> Specification<T> parseFilterGroup(FilterGroupItemV2 filterGroup, Class<T> entityClass) {
        if (filterGroup == null || filterGroup.getItems() == null || filterGroup.getItems().isEmpty()) {
            return Specification.where(null);
        }

        return (root, query, criteriaBuilder) -> {
            return buildPredicate(root, criteriaBuilder, filterGroup.getItems(), entityClass, query);
        };
    }

    private static <T> Predicate buildPredicate(Root<T> root, CriteriaBuilder criteriaBuilder, 
                                               List<FilterItemV2> items, Class<T> entityClass,
                                               CriteriaQuery<?> query) {
        if (items == null || items.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        List<Predicate> predicates = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        for (FilterItemV2 item : items) {
            if (item instanceof FilterConditionItemV2) {
                FilterConditionItemV2 condition = (FilterConditionItemV2) item;
                Predicate predicate = parseFilterCondition(root, criteriaBuilder, condition);
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

        return combinePredicates(criteriaBuilder, predicates, operators);
    }

    private static Predicate combinePredicates(CriteriaBuilder criteriaBuilder, 
                                             List<Predicate> predicates, 
                                             List<String> operators) {
        if (predicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

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

    private static <T> Predicate parseFilterCondition(Root<T> root, CriteriaBuilder criteriaBuilder, 
                                                    FilterConditionItemV2 condition) {
        try {
            String field = condition.getField();
            FilterOperator operator = condition.getOperator();
            Object value = condition.getValue();

            Path<?> fieldPath = getFieldPath(root, field);
            if (fieldPath == null) {
                log.debug("[communication-service] [EnhancedQueryParserV2.parseCondition] Field path not found: {}", field);
                return null;
            }

            switch (operator) {
                case EQUALS:
                    return criteriaBuilder.equal(fieldPath, value);
                case NOT_EQUALS:
                    return criteriaBuilder.notEqual(fieldPath, value);
                case GREATER_THAN:
                    return criteriaBuilder.greaterThan((Path<Comparable>) fieldPath, (Comparable) value);
                case GREATER_THAN_OR_EQUAL:
                    return criteriaBuilder.greaterThanOrEqualTo((Path<Comparable>) fieldPath, (Comparable) value);
                case LESS_THAN:
                    return criteriaBuilder.lessThan((Path<Comparable>) fieldPath, (Comparable) value);
                case LESS_THAN_OR_EQUAL:
                    return criteriaBuilder.lessThanOrEqualTo((Path<Comparable>) fieldPath, (Comparable) value);
                case IN:
                    if (value instanceof List) {
                        return fieldPath.in((List<?>) value);
                    }
                    return null;
                case NOT_IN:
                    if (value instanceof List) {
                        return criteriaBuilder.not(fieldPath.in((List<?>) value));
                    }
                    return null;
                case IS_NULL:
                    return criteriaBuilder.isNull(fieldPath);
                case IS_NOT_NULL:
                    return criteriaBuilder.isNotNull(fieldPath);
                case CONTAINS:
                    boolean caseSensitive = condition.getCaseSensitive() != null && condition.getCaseSensitive();
                    if (caseSensitive) {
                        return criteriaBuilder.like(fieldPath.as(String.class), "%" + value + "%");
                    } else {
                        return criteriaBuilder.like(
                            criteriaBuilder.lower(fieldPath.as(String.class)), 
                            "%" + value.toString().toLowerCase() + "%"
                        );
                    }
                case STARTS_WITH:
                    caseSensitive = condition.getCaseSensitive() != null && condition.getCaseSensitive();
                    if (caseSensitive) {
                        return criteriaBuilder.like(fieldPath.as(String.class), value + "%");
                    } else {
                        return criteriaBuilder.like(
                            criteriaBuilder.lower(fieldPath.as(String.class)), 
                            value.toString().toLowerCase() + "%"
                        );
                    }
                case ENDS_WITH:
                    caseSensitive = condition.getCaseSensitive() != null && condition.getCaseSensitive();
                    if (caseSensitive) {
                        return criteriaBuilder.like(fieldPath.as(String.class), "%" + value);
                    } else {
                        return criteriaBuilder.like(
                            criteriaBuilder.lower(fieldPath.as(String.class)), 
                            "%" + value.toString().toLowerCase()
                        );
                    }
                default:
                    log.debug("[communication-service] [EnhancedQueryParserV2.parseCondition] Unsupported operator: {}", operator);
                    return null;
            }
            
        } catch (Exception e) {
            log.error("Error parsing filter condition: {}", e.getMessage(), e);
            return null;
        }
    }

    private static <T> Path<?> getFieldPath(Root<T> root, String field) {
        try {
            String[] parts = field.split("\\.");
            Path<?> path = root;
            for (String part : parts) {
                path = path.get(part);
            }
            return path;
        } catch (Exception e) {
            log.debug("[communication-service] [EnhancedQueryParserV2.getFieldPath] Error getting field path for: {}, error: {}", field, e.getMessage());
            return null;
        }
    }
}
