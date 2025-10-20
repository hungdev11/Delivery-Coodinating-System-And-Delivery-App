package com.ds.user.common.entities.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base response wrapper for all API responses
 * Follows the standard defined in RESTFUL.md
 * 
 * @param <T> The type of the result data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaseResponse<T> {
    /**
     * Optional data result
     */
    private T result;
    
    /**
     * Optional message (for success messages or error descriptions)
     */
    private String message;
    
    /**
     * Create a success response with result data only
     */
    public static <T> BaseResponse<T> success(T result) {
        return BaseResponse.<T>builder()
                .result(result)
                .build();
    }
    
    /**
     * Create a success response with result data and message
     */
    public static <T> BaseResponse<T> success(T result, String message) {
        return BaseResponse.<T>builder()
                .result(result)
                .message(message)
                .build();
    }
    
    /**
     * Create an error response with message only
     */
    public static <T> BaseResponse<T> error(String message) {
        return BaseResponse.<T>builder()
                .message(message)
                .build();
    }
}
