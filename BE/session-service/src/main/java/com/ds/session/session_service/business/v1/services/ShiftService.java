package com.ds.session.session_service.business.v1.services;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.Shift;
import com.ds.session.session_service.app_context.repositories.ShiftRepository;
import com.ds.session.session_service.common.entities.dto.request.ShiftRequest;
import com.ds.session.session_service.common.enums.ShiftType;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IShiftService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftService implements IShiftService{
    private final ShiftRepository shiftRepository;

    public Shift createShift(ShiftRequest shift) {
        if (shiftRepository.existsByType(shift.getType())) {
            throw new IllegalArgumentException("Shift type already exists");
        }
        Shift newShift = Shift.builder()
                            .type(shift.getType())
                            .startTime(shift.getStartTime())
                            .endTime(shift.getEndTime())
                            .build();
        if (!newShift.validateShiftTime()) {
            throw new IllegalArgumentException("Invalid shift time");
        }
        return shiftRepository.save(newShift);
    }

    public Shift updateShift(Long id, ShiftRequest updated) {
        Shift shift = shiftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFound("Shift not found"));

        shift.setType(updated.getType());
        shift.setStartTime(updated.getStartTime());
        shift.setEndTime(updated.getEndTime());

        if (!shift.validateShiftTime()) {
            throw new IllegalArgumentException("Invalid shift time");
        }

        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public List<Shift> getAllShifts() {
        return shiftRepository.findAll(Sort.by("startTime"));
    }

    @Transactional(readOnly = true)
    public Shift getShiftById(Long id) {
        return shiftRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFound("Shift not found"));
    }

    public void deleteShift(Long id) {
        if (!shiftRepository.existsById(id)) {
            throw new IllegalArgumentException("Shift not found");
        }
        //shiftRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Shift getShiftByType(ShiftType type) {
        return shiftRepository.findByType(type)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found for type: " + type));
    }

    /**
     * Kiểm tra một khung giờ có nằm trong ca hay không.
     */
    @Transactional(readOnly = true)
    public boolean isInShift(ShiftType type, LocalTime windowStart, LocalTime windowEnd) {
        Shift shift = getShiftByType(type);
        return shift.isInShiftTime(windowStart, windowEnd);
    }
}

