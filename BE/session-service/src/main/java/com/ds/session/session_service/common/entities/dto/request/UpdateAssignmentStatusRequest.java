package com.ds.session.session_service.common.entities.dto.request;

import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating assignment status by sessionId and assignmentId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssignmentStatusRequest {
    @NotNull(message = "Assignment status is required")
    private AssignmentStatus assignmentStatus;
    
    @NotNull(message = "Parcel event is required")
    private ParcelEvent parcelEvent;
    
    private String failReason;
    
    private RouteInfo routeInfo;
}
