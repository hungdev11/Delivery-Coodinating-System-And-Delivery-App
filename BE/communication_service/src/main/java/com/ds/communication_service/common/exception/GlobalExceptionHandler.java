package com.ds.communication_service.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.ds.communication_service.common.dto.BaseResponse;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<BaseResponse<Object>> handleEntityNotFound(
                        EntityNotFoundException ex,
                        WebRequest request) {
                log.debug("[communication-service] [GlobalExceptionHandler.handleEntityNotFound] Entity not found: {}",
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(BaseResponse.error("Không tìm thấy dữ liệu"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<BaseResponse<Object>> handleAccessDenied(
                        AccessDeniedException ex,
                        WebRequest request) {
                log.debug("[communication-service] [GlobalExceptionHandler.handleAccessDenied] Access denied: {}",
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(BaseResponse.error("Bạn không có quyền thực hiện hành động này"));
        }

        @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
        public ResponseEntity<BaseResponse<Object>> handleBadRequest(
                        RuntimeException ex,
                        WebRequest request) {
                log.debug("[communication-service] [GlobalExceptionHandler.handleBadRequest] Bad request: {}",
                                ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(BaseResponse.error(ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<BaseResponse<Object>> handleGlobalException(
                        Exception ex,
                        WebRequest request) {
                log.error("[communication-service] [GlobalExceptionHandler.handleGlobalException] Unexpected error",
                                ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(BaseResponse.error("System have a technical issues!"));
        }
}
