package com.ds.session.session_service.common.entities.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SessionFailRequest {
    @NotBlank(message = "Reason is required")
    private String reason;
}
