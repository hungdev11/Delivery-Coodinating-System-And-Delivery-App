package com.ds.deliveryapp.clients.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskFailRequest {
    private String reason;
    private RouteInfo routeInfo;
}
