package com.ds.session.session_service.common.entities.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShipperInfo {
    private String id;
    private String name;
    private String phone;
}
