package com.ds.project.application.controllers.v1;

import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.dto.SettingDto;
import com.ds.project.common.entities.dto.request.SettingRequest;
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
        BaseResponse<SettingDto> response = settingService.createSetting(settingRequest);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to create setting"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getSettingById(@PathVariable String id) {
        return settingService.getSettingById(id)
            .map(response -> {
                if (response.getResult().isPresent()) {
                    return ResponseUtils.success(response.getResult().get());
                } else {
                    return ResponseUtils.error(response.getMessage().orElse("Setting not found"), 
                        org.springframework.http.HttpStatus.NOT_FOUND);
                }
            })
            .orElse(ResponseUtils.error("Setting not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/key/{key}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getSettingByKey(@PathVariable String key) {
        return settingService.getSettingByKey(key)
            .map(response -> {
                if (response.getResult().isPresent()) {
                    return ResponseUtils.success(response.getResult().get());
                } else {
                    return ResponseUtils.error(response.getMessage().orElse("Setting not found"), 
                        org.springframework.http.HttpStatus.NOT_FOUND);
                }
            })
            .orElse(ResponseUtils.error("Setting not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/group/{group}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getSettingsByGroup(@PathVariable String group) {
        List<BaseResponse<SettingDto>> responses = settingService.getSettingsByGroup(group);
        
        if (responses.isEmpty() || responses.get(0).getResult().isPresent()) {
            List<SettingDto> settings = responses.stream()
                .filter(response -> response.getResult().isPresent())
                .map(response -> response.getResult().get())
                .toList();
            return ResponseUtils.success(settings);
        } else {
            return ResponseUtils.error(responses.get(0).getMessage().orElse("Failed to get settings"), 
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getAllSettings() {
        BaseResponse<PagedData<Page, SettingDto>> response = settingService.getAllSettings();
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to get settings"), 
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateSetting(@PathVariable String id, @Valid @RequestBody SettingRequest settingRequest) {
        BaseResponse<SettingDto> response = settingService.updateSetting(id, settingRequest);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to update setting"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/key/{key}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateSettingByKey(@PathVariable String key, @RequestBody Map<String, String> request) {
        try {
            String value = request.get("value");
            BaseResponse<SettingDto> response = settingService.updateSettingByKey(key, value);
            
            if (response.getResult().isPresent()) {
                return ResponseUtils.success(response.getResult().get());
            } else {
                return ResponseUtils.error(response.getMessage().orElse("Failed to update setting"), 
                    org.springframework.http.HttpStatus.BAD_REQUEST);
            }
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
