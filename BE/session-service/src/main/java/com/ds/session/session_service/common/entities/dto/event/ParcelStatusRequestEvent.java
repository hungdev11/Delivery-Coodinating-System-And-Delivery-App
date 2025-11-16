package com.ds.session.session_service.common.entities.dto.event;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelStatusRequestEvent {
    private String eventId;
    private String parcelId;
    private String sourceService;
    private String eventType;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
