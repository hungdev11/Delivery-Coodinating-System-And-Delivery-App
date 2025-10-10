package com.ds.project.application.controllers.v1;

import com.ds.project.common.entities.dto.request.SettingRequest;
import com.ds.project.common.entities.dto.response.SettingResponse;
import com.ds.project.common.utils.ResponseUtils;
import com.ds.project.application.annotations.AuthRequired;
import com.ds.project.common.interfaces.ISettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Setting operations
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {
    
    private final ISettingService settingService;
    
    @PostMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> createSetting(@Valid @RequestBody SettingRequest settingRequest) {
        try {
            SettingResponse createdSetting = settingService.createSetting(settingRequest);
            return ResponseUtils.success(createdSetting, "Setting created successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to create setting: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getSettingById(@PathVariable String id) {
        return settingService.getSettingById(id)
            .map(setting -> ResponseUtils.success(setting))
            .orElse(ResponseUtils.error("Setting not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/key/{key}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getSettingByKey(@PathVariable String key) {
        return settingService.getSettingByKey(key)
            .map(setting -> ResponseUtils.success(setting))
            .orElse(ResponseUtils.error("Setting not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/group/{group}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getSettingsByGroup(@PathVariable String group) {
        List<SettingResponse> settings = settingService.getSettingsByGroup(group);
        return ResponseUtils.success(settings);
    }
    
    @GetMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        List<SettingResponse> settings = settingService.getAllSettings();
        return ResponseUtils.success(settings);
    }
    
    @PutMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateSetting(@PathVariable String id, @Valid @RequestBody SettingRequest settingRequest) {
        try {
            SettingResponse updatedSetting = settingService.updateSetting(id, settingRequest);
            return ResponseUtils.success(updatedSetting, "Setting updated successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to update setting: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/key/{key}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateSettingByKey(@PathVariable String key, @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            SettingResponse updatedSetting = settingService.updateSettingByKey(key, value);
            return ResponseUtils.success(updatedSetting, "Setting updated successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to update setting: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> deleteSetting(@PathVariable String id) {
        try {
            settingService.deleteSetting(id);
            return ResponseUtils.success(null, "Setting deleted successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to delete setting: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
