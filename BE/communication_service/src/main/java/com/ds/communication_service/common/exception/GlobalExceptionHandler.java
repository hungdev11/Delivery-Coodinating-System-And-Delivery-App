package com.ds.communication_service.common.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; 
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.ds.communication_service.common.dto.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice // Bắt exception từ tất cả @RestController
public class GlobalExceptionHandler {

    /**
     * 404 - Not Found
     * Bắt lỗi khi không tìm thấy entity (ví dụ: tìm Conversation, Proposal)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, 
            WebRequest request
    ) {
        log.warn("Không tìm thấy tài nguyên: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .message(ex.getMessage()) // "Conversation not found with ID: ..."
                .path(request.getDescription(false)) // Lấy URL (ví dụ: "uri=/api/v1/conversations/...")
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * 403 - Forbidden (Bị cấm)
     * Bắt lỗi khi user không có quyền (ví dụ: cố gắng chấp nhận proposal của người khác)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, 
            WebRequest request
    ) {
        log.warn("Truy cập bị từ chối: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .message("Bạn không có quyền thực hiện hành động này.")
                .path(request.getDescription(false))
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * 400 - Bad Request
     * Bắt lỗi logic nghiệp vụ hoặc đầu vào không hợp lệ 
     * (ví dụ: "Proposal type không hợp lệ", "Đề nghị đã được xử lý")
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(
            RuntimeException ex, // Bắt cả hai loại exception
            WebRequest request
    ) {
        log.warn("Yêu cầu không hợp lệ: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 500 - Internal Server Error (Lỗi chung)
     * Bắt tất cả các lỗi khác (ví dụ: NullPointerException)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, 
            WebRequest request
    ) {
        // Log lỗi nghiêm trọng này để debug
        log.error("Lỗi server không xác định:", ex); 
        ErrorResponse error = ErrorResponse.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .message("Hệ thống đã xảy ra lỗi. Vui lòng thử lại sau.")
                .path(request.getDescription(false))
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
