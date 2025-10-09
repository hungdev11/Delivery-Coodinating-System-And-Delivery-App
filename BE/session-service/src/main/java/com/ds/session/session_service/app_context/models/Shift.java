package com.ds.session.session_service.app_context.models;

import java.time.LocalTime;
import com.ds.session.session_service.common.enums.ShiftType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {
    @Id()
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private ShiftType type;
    private LocalTime startTime;
    private LocalTime endTime;

    public boolean validateShiftTime() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }


    public boolean isInShiftTime(LocalTime windowStart, LocalTime windowEnd) {
        if (startTime == null || endTime == null || windowStart == null || windowEnd == null) return false;
        return startTime.isBefore(windowStart) && endTime.isBefore(windowEnd);
    }
}
