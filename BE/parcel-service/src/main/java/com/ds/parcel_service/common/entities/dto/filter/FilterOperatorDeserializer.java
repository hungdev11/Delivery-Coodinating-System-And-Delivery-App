package com.ds.parcel_service.common.entities.dto.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom deserializer for FilterOperator enum
 * Handles both enum name (EQUALS) and enum value (equals)
 */
public class FilterOperatorDeserializer extends JsonDeserializer<FilterOperator> {
    
    @Override
    public FilterOperator deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        String normalized = value.trim().toUpperCase();
        
        // Try to match by enum name first (case-insensitive)
        try {
            return FilterOperator.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // If not found by name, try to match by value
            for (FilterOperator operator : FilterOperator.values()) {
                if (operator.getValue().equalsIgnoreCase(value) || 
                    operator.name().equalsIgnoreCase(value)) {
                    return operator;
                }
            }
        }
        
        throw new IOException("Unknown FilterOperator value: " + value);
    }
}
