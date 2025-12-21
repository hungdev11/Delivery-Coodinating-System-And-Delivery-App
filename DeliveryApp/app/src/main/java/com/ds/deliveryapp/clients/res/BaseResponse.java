package com.ds.deliveryapp.clients.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse <T>{
    private T result;
    private T data; // Some APIs return 'data' instead of 'result'
    private String message;
    
    /**
     * Get the data, checking both 'result' and 'data' fields
     * Some APIs return 'data', others return 'result'
     */
    public T getData() {
        return data != null ? data : result;
    }
}
