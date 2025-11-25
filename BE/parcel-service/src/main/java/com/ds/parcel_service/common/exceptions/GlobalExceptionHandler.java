package com.ds.parcel_service.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ds.parcel_service.common.entities.dto.common.BaseResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<BaseResponse<Object>> handleResourceNotFound(ResourceNotFound ex) {
        log.debug("[parcel-service] [GlobalExceptionHandler.handleResourceNotFound] Resource not found: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("Không tìm thấy dữ liệu"));
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<BaseResponse<Object>> handleBadRequest(RuntimeException ex) {
        log.debug("[parcel-service] [GlobalExceptionHandler.handleBadRequest] Bad request: {}", ex.getMessage());
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

        log.debug("[parcel-service] [GlobalExceptionHandler.handleValidationErrors] Validation failed: {}",
                errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(errorMessage.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(Exception ex) {
        log.error("[parcel-service] [GlobalExceptionHandler.handleGlobalException] Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("System have a technical issues!"));
    }
}
