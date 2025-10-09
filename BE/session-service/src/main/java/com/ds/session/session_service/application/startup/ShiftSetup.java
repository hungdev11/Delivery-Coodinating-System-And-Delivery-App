package com.ds.session.session_service.application.startup;

import java.time.LocalTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ds.session.session_service.common.entities.dto.request.ShiftRequest;
import com.ds.session.session_service.common.enums.ShiftType;
import com.ds.session.session_service.common.interfaces.IShiftService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@RequiredArgsConstructor
public class ShiftSetup implements CommandLineRunner {

    private final IShiftService shiftService;

    @Override
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("INITIALIZING SHIFT SERVICE INTEGRATION");
        log.info("=".repeat(80));

        long startTime = System.currentTimeMillis();

        initDefaultShifts();

        long endTime = System.currentTimeMillis();
        log.info("Shift Service initialization completed in {} ms", (endTime - startTime));
        log.info("=".repeat(80));
    }

    private void initDefaultShifts() {
        List<ShiftType> defaultShifts = List.of(ShiftType.MORNING, ShiftType.AFTERNOON, ShiftType.EVENING);
        for (ShiftType type : defaultShifts) {
            try {
                shiftService.getShiftByType(type);
            } catch (IllegalArgumentException exception) {
                ShiftRequest request = switch (type) {
                    case MORNING -> new ShiftRequest(type, LocalTime.of(8, 0), LocalTime.of(12, 0));
                    case AFTERNOON -> new ShiftRequest(type, LocalTime.of(12, 0), LocalTime.of(17, 0));
                    case EVENING -> new ShiftRequest(type, LocalTime.of(17, 0), LocalTime.of(22, 0));
                };
                shiftService.createShift(request);
                log.info("Created default shift: {}", type);
            }
        }
    }
}
