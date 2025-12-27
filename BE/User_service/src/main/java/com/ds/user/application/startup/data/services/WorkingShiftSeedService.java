package com.ds.user.application.startup.data.services;

import com.ds.user.app_context.repositories.DeliveryManRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.app_context.repositories.WorkingShiftRepository;
import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.entities.base.DeliveryMan;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.WorkingShift;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service to seed working shifts for delivery men from configuration
 */
@Slf4j
@Service
public class WorkingShiftSeedService {

    @Autowired
    private WorkingShiftRepository workingShiftRepository;

    @Autowired
    private DeliveryManRepository deliveryManRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Seed working shifts for all delivery men from configuration
     * Only seeds if database is empty (no shifts exist)
     */
    @Transactional
    public void seedShiftsForAllDeliveryMen(KeycloakInitConfig.RealmConfig realmConfig) {
        // Check if database already has shifts - if yes, skip seeding
        long existingShiftCount = workingShiftRepository.count();
        if (existingShiftCount > 0) {
            log.info("✓ Working shift seeding skipped: Database already has {} shift(s). Skipping seed.", existingShiftCount);
            return;
        }

        log.info("🌱 Starting working shift seeding for delivery men...");

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;
        int shiftCount = 0;

        for (KeycloakInitConfig.UserConfig userConfig : realmConfig.getUsers()) {
            // Only seed shifts for SHIPPER users with deliveryMan config
            if (userConfig.getDeliveryMan() == null) {
                continue;
            }

            boolean isShipper = userConfig.getRealmRoles() != null && 
                    userConfig.getRealmRoles().contains("SHIPPER");
            
            if (!isShipper) {
                continue;
            }

            try {
                // Find user by username first
                Optional<User> userOpt = userRepository.findByUsername(userConfig.getUsername());
                if (userOpt.isEmpty()) {
                    log.warn("⚠️ User not found: {}. Skipping shift seeding.", userConfig.getUsername());
                    skipCount++;
                    continue;
                }

                User user = userOpt.get();
                
                // Find delivery man by userId
                Optional<DeliveryMan> deliveryManOpt = deliveryManRepository.findByUserId(user.getId());
                if (deliveryManOpt.isEmpty()) {
                    log.warn("⚠️ Delivery man not found for user: {}. Skipping shift seeding.", userConfig.getUsername());
                    skipCount++;
                    continue;
                }

                DeliveryMan deliveryMan = deliveryManOpt.get();

                // Check if delivery man already has shifts
                List<WorkingShift> existingShifts = workingShiftRepository.findByDeliveryManId(deliveryMan.getId());
                if (!existingShifts.isEmpty()) {
                    log.info("✓ Delivery man '{}' already has {} shift(s). Skipping shift seeding.", 
                            userConfig.getUsername(), existingShifts.size());
                    skipCount++;
                    continue;
                }

                // Seed shifts from config
                List<KeycloakInitConfig.ShiftConfig> shiftsConfig = userConfig.getDeliveryMan().getShifts();
                if (shiftsConfig != null && !shiftsConfig.isEmpty()) {
                    for (KeycloakInitConfig.ShiftConfig shiftConfig : shiftsConfig) {
                        seedShift(deliveryMan, shiftConfig);
                        shiftCount++;
                    }
                    successCount++;
                    log.info("✓ Seeded {} shift(s) for delivery man '{}'", 
                            shiftsConfig.size(), userConfig.getUsername());
                } else {
                    log.debug("No shifts configured for delivery man '{}'. Skipping.", userConfig.getUsername());
                    skipCount++;
                }
            } catch (Exception e) {
                log.error("❌ Failed to seed shifts for delivery man '{}': {}", 
                        userConfig.getUsername(), e.getMessage(), e);
                failCount++;
            }
        }

        log.info("🌱 Working shift seeding completed - Success: {}, Failed: {}, Skipped: {}, Total shifts: {}", 
                successCount, failCount, skipCount, shiftCount);
    }

    /**
     * Seed a single shift for a delivery man
     */
    private void seedShift(DeliveryMan deliveryMan, KeycloakInitConfig.ShiftConfig shiftConfig) {
        if (shiftConfig.getDayOfWeek() == null || shiftConfig.getStartTime() == null || shiftConfig.getEndTime() == null) {
            log.warn("⚠️ Shift config is incomplete. Skipping shift seeding.");
            return;
        }

        try {
            // Parse time strings (HH:mm format)
            LocalTime startTime = LocalTime.parse(shiftConfig.getStartTime());
            LocalTime endTime = LocalTime.parse(shiftConfig.getEndTime());

            WorkingShift shift = WorkingShift.builder()
                    .deliveryMan(deliveryMan)
                    .dayOfWeek(shiftConfig.getDayOfWeek())
                    .startTime(startTime)
                    .endTime(endTime)
                    .maxSessionTimeHours(shiftConfig.getMaxSessionTimeHours() != null 
                            ? shiftConfig.getMaxSessionTimeHours() 
                            : 4.0)
                    .isActive(shiftConfig.getIsActive() != null 
                            ? shiftConfig.getIsActive() 
                            : true)
                    .build();

            workingShiftRepository.save(shift);
            log.debug("✓ Created shift for delivery man '{}': Day {}, {} - {}", 
                    deliveryMan.getUser().getUsername(), 
                    shiftConfig.getDayOfWeek(), 
                    startTime, 
                    endTime);
        } catch (Exception e) {
            log.error("❌ Failed to seed shift for delivery man '{}': {}", 
                    deliveryMan.getUser().getUsername(), e.getMessage(), e);
            throw e;
        }
    }
}
