package com.ds.gateway.common.exceptions;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Global exception handler for API Gateway
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        log.debug("[api-gateway] [GlobalExceptionHandler.handleUnauthorizedException] Unauthorized access: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error("Vui lòng đăng nhập"));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<BaseResponse<Object>> handleForbiddenException(
            ForbiddenException ex, WebRequest request) {

        log.debug("[api-gateway] [GlobalExceptionHandler.handleForbiddenException] Forbidden access: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error("Bạn không có quyền truy cập"));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<BaseResponse<Object>> handleServiceUnavailableException(
            ServiceUnavailableException ex, WebRequest request) {

        log.debug("[api-gateway] [GlobalExceptionHandler.handleServiceUnavailableException] Service unavailable: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(BaseResponse.error("Dịch vụ tạm thời không khả dụng"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.debug("[api-gateway] [GlobalExceptionHandler.handleAuthenticationException] Authentication failed: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error("Xác thực thất bại"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        log.debug("[api-gateway] [GlobalExceptionHandler.handleAccessDeniedException] Access denied: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error("Truy cập bị từ chối"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.debug("[api-gateway] [GlobalExceptionHandler.handleIllegalArgumentException] Bad request: {}",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.error("[api-gateway] [GlobalExceptionHandler.handleHttpMessageNotReadableException] Invalid request body", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error("Dữ liệu yêu cầu không hợp lệ"));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<BaseResponse<Object>> handleWebClientResponseException(
            WebClientResponseException ex, WebRequest request) {

        log.error("[api-gateway] [GlobalExceptionHandler.handleWebClientResponseException] Error from downstream service: status={}, body={}", 
                ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
        
        // Try to parse the error response from the downstream service
        String errorMessage = "Lỗi từ dịch vụ";
        try {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isEmpty()) {
                errorMessage = responseBody;
            }
        } catch (Exception e) {
            log.warn("[api-gateway] [GlobalExceptionHandler.handleWebClientResponseException] Failed to parse error response", e);
        }
        
        return ResponseEntity.status(ex.getStatusCode())
                .body(BaseResponse.error(errorMessage));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        log.warn("[api-gateway] [GlobalExceptionHandler.handleHttpRequestMethodNotSupportedException] Method not supported: {} for {}", 
                ex.getMethod(), request.getDescription(false));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(BaseResponse.error("Phương thức HTTP không được hỗ trợ cho endpoint này"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("[api-gateway] [GlobalExceptionHandler.handleGlobalException] Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("System have a technical issues!"));
    }
}
