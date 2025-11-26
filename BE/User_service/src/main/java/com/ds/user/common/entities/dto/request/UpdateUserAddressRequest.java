package com.ds.user.common.entities.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserAddressRequest {
    private String destinationId;
    private String note;
    private String tag;
    private Boolean isPrimary;
}
