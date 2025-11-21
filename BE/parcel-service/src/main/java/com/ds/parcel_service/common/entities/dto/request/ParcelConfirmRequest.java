package com.ds.parcel_service.common.entities.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParcelConfirmRequest {
    private UUID assignmentId;

    /**
     * Optional note from client (feedback or signature).
     */
    private String note;

    @NotBlank(message = "confirmationSource is required")
    private String confirmationSource; // e.g. WEB_CLIENT, CHAT
}

