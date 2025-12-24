package com.ds.user.application.client;

import lombok.Data;

@Data
public class AddressResponse<T> {
    private T result;
    private String message;
}
