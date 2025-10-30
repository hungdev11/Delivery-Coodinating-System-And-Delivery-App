package com.ds.session.session_service.common.entities.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskFailRequest {
    
    @NotBlank(message = "Reason is required")
    private String reason;

    @Valid
    @NotNull(message = "RouteInfo is required")
    private RouteInfo routeInfo;
}

