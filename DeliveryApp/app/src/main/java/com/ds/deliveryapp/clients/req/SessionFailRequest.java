package com.ds.deliveryapp.clients.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SessionFailRequest {
    private String reason;
}