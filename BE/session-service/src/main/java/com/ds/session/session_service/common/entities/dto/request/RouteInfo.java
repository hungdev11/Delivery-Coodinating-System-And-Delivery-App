package com.ds.session.session_service.common.entities.dto.request;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteInfo {
    private LocalTime startTime;
    private LocalTime finishTime;
    private double distanceM;
    private long durationS;
    private String waypoints;
}
