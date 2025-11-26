package com.ds.session.session_service.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ds.session.session_service.common.entities.dto.common.BaseResponse;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<BaseResponse<Object>> handleResourceNotFound(ResourceNotFound ex) {
        log.debug("[session-service] [GlobalExceptionHandler.handleResourceNotFound] Resource not found: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("Không tìm thấy tài nguyên"));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<BaseResponse<Object>> handleFeignExceptions(FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.error(
                "[session-service] [GlobalExceptionHandler.handleFeignExceptions] Feign communication error (Status: {})",
                ex.status(), ex);

        if (status == HttpStatus.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.error("Không tìm thấy dữ liệu từ dịch vụ liên quan"));
        }

        return ResponseEntity.status(status)
                .body(BaseResponse.error("Lỗi kết nối với dịch vụ khác"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("[session-service] [GlobalExceptionHandler.handleIllegalArgument] Illegal argument: {}",
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

        log.debug("[session-service] [GlobalExceptionHandler.handleValidationErrors] Validation failed: {}",
                errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(errorMessage.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(Exception ex) {
        log.error("[session-service] [GlobalExceptionHandler.handleGlobalException] Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("System have a technical issues!"));
    }
}
