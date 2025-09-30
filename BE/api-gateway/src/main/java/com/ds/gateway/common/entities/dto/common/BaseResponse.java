package com.ds.gateway.common.entities.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base response wrapper for all API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private String message;
    private T result;
    
    public static <T> BaseResponse<T> success(T result) {
        return BaseResponse.<T>builder()
            .message("Success")
            .result(result)
            .build();
    }
    
    public static <T> BaseResponse<T> success(String message, T result) {
        return BaseResponse.<T>builder()
            .message(message)
            .result(result)
            .build();
    }
    
    public static <T> BaseResponse<T> error(String message) {
        return BaseResponse.<T>builder()
            .message(message)
            .result(null)
            .build();
    }
}
