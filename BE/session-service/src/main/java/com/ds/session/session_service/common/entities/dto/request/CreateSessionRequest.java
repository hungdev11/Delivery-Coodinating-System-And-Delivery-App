package com.ds.session.session_service.common.entities.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSessionRequest {
    
    @NotNull(message = "deliveryManId is required")
    private String deliveryManId;

    @NotEmpty(message = "assignmentsIds list cannot be empty")
    private List<String> assignmentsIds;
}
