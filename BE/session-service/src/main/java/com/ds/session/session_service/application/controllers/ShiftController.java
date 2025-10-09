package com.ds.session.session_service.application.controllers;

import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ds.session.session_service.app_context.models.Shift;
import com.ds.session.session_service.common.entities.dto.request.ShiftRequest;
import com.ds.session.session_service.common.enums.ShiftType;
import com.ds.session.session_service.common.interfaces.IShiftService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftController {
    private final IShiftService shiftService;

    @PostMapping
    public ResponseEntity<Shift> createShift(@RequestBody ShiftRequest request) {
        return ResponseEntity.ok(shiftService.createShift(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shift> updateShift(@PathVariable Long id, @RequestBody ShiftRequest request) {
        return ResponseEntity.ok(shiftService.updateShift(id, request));
    }

    @GetMapping
    public ResponseEntity<List<Shift>> getAllShifts() {
        return ResponseEntity.ok(shiftService.getAllShifts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getShiftById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<Shift> getShiftByType(@PathVariable ShiftType type) {
        return ResponseEntity.ok(shiftService.getShiftByType(type));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateShiftTime(
            @RequestParam ShiftType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime end) {
        return ResponseEntity.ok(shiftService.isInShift(type, start, end));
    }
}


