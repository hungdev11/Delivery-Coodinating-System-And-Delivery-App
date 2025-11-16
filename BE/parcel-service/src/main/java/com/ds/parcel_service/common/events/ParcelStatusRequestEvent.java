package com.ds.parcel_service.common.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Typed DTO for parcel status request events.
 * Keep this class stable and (optionally) duplicated across services while you don't have a shared module.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParcelStatusRequestEvent {
    private UUID parcelId;
    private String eventType;
    private String eventId;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
