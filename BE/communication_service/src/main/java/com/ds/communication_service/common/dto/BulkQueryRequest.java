package com.ds.communication_service.common.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for bulk query operations
 * Supports querying multiple items by list of IDs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkQueryRequest {
    
    /**
     * List of IDs to query
     * Can be UUIDs (for tickets) or Strings (for parcel/assignment/reporter IDs)
     */
    @NotEmpty(message = "IDs list cannot be empty")
    private List<String> ids;
    
    /**
     * Optional: Additional filter criteria
     */
    private String filterType; // e.g., "parcel", "assignment", "reporter", "ticket"
}
