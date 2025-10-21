package com.ds.deliveryapp.clients.req;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteInfo {
    private double distanceM;
    private long durationS;
    private String waypoints;
}
