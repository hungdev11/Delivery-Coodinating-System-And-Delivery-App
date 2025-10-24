package com.ds.deliveryapp.clients.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse <T>{
    private T result;
    private String message;
}
