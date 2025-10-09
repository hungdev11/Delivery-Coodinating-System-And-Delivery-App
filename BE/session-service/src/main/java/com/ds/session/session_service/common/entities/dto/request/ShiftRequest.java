package com.ds.session.session_service.common.entities.dto.request;

import java.time.LocalTime;

import com.ds.session.session_service.common.enums.ShiftType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRequest {
    private ShiftType type;
    private LocalTime startTime;
    private LocalTime endTime;
}
