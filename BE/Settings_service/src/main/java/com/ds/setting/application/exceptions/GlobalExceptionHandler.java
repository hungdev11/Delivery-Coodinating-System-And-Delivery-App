package com.ds.setting.application.exceptions;

import com.ds.setting.common.entities.dto.common.BaseResponse;
import com.ds.setting.common.exceptions.ReadOnlySettingException;
import com.ds.setting.common.exceptions.SettingNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for REST API
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SettingNotFoundException.class)
    public ResponseEntity<BaseResponse<Object>> handleSettingNotFound(SettingNotFoundException ex) {
        log.debug("[settings-service] [GlobalExceptionHandler.handleSettingNotFound] Setting not found: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("Không tìm thấy cài đặt"));
    }

    @ExceptionHandler(ReadOnlySettingException.class)
    public ResponseEntity<BaseResponse<Object>> handleReadOnlySetting(ReadOnlySettingException ex) {
        log.debug(
                "[settings-service] [GlobalExceptionHandler.handleReadOnlySetting] Read-only setting modification attempted: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error("Không thể thay đổi cài đặt này"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("[settings-service] [GlobalExceptionHandler.handleIllegalArgument] Illegal argument: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        StringBuilder errorMessage = new StringBuilder("Dữ liệu không hợp lệ: ");
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMessage.append(fieldName).append(" - ").append(message).append("; ");
        });

        log.debug("[settings-service] [GlobalExceptionHandler.handleValidationErrors] Validation failed: {}",
                errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(errorMessage.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(Exception ex) {
        log.error("[settings-service] [GlobalExceptionHandler.handleGlobalException] Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("System have a technical issues!"));
    }
}
