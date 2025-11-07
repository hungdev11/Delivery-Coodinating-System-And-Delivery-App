package com.ds.deliveryapp.clients.req;

import lombok.*;
import java.util.List;

/**
 * Routing Request Models
 * DTOs for routing module inputs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRequestDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WaypointDto {
        private String parcelId;
        private double lat;
        private double lon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriorityGroupDto {
        private int priority; // 0 = urgent, 1 = express, 2 = fast, 3 = normal, 4 = economy
        private List<WaypointDto> waypoints;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteRequestDto {
        private WaypointDto startPoint;
        private List<PriorityGroupDto> priorityGroups;
        private Boolean steps;
        private Boolean annotations;
        private String vehicle;   // car | motorbike
        private String mode;      // priority_first | speed_leaning | balanced | no_recommend | base
        private String strategy;  // strict_urgent | flexible
    }
}
