package com.ds.session.session_service.common.interfaces;

import java.time.LocalTime;
import java.util.List;

import com.ds.session.session_service.app_context.models.Shift;
import com.ds.session.session_service.common.entities.dto.request.ShiftRequest;
import com.ds.session.session_service.common.enums.ShiftType;

public interface IShiftService {
    Shift createShift(ShiftRequest shift);
    Shift updateShift(Long id, ShiftRequest updated);
    List<Shift> getAllShifts();
    Shift getShiftById(Long id);
    void deleteShift(Long id);
    Shift getShiftByType(ShiftType type);
    boolean isInShift(ShiftType type, LocalTime windowStart, LocalTime windowEnd);
}
