package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper from User Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean success;
    private String message;
    private T result;
}
