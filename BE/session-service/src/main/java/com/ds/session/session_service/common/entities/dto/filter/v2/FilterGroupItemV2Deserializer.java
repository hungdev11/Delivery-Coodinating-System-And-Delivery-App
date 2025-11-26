package com.ds.session.session_service.common.entities.dto.filter.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ds.session.session_service.common.entities.dto.filter.FilterOperator;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for FilterGroupItemV2 to support both old and new formats
 * 
 * Old format:
 * {
 *   "logic": "AND",
 *   "conditions": [
 *     {"field": "deliveryManId", "operator": "eq", "value": "..."}
 *   ]
 * }
 * 
 * New format:
 * {
 *   "type": "group",
 *   "items": [
 *     {"type": "condition", "field": "deliveryManId", "operator": "eq", "value": "..."}
 *   ]
 * }
 */
@Slf4j
public class FilterGroupItemV2Deserializer extends JsonDeserializer<FilterGroupItemV2> {

    @Override
    public FilterGroupItemV2 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        
        // Check if it's old format (has "logic" and "conditions")
        if (node.has("logic") && node.has("conditions")) {
            log.debug("[session-service] [FilterGroupItemV2Deserializer] Detected old format with logic and conditions");
            return deserializeOldFormat(node, mapper);
        }
        
        // Otherwise, deserialize as new format
        log.debug("[session-service] [FilterGroupItemV2Deserializer] Deserializing new format");
        return deserializeNewFormat(node, mapper);
    }
    
    private FilterGroupItemV2 deserializeOldFormat(JsonNode node, ObjectMapper mapper) {
        String logic = node.get("logic").asText("AND");
        JsonNode conditionsNode = node.get("conditions");
        
        List<FilterItemV2> items = new ArrayList<>();
        
        if (conditionsNode != null && conditionsNode.isArray()) {
            int conditionCount = conditionsNode.size();
            for (int i = 0; i < conditionCount; i++) {
                JsonNode conditionNode = conditionsNode.get(i);
                
                // Convert old condition format to new condition format
                FilterConditionItemV2 condition = new FilterConditionItemV2();
                condition.setType(FilterItemType.CONDITION);
                
                if (conditionNode.has("field")) {
                    condition.setField(conditionNode.get("field").asText());
                }
                
                if (conditionNode.has("operator")) {
                    String operatorStr = conditionNode.get("operator").asText().toLowerCase();
                    FilterOperator operator = FilterOperator.fromValue(operatorStr);
                    if (operator == null) {
                        // Try to map common aliases
                        switch (operatorStr) {
                            case "eq":
                            case "equals":
                                operator = FilterOperator.EQUALS;
                                break;
                            case "ne":
                            case "not_equals":
                                operator = FilterOperator.NOT_EQUALS;
                                break;
                            case "gt":
                            case "greater_than":
                                operator = FilterOperator.GREATER_THAN;
                                break;
                            case "gte":
                            case "greater_than_or_equal":
                                operator = FilterOperator.GREATER_THAN_OR_EQUAL;
                                break;
                            case "lt":
                            case "less_than":
                                operator = FilterOperator.LESS_THAN;
                                break;
                            case "lte":
                            case "less_than_or_equal":
                                operator = FilterOperator.LESS_THAN_OR_EQUAL;
                                break;
                            case "in":
                                operator = FilterOperator.IN;
                                break;
                            case "not_in":
                                operator = FilterOperator.NOT_IN;
                                break;
                            case "contains":
                                operator = FilterOperator.CONTAINS;
                                break;
                            case "starts_with":
                                operator = FilterOperator.STARTS_WITH;
                                break;
                            case "ends_with":
                                operator = FilterOperator.ENDS_WITH;
                                break;
                            default:
                                log.debug("[session-service] [FilterGroupItemV2Deserializer] Unknown operator: {}, defaulting to EQUALS", operatorStr);
                                operator = FilterOperator.EQUALS;
                        }
                    }
                    condition.setOperator(operator);
                }
                
                if (conditionNode.has("value")) {
                    JsonNode valueNode = conditionNode.get("value");
                    if (valueNode.isTextual()) {
                        condition.setValue(valueNode.asText());
                    } else if (valueNode.isNumber()) {
                        condition.setValue(valueNode.asDouble());
                    } else if (valueNode.isBoolean()) {
                        condition.setValue(valueNode.asBoolean());
                    } else if (valueNode.isArray()) {
                        List<Object> values = new ArrayList<>();
                        for (JsonNode item : valueNode) {
                            if (item.isTextual()) {
                                values.add(item.asText());
                            } else if (item.isNumber()) {
                                values.add(item.asDouble());
                            } else if (item.isBoolean()) {
                                values.add(item.asBoolean());
                            }
                        }
                        condition.setValue(values);
                    } else {
                        condition.setValue(valueNode.asText());
                    }
                }
                
                items.add(condition);
                
                // Add operator between conditions (not after the last one)
                if (i < conditionCount - 1) {
                    FilterOperatorItemV2 operator = new FilterOperatorItemV2();
                    operator.setType(FilterItemType.OPERATOR);
                    operator.setValue(logic);
                    items.add(operator);
                }
            }
        }
        
        FilterGroupItemV2 group = new FilterGroupItemV2();
        group.setType(FilterItemType.GROUP);
        group.setItems(items);
        
        log.debug("[session-service] [FilterGroupItemV2Deserializer] Converted old format to new format: {} conditions with logic {}", 
            conditionsNode != null ? conditionsNode.size() : 0, logic);
        
        return group;
    }
    
    private FilterGroupItemV2 deserializeNewFormat(JsonNode node, ObjectMapper mapper) {
        // Create a new ObjectMapper without this custom deserializer to avoid infinite loop
        // Use the original mapper but manually deserialize to avoid recursion
        FilterGroupItemV2 group = new FilterGroupItemV2();
        group.setType(FilterItemType.GROUP);
        
        if (node.has("items") && node.get("items").isArray()) {
            List<FilterItemV2> items = new ArrayList<>();
            for (JsonNode itemNode : node.get("items")) {
                try {
                    if (itemNode.has("type")) {
                        String type = itemNode.get("type").asText();
                        if ("condition".equals(type)) {
                            FilterConditionItemV2 condition = mapper.treeToValue(itemNode, FilterConditionItemV2.class);
                            items.add(condition);
                        } else if ("operator".equals(type)) {
                            FilterOperatorItemV2 operator = mapper.treeToValue(itemNode, FilterOperatorItemV2.class);
                            items.add(operator);
                        } else if ("group".equals(type)) {
                            // Recursive call for nested groups - but this will use the custom deserializer again
                            // So we need to manually deserialize nested groups too
                            FilterGroupItemV2 nestedGroup = deserializeNewFormat(itemNode, mapper);
                            items.add(nestedGroup);
                        }
                    }
                } catch (Exception ex) {
                    log.debug("[session-service] [FilterGroupItemV2Deserializer] Error deserializing item: {}", ex.getMessage());
                }
            }
            group.setItems(items);
        }
        
        return group;
    }
}
