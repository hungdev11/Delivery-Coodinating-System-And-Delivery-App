package com.ds.session.session_service.common.entities.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SessionFailRequest {
    private String reason;
}
