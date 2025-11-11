package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private String message;
    private T data;

    /**
     * Create a success response
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(true, message, data);
    }

    /**
     * Create a success response without data
     */
    public static <T> BaseResponse<T> success(String message) {
        return new BaseResponse<>(true, message, null);
    }

    /**
     * Create an error response
     */
    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(false, message, null);
    }
}
