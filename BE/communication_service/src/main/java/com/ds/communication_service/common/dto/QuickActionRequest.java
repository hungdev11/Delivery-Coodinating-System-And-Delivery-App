package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for quick action requests on proposals
 * Enables 2-touch interaction for shippers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickActionRequest {
    /**
     * ID of the proposal being acted upon
     */
    private String proposalId;
    
    /**
     * Action type: ACCEPT, REJECT, POSTPONE
     */
    private ActionType action;
    
    /**
     * Optional note for the action
     */
    private String note;
    
    /**
     * For POSTPONE: new window start time
     */
    private LocalDateTime postponeWindowStart;
    
    /**
     * For POSTPONE: new window end time
     */
    private LocalDateTime postponeWindowEnd;
    
    /**
     * ID of the user performing the action
     */
    private String userId;
    
    public enum ActionType {
        ACCEPT,
        REJECT,
        POSTPONE
    }
}
