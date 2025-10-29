package com.ds.deliveryapp.clients.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFailRequest {
    private String reason;
    private RouteInfo routeInfo;
}
