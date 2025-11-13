package com.ds.user.common.entities.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserAddressRequest {
    @NotBlank(message = "destinationId must not be blank")
    private String destinationId;
    
    private String note;
    
    private String tag; // e.g., "Home", "Work", "Other"
    
    @Builder.Default
    private Boolean isPrimary = false;
}
