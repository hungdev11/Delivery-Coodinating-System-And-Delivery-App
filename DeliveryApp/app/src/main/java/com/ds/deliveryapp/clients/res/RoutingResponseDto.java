package com.ds.deliveryapp.clients.res;

import com.ds.deliveryapp.clients.req.RoutingRequestDto;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * Routing Response Models
 * DTOs for routing module outputs
 */
public class RoutingResponseDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManeuverDto {
        private String type;
        private String modifier;
        private List<Double> location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteStepDto {
        private double distance;
        private double duration;
        private String instruction;
        private String name;
        private ManeuverDto maneuver;
        private Geometry geometry;
        private List<String> addresses;
        private String trafficLevel;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Geometry {
            private String type; // e.g. "LineString"
            private List<List<Double>> coordinates;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteLegDto {
        private String parcelId;
        private double distance;
        private double duration;
        private List<RouteStepDto> steps;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrafficSummaryDto {
        private double averageSpeed;
        private String congestionLevel;
        private double estimatedDelay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteDto {
        private double distance;
        private double duration;
        private String geometry;
        private List<RouteLegDto> legs;
        private TrafficSummaryDto trafficSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteResponseDto {
        private String code;
        private RouteDto route;
        private List<VisitOrder> visitOrder;
        private Summary summary;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class VisitOrder {
            private int index;
            private int priority;
            private String priorityLabel;
            private RoutingRequestDto.WaypointDto waypoint;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Summary {
            private double totalDistance;
            private double totalDuration;
            private int totalWaypoints;
            private Map<String, Integer> priorityCounts;
        }
    }
}
